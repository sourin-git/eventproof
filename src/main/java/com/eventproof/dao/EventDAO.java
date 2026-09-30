package com.eventproof.dao;

import com.eventproof.config.DatabaseConnection;
import com.eventproof.model.EventDefinition;
import org.postgresql.util.PGobject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EventDAO {

    public EventDefinition create(EventDefinition event) throws SQLException {
        String sql = "INSERT INTO events (project_id, sequence_no, event_name, sample_payload) "
                + "VALUES (?, ?, ?, ?) "
                + "RETURNING id, project_id, sequence_no, event_name, sample_payload, created_at";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, event.getProjectId());
            statement.setInt(2, event.getSequenceNo());
            statement.setString(3, event.getEventName());

            if (event.getSamplePayloadJson() == null) {
                statement.setNull(4, Types.OTHER);
            } else {
                PGobject payload = new PGobject();
                payload.setType("jsonb");
                payload.setValue(event.getSamplePayloadJson());
                statement.setObject(4, payload);
            }

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Creating the event returned no row.");
                }
                return mapEvent(result);
            }
        }
    }

    public List<EventDefinition> findByProjectId(long projectId) throws SQLException {
        String sql = "SELECT id, project_id, sequence_no, event_name, sample_payload, created_at "
                + "FROM events WHERE project_id = ? ORDER BY sequence_no ASC";
        List<EventDefinition> events = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, projectId);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    events.add(mapEvent(result));
                }
            }
        }

        return events;
    }

    public Optional<EventDefinition> findById(long id) throws SQLException {
        String sql = "SELECT id, project_id, sequence_no, event_name, sample_payload, created_at "
                + "FROM events WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return Optional.of(mapEvent(result));
                }
                return Optional.empty();
            }
        }
    }

    private EventDefinition mapEvent(ResultSet result) throws SQLException {
        return new EventDefinition(
                result.getLong("id"),
                result.getLong("project_id"),
                result.getInt("sequence_no"),
                result.getString("event_name"),
                result.getString("sample_payload"),
                result.getObject("created_at", OffsetDateTime.class)
        );
    }
}
