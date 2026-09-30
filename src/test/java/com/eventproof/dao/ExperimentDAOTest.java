package com.eventproof.dao;

import com.eventproof.config.DatabaseConnection;
import com.eventproof.experiment.ExperimentEngine;
import com.eventproof.model.EventDefinition;
import com.eventproof.model.Experiment;
import com.eventproof.model.ExperimentEvent;
import com.eventproof.model.ExperimentStatus;
import com.eventproof.model.FaultType;
import com.eventproof.model.GeneratedEvent;
import com.eventproof.model.OccurrenceKind;
import com.eventproof.model.Project;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExperimentDAOTest {
    private final ProjectDAO projectDAO = new ProjectDAO();
    private final EventDAO eventDAO = new EventDAO();
    private final ExperimentDAO experimentDAO = new ExperimentDAO();
    private final ExperimentEngine engine = new ExperimentEngine();

    @Test
    void savesDuplicateExperimentAndTimeline() throws SQLException {
        Project project = createTestProject("Duplicate");
        try {
            List<EventDefinition> events = createThreeEvents(project.getId());
            EventDefinition a = events.get(0);
            EventDefinition b = events.get(1);
            EventDefinition c = events.get(2);
            String name = "EventProof Duplicate Test " + UUID.randomUUID();
            Experiment newExperiment = new Experiment(project.getId(), name, FaultType.DUPLICATE, b.getId());

            assertEquals(ExperimentStatus.PENDING, newExperiment.getStatus());
            List<GeneratedEvent> generated = engine.generate(events, FaultType.DUPLICATE, b.getId());
            Experiment saved = experimentDAO.saveCompletedExperiment(newExperiment, generated);

            assertTrue(saved.getId() > 0);
            assertEquals(ExperimentStatus.COMPLETED, saved.getStatus());
            assertNotNull(saved.getCreatedAt());
            assertEquals(b.getId(), saved.getTargetEventId());

            Experiment found = experimentDAO.findById(saved.getId()).orElseThrow();
            assertEquals(saved.getId(), found.getId());
            assertEquals(project.getId(), found.getProjectId());
            assertEquals(name, found.getName());
            assertEquals(FaultType.DUPLICATE, found.getFaultType());
            assertEquals(b.getId(), found.getTargetEventId());
            assertEquals(ExperimentStatus.COMPLETED, found.getStatus());
            assertEquals(saved.getCreatedAt(), found.getCreatedAt());
            assertTrue(experimentDAO.findByProjectId(project.getId()).stream()
                    .anyMatch(experiment -> experiment.getId() == saved.getId()));

            assertTimeline(experimentDAO.findTimeline(saved.getId()), saved.getId(),
                    List.of(a.getId(), b.getId(), b.getId(), c.getId()),
                    List.of(OccurrenceKind.NORMAL, OccurrenceKind.NORMAL,
                            OccurrenceKind.DUPLICATE, OccurrenceKind.NORMAL));
        } finally {
            deleteTestData(project);
        }
    }

    @Test
    void savesDropExperimentWithoutDroppedEvent() throws SQLException {
        Project project = createTestProject("Drop");
        try {
            List<EventDefinition> events = createThreeEvents(project.getId());
            EventDefinition a = events.get(0);
            EventDefinition b = events.get(1);
            EventDefinition c = events.get(2);
            Experiment newExperiment = new Experiment(
                    project.getId(), "EventProof Drop Test " + UUID.randomUUID(), FaultType.DROP, b.getId());

            List<GeneratedEvent> generated = engine.generate(events, FaultType.DROP, b.getId());
            Experiment saved = experimentDAO.saveCompletedExperiment(newExperiment, generated);

            assertEquals(ExperimentStatus.COMPLETED, saved.getStatus());
            Experiment found = experimentDAO.findById(saved.getId()).orElseThrow();
            assertEquals(FaultType.DROP, found.getFaultType());
            assertEquals(b.getId(), found.getTargetEventId());
            assertTimeline(experimentDAO.findTimeline(saved.getId()), saved.getId(),
                    List.of(a.getId(), c.getId()),
                    List.of(OccurrenceKind.NORMAL, OccurrenceKind.NORMAL));
        } finally {
            deleteTestData(project);
        }
    }

    @Test
    void rollsBackExperimentAndEarlierTimelineRowOnForeignKeyFailure() throws SQLException {
        Project project = createTestProject("Rollback");
        try {
            EventDefinition validEvent = eventDAO.create(
                    new EventDefinition(project.getId(), 1, "TEST_VALID_EVENT", null));
            EventDefinition nonexistentEvent = new EventDefinition(
                    Long.MAX_VALUE, project.getId(), 2, "TEST_NONEXISTENT_EVENT", null, null);
            String name = "EventProof Rollback Test " + UUID.randomUUID();
            Experiment experiment = new Experiment(project.getId(), name, FaultType.NORMAL, null);
            List<GeneratedEvent> generated = List.of(
                    new GeneratedEvent(1, validEvent, OccurrenceKind.NORMAL),
                    new GeneratedEvent(2, nonexistentEvent, OccurrenceKind.NORMAL)
            );

            SQLException failure = assertThrows(SQLException.class,
                    () -> experimentDAO.saveCompletedExperiment(experiment, generated));
            assertEquals("23503", failure.getSQLState(), "The later insert should fail on a foreign key.");
            assertEquals(0, countExperimentByName(project.getId(), name));
            assertEquals(0, countTimelineRowsForEvent(validEvent.getId()));
        } finally {
            deleteTestData(project);
        }
    }

    private Project createTestProject(String testName) throws SQLException {
        return projectDAO.create(new Project(
                "EventProof ExperimentDAO " + testName + " Test " + UUID.randomUUID(),
                "Temporary project for ExperimentDAOTest"));
    }

    private List<EventDefinition> createThreeEvents(long projectId) throws SQLException {
        EventDefinition a = eventDAO.create(new EventDefinition(projectId, 1, "TEST_A", null));
        EventDefinition b = eventDAO.create(new EventDefinition(projectId, 2, "TEST_B", null));
        EventDefinition c = eventDAO.create(new EventDefinition(projectId, 3, "TEST_C", null));
        return List.of(a, b, c);
    }

    private void assertTimeline(List<ExperimentEvent> actual, long experimentId,
                                List<Long> expectedEventIds, List<OccurrenceKind> expectedKinds) {
        assertEquals(expectedEventIds.size(), expectedKinds.size());
        assertEquals(expectedEventIds.size(), actual.size());
        for (int index = 0; index < actual.size(); index++) {
            ExperimentEvent row = actual.get(index);
            assertTrue(row.getId() > 0);
            assertEquals(experimentId, row.getExperimentId());
            assertEquals(index + 1, row.getExecutionOrder());
            assertEquals(expectedEventIds.get(index), row.getEventId());
            assertEquals(expectedKinds.get(index), row.getOccurrenceKind());
        }
    }

    private int countExperimentByName(long projectId, String name) throws SQLException {
        String sql = "SELECT COUNT(*) FROM experiments WHERE project_id = ? AND name = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, projectId);
            statement.setString(2, name);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private int countTimelineRowsForEvent(long eventId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM experiment_events WHERE event_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, eventId);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private void deleteTestData(Project project) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement deleteTimeline = connection.prepareStatement(
                     "DELETE FROM experiment_events WHERE experiment_id IN "
                             + "(SELECT id FROM experiments WHERE project_id = ?)");
             PreparedStatement deleteExperiments = connection.prepareStatement(
                     "DELETE FROM experiments WHERE project_id = ?");
             PreparedStatement deleteEvents = connection.prepareStatement(
                     "DELETE FROM events WHERE project_id = ?");
             PreparedStatement deleteProject = connection.prepareStatement(
                     "DELETE FROM projects WHERE id = ? AND name = ?")) {
            deleteTimeline.setLong(1, project.getId());
            deleteTimeline.executeUpdate();

            deleteExperiments.setLong(1, project.getId());
            deleteExperiments.executeUpdate();

            deleteEvents.setLong(1, project.getId());
            deleteEvents.executeUpdate();

            deleteProject.setLong(1, project.getId());
            deleteProject.setString(2, project.getName());
            assertEquals(1, deleteProject.executeUpdate(), "The temporary test project should be removed.");
        }
    }
}
