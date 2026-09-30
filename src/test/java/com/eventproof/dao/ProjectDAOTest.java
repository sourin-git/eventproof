package com.eventproof.dao;

import com.eventproof.config.DatabaseConnection;
import com.eventproof.model.Project;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectDAOTest {

    @Test
    void createsAndFindsProject() throws SQLException {
        ProjectDAO dao = new ProjectDAO();
        String name = "EventProof ProjectDAO Test " + UUID.randomUUID();
        String description = "Temporary project for ProjectDAOTest";
        boolean created = false;

        try {
            Project saved = dao.create(new Project(name, description));
            created = true;

            assertTrue(saved.getId() > 0);
            assertEquals(name, saved.getName());
            assertEquals(description, saved.getDescription());
            assertNotNull(saved.getCreatedAt());

            Project loaded = dao.findById(saved.getId()).orElseThrow();
            assertEquals(saved.getId(), loaded.getId());
            assertEquals(name, loaded.getName());
            assertEquals(description, loaded.getDescription());
            assertEquals(saved.getCreatedAt(), loaded.getCreatedAt());

            assertTrue(dao.findAll().stream().anyMatch(project -> project.getId() == saved.getId()));
        } finally {
            int deleted = deleteTestProject(name, description);
            if (created) {
                assertEquals(1, deleted, "The temporary test project should be removed.");
            }
        }
    }

    private int deleteTestProject(String name, String description) throws SQLException {
        String sql = "DELETE FROM projects WHERE name = ? AND description = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setString(2, description);
            return statement.executeUpdate();
        }
    }
}
