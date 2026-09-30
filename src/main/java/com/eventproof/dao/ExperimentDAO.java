package com.eventproof.dao;

import com.eventproof.config.DatabaseConnection;
import com.eventproof.model.EventDefinition;
import com.eventproof.model.Experiment;
import com.eventproof.model.ExperimentEvent;
import com.eventproof.model.ExperimentStatus;
import com.eventproof.model.FaultType;
import com.eventproof.model.GeneratedEvent;
import com.eventproof.model.OccurrenceKind;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ExperimentDAO {

    public Experiment saveCompletedExperiment(Experiment experiment,
                                              List<GeneratedEvent> generatedEvents) throws SQLException {
        validate(experiment, generatedEvents);

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Experiment pending = insertPending(connection, experiment);
                insertTimeline(connection, pending.getId(), generatedEvents);
                markCompleted(connection, pending.getId());

                Experiment completed = new Experiment(
                        pending.getId(), pending.getProjectId(), pending.getName(),
                        pending.getFaultType(), pending.getTargetEventId(),
                        ExperimentStatus.COMPLETED, pending.getCreatedAt()
                );
                connection.commit();
                return completed;
            } catch (SQLException | RuntimeException failure) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
                throw failure;
            }
        }
    }

    public Optional<Experiment> findById(long id) throws SQLException {
        String sql = "SELECT id, project_id, name, fault_type, target_event_id, status, created_at "
                + "FROM experiments WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return Optional.of(mapExperiment(result));
                }
                return Optional.empty();
            }
        }
    }

    public List<Experiment> findByProjectId(long projectId) throws SQLException {
        String sql = "SELECT id, project_id, name, fault_type, target_event_id, status, created_at "
                + "FROM experiments WHERE project_id = ? ORDER BY created_at DESC, id DESC";
        List<Experiment> experiments = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, projectId);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    experiments.add(mapExperiment(result));
                }
            }
        }
        return experiments;
    }

    public List<ExperimentEvent> findTimeline(long experimentId) throws SQLException {
        String sql = "SELECT id, experiment_id, event_id, execution_order, occurrence_kind "
                + "FROM experiment_events WHERE experiment_id = ? ORDER BY execution_order ASC";
        List<ExperimentEvent> timeline = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, experimentId);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    timeline.add(new ExperimentEvent(
                            result.getLong("id"),
                            result.getLong("experiment_id"),
                            result.getLong("event_id"),
                            result.getInt("execution_order"),
                            OccurrenceKind.valueOf(result.getString("occurrence_kind"))
                    ));
                }
            }
        }
        return timeline;
    }

    private Experiment insertPending(Connection connection, Experiment experiment) throws SQLException {
        String sql = "INSERT INTO experiments (project_id, name, fault_type, target_event_id, status) "
                + "VALUES (?, ?, ?, ?, ?) RETURNING id, created_at";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, experiment.getProjectId());
            statement.setString(2, experiment.getName());
            statement.setString(3, experiment.getFaultType().name());
            if (experiment.getTargetEventId() == null) {
                statement.setNull(4, Types.BIGINT);
            } else {
                statement.setLong(4, experiment.getTargetEventId());
            }
            statement.setString(5, ExperimentStatus.PENDING.name());

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Creating the experiment returned no row.");
                }
                return new Experiment(
                        result.getLong("id"), experiment.getProjectId(), experiment.getName(),
                        experiment.getFaultType(), experiment.getTargetEventId(),
                        ExperimentStatus.PENDING,
                        result.getObject("created_at", OffsetDateTime.class)
                );
            }
        }
    }

    private void insertTimeline(Connection connection, long experimentId,
                                List<GeneratedEvent> generatedEvents) throws SQLException {
        String sql = "INSERT INTO experiment_events "
                + "(experiment_id, event_id, execution_order, occurrence_kind) VALUES (?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (GeneratedEvent generated : generatedEvents) {
                statement.setLong(1, experimentId);
                statement.setLong(2, generated.getSourceEvent().getId());
                statement.setInt(3, generated.getExecutionOrder());
                statement.setString(4, generated.getOccurrenceKind().name());
                statement.executeUpdate();
            }
        }
    }

    private void markCompleted(Connection connection, long experimentId) throws SQLException {
        String sql = "UPDATE experiments SET status = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ExperimentStatus.COMPLETED.name());
            statement.setLong(2, experimentId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Could not mark the experiment as completed.");
            }
        }
    }

    private Experiment mapExperiment(ResultSet result) throws SQLException {
        long targetId = result.getLong("target_event_id");
        Long targetEventId = result.wasNull() ? null : targetId;
        return new Experiment(
                result.getLong("id"),
                result.getLong("project_id"),
                result.getString("name"),
                FaultType.valueOf(result.getString("fault_type")),
                targetEventId,
                ExperimentStatus.valueOf(result.getString("status")),
                result.getObject("created_at", OffsetDateTime.class)
        );
    }

    private void validate(Experiment experiment, List<GeneratedEvent> generatedEvents) {
        if (experiment == null) {
            throw new IllegalArgumentException("experiment must not be null.");
        }
        if (generatedEvents == null) {
            throw new IllegalArgumentException("generatedEvents must not be null.");
        }
        if (experiment.getProjectId() <= 0) {
            throw new IllegalArgumentException("projectId must be positive.");
        }
        if (experiment.getName() == null || experiment.getName().isBlank()) {
            throw new IllegalArgumentException("experiment name must not be blank.");
        }
        if (experiment.getFaultType() == null) {
            throw new IllegalArgumentException("faultType must not be null.");
        }
        if (experiment.getFaultType() == FaultType.NORMAL && experiment.getTargetEventId() != null) {
            throw new IllegalArgumentException("NORMAL must not have a target event.");
        }
        if (experiment.getFaultType() != FaultType.NORMAL && experiment.getTargetEventId() == null) {
            throw new IllegalArgumentException(experiment.getFaultType() + " requires a target event.");
        }

        int expectedOrder = 1;
        for (GeneratedEvent generated : generatedEvents) {
            if (generated == null) {
                throw new IllegalArgumentException("generatedEvents must not contain null.");
            }
            EventDefinition source = generated.getSourceEvent();
            if (source == null) {
                throw new IllegalArgumentException("Generated event source must not be null.");
            }
            if (source.getId() <= 0) {
                throw new IllegalArgumentException("Generated event source ID must be positive.");
            }
            if (source.getProjectId() != experiment.getProjectId()) {
                throw new IllegalArgumentException("Generated event belongs to another project.");
            }
            if (generated.getExecutionOrder() != expectedOrder) {
                throw new IllegalArgumentException("Generated execution orders must be 1, 2, 3, ...");
            }
            if (generated.getOccurrenceKind() == null) {
                throw new IllegalArgumentException("Generated occurrence kind must not be null.");
            }
            expectedOrder++;
        }
    }
}
