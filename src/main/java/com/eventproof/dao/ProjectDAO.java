package com.eventproof.dao;

import com.eventproof.config.DatabaseConnection;
import com.eventproof.model.Project;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProjectDAO {

    public Project create(Project project) throws SQLException {
        String sql = "INSERT INTO projects (name, description) VALUES (?, ?) "
                + "RETURNING id, name, description, created_at";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, project.getName());
            statement.setString(2, project.getDescription());

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Creating the project returned no row.");
                }
                return mapProject(result);
            }
        }
    }

    public List<Project> findAll() throws SQLException {
        String sql = "SELECT id, name, description, created_at FROM projects "
                + "ORDER BY created_at DESC, id DESC";
        List<Project> projects = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                projects.add(mapProject(result));
            }
        }

        return projects;
    }

    public Optional<Project> findById(long id) throws SQLException {
        String sql = "SELECT id, name, description, created_at FROM projects WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return Optional.of(mapProject(result));
                }
                return Optional.empty();
            }
        }
    }

    private Project mapProject(ResultSet result) throws SQLException {
        return new Project(
                result.getLong("id"),
                result.getString("name"),
                result.getString("description"),
                result.getObject("created_at", OffsetDateTime.class)
        );
    }
}
