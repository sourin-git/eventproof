package com.eventproof.service;

import com.eventproof.config.DatabaseConnection;
import com.eventproof.model.EventDefinition;
import com.eventproof.model.Experiment;
import com.eventproof.model.ExperimentEvent;
import com.eventproof.model.ExperimentStatus;
import com.eventproof.model.FaultType;
import com.eventproof.model.OccurrenceKind;
import com.eventproof.model.Project;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Review1ServiceFlowTest {
    private final ProjectService projectService = new ProjectService();
    private final EventService eventService = new EventService();
    private final ExperimentService experimentService = new ExperimentService();

    @Test
    void createsProjectEventsAndDuplicateExperiment() throws SQLException {
        String projectName = "EventProof Service Flow Test " + UUID.randomUUID();
        Project project = projectService.createProject(projectName, "Temporary service integration test");

        try {
            assertEquals(project.getId(), projectService.getProject(project.getId()).orElseThrow().getId());
            assertTrue(projectService.getAllProjects().stream()
                    .anyMatch(item -> item.getId() == project.getId()));

            EventDefinition a = eventService.createEvent(project.getId(), 1, "TEST_A", null);
            EventDefinition b = eventService.createEvent(project.getId(), 2, "TEST_B", null);
            EventDefinition c = eventService.createEvent(project.getId(), 3, "TEST_C", null);

            List<EventDefinition> events = eventService.getProjectEvents(project.getId());
            assertEquals(List.of(a.getId(), b.getId(), c.getId()),
                    events.stream().map(EventDefinition::getId).toList());

            String experimentName = "EventProof Service Duplicate Test " + UUID.randomUUID();
            Experiment experiment = experimentService.runExperiment(
                    project.getId(), experimentName, FaultType.DUPLICATE, b.getId());

            assertTrue(experiment.getId() > 0);
            assertEquals(ExperimentStatus.COMPLETED, experiment.getStatus());
            assertEquals(b.getId(), experiment.getTargetEventId());
            assertNotNull(experiment.getCreatedAt());
            assertTrue(experimentService.getProjectExperiments(project.getId()).stream()
                    .anyMatch(item -> item.getId() == experiment.getId()));

            List<ExperimentEvent> timeline = experimentService.getExperimentTimeline(experiment.getId());
            assertEquals(4, timeline.size());
            List<Long> expectedEventIds = List.of(a.getId(), b.getId(), b.getId(), c.getId());
            List<OccurrenceKind> expectedKinds = List.of(
                    OccurrenceKind.NORMAL, OccurrenceKind.NORMAL,
                    OccurrenceKind.DUPLICATE, OccurrenceKind.NORMAL);
            for (int index = 0; index < timeline.size(); index++) {
                assertEquals(index + 1, timeline.get(index).getExecutionOrder());
                assertEquals(expectedEventIds.get(index), timeline.get(index).getEventId());
                assertEquals(expectedKinds.get(index), timeline.get(index).getOccurrenceKind());
            }
        } finally {
            deleteTestData(project);
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
