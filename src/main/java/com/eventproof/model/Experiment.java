package com.eventproof.model;

import java.time.OffsetDateTime;

public class Experiment {
    private final long id;
    private final long projectId;
    private final String name;
    private final FaultType faultType;
    private final Long targetEventId;
    private final ExperimentStatus status;
    private final OffsetDateTime createdAt;

    public Experiment(long projectId, String name, FaultType faultType, Long targetEventId) {
        this(0, projectId, name, faultType, targetEventId, ExperimentStatus.PENDING, null);
    }

    public Experiment(long id, long projectId, String name, FaultType faultType,
                      Long targetEventId, ExperimentStatus status, OffsetDateTime createdAt) {
        this.id = id;
        this.projectId = projectId;
        this.name = name;
        this.faultType = faultType;
        this.targetEventId = targetEventId;
        this.status = status;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public long getProjectId() {
        return projectId;
    }

    public String getName() {
        return name;
    }

    public FaultType getFaultType() {
        return faultType;
    }

    public Long getTargetEventId() {
        return targetEventId;
    }

    public ExperimentStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "Experiment{id=" + id + ", projectId=" + projectId + ", name='" + name
                + "', faultType=" + faultType + ", targetEventId=" + targetEventId
                + ", status=" + status + ", createdAt=" + createdAt + "}";
    }
}
