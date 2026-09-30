package com.eventproof.model;

public class ExperimentEvent {
    private final long id;
    private final long experimentId;
    private final long eventId;
    private final int executionOrder;
    private final OccurrenceKind occurrenceKind;

    public ExperimentEvent(long id, long experimentId, long eventId,
                           int executionOrder, OccurrenceKind occurrenceKind) {
        this.id = id;
        this.experimentId = experimentId;
        this.eventId = eventId;
        this.executionOrder = executionOrder;
        this.occurrenceKind = occurrenceKind;
    }

    public long getId() {
        return id;
    }

    public long getExperimentId() {
        return experimentId;
    }

    public long getEventId() {
        return eventId;
    }

    public int getExecutionOrder() {
        return executionOrder;
    }

    public OccurrenceKind getOccurrenceKind() {
        return occurrenceKind;
    }

    @Override
    public String toString() {
        return "ExperimentEvent{id=" + id + ", experimentId=" + experimentId
                + ", eventId=" + eventId + ", executionOrder=" + executionOrder
                + ", occurrenceKind=" + occurrenceKind + "}";
    }
}
