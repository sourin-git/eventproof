package com.eventproof.model;

import java.time.OffsetDateTime;

public class EventDefinition {
    private final long id;
    private final long projectId;
    private final int sequenceNo;
    private final String eventName;
    private final String samplePayloadJson;
    private final OffsetDateTime createdAt;

    public EventDefinition(long projectId, int sequenceNo, String eventName, String samplePayloadJson) {
        this(0, projectId, sequenceNo, eventName, samplePayloadJson, null);
    }

    public EventDefinition(long id, long projectId, int sequenceNo, String eventName,
                           String samplePayloadJson, OffsetDateTime createdAt) {
        this.id = id;
        this.projectId = projectId;
        this.sequenceNo = sequenceNo;
        this.eventName = eventName;
        this.samplePayloadJson = samplePayloadJson;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public long getProjectId() {
        return projectId;
    }

    public int getSequenceNo() {
        return sequenceNo;
    }

    public String getEventName() {
        return eventName;
    }

    public String getSamplePayloadJson() {
        return samplePayloadJson;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "EventDefinition{id=" + id + ", projectId=" + projectId
                + ", sequenceNo=" + sequenceNo + ", eventName='" + eventName
                + "', samplePayloadJson='" + samplePayloadJson + "', createdAt=" + createdAt + "}";
    }
}
