package com.eventproof.dao;

import com.eventproof.config.DatabaseConnection;
import com.eventproof.model.EventDefinition;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventDAOTest {

    @Test
    void createsAndFindsOrderedEvents() throws SQLException {
        ProjectDAO projectDAO = new ProjectDAO();
        EventDAO eventDAO = new EventDAO();
        String projectName = "EventProof EventDAO Test " + UUID.randomUUID();
        Project project = projectDAO.create(new Project(projectName, "Temporary EventDAO test project"));

        try {
            String firstPayload = "{\"orderId\":\"TEST-1\",\"amount\":42}";
            EventDefinition first = eventDAO.create(
                    new EventDefinition(project.getId(), 1, "TEST_ORDER_CREATED", firstPayload));
            EventDefinition second = eventDAO.create(
                    new EventDefinition(project.getId(), 2, "TEST_PAYMENT_COMPLETED", null));
            EventDefinition third = eventDAO.create(
                    new EventDefinition(project.getId(), 3, "TEST_ORDER_CONFIRMED", "{\"confirmed\":true}"));

            assertTrue(first.getId() > 0);
            assertTrue(second.getId() > 0);
            assertTrue(third.getId() > 0);
            assertNotNull(first.getCreatedAt());

            List<EventDefinition> events = eventDAO.findByProjectId(project.getId());
            assertEquals(3, events.size());
            assertEquals(List.of(1, 2, 3), events.stream().map(EventDefinition::getSequenceNo).toList());
            assertEquals(List.of(first.getId(), second.getId(), third.getId()),
                    events.stream().map(EventDefinition::getId).toList());
            assertNull(events.get(1).getSamplePayloadJson());

            EventDefinition loaded = eventDAO.findById(first.getId()).orElseThrow();
            assertEquals(project.getId(), loaded.getProjectId());
            assertEquals(1, loaded.getSequenceNo());
            assertEquals("TEST_ORDER_CREATED", loaded.getEventName());
            assertEquals(first.getCreatedAt(), loaded.getCreatedAt());
            assertJsonEquals(firstPayload, loaded.getSamplePayloadJson());
        } finally {
            deleteTestData(project.getId(), projectName);
        }
    }

    private void assertJsonEquals(String expected, String actual) throws SQLException {
        String sql = "SELECT ?::jsonb = ?::jsonb";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, expected);
            statement.setString(2, actual);

            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                assertTrue(result.getBoolean(1), "Sample JSON payload should round-trip without changing its value.");
            }
        }
    }

    private void deleteTestData(long projectId, String projectName) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement deleteEvents = connection.prepareStatement(
                     "DELETE FROM events WHERE project_id = ?");
             PreparedStatement deleteProject = connection.prepareStatement(
                     "DELETE FROM projects WHERE id = ? AND name = ?")) {
            deleteEvents.setLong(1, projectId);
            deleteEvents.executeUpdate();

            deleteProject.setLong(1, projectId);
            deleteProject.setString(2, projectName);
            assertEquals(1, deleteProject.executeUpdate(), "The temporary test project should be removed.");
        }
    }
}
