package com.eventproof.model;

import java.util.Objects;

public final class GeneratedEvent {
    private final int executionOrder;
    private final EventDefinition sourceEvent;
    private final OccurrenceKind occurrenceKind;

    public GeneratedEvent(int executionOrder, EventDefinition sourceEvent, OccurrenceKind occurrenceKind) {
        this.executionOrder = executionOrder;
        this.sourceEvent = Objects.requireNonNull(sourceEvent, "sourceEvent");
        this.occurrenceKind = Objects.requireNonNull(occurrenceKind, "occurrenceKind");
    }

    public int getExecutionOrder() {
        return executionOrder;
    }

    public EventDefinition getSourceEvent() {
        return sourceEvent;
    }

    public OccurrenceKind getOccurrenceKind() {
        return occurrenceKind;
    }

    @Override
    public String toString() {
        return "GeneratedEvent{executionOrder=" + executionOrder
                + ", sourceEvent=" + sourceEvent.getEventName()
                + ", occurrenceKind=" + occurrenceKind + "}";
    }
}
