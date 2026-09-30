package com.eventproof.experiment;

import com.eventproof.model.EventDefinition;
import com.eventproof.model.FaultType;
import com.eventproof.model.GeneratedEvent;
import com.eventproof.model.OccurrenceKind;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExperimentEngine {

    public List<GeneratedEvent> generate(List<EventDefinition> originalEvents,
                                         FaultType faultType, Long targetEventId) {
        if (originalEvents == null) {
            throw new IllegalArgumentException("originalEvents must not be null.");
        }
        if (faultType == null) {
            throw new IllegalArgumentException("faultType must not be null.");
        }
        if (faultType == FaultType.NORMAL && targetEventId != null) {
            throw new IllegalArgumentException("NORMAL must not have a target event.");
        }
        if (faultType != FaultType.NORMAL && targetEventId == null) {
            throw new IllegalArgumentException(faultType + " requires a target event.");
        }

        List<EventDefinition> orderedEvents = new ArrayList<>(originalEvents);
        Set<Long> eventIds = new HashSet<>();
        Set<Integer> sequenceNumbers = new HashSet<>();
        for (EventDefinition event : orderedEvents) {
            if (event == null) {
                throw new IllegalArgumentException("originalEvents must not contain null events.");
            }
            if (!eventIds.add(event.getId())) {
                throw new IllegalArgumentException("Duplicate event ID: " + event.getId());
            }
            if (!sequenceNumbers.add(event.getSequenceNo())) {
                throw new IllegalArgumentException("Duplicate sequence number: " + event.getSequenceNo());
            }
        }

        if (faultType != FaultType.NORMAL && !eventIds.contains(targetEventId)) {
            throw new IllegalArgumentException("Target event ID is not in originalEvents: " + targetEventId);
        }

        orderedEvents.sort(Comparator.comparingInt(EventDefinition::getSequenceNo));

        List<GeneratedEvent> timeline = new ArrayList<>();
        for (EventDefinition event : orderedEvents) {
            boolean isTarget = faultType != FaultType.NORMAL && event.getId() == targetEventId.longValue();
            if (faultType == FaultType.DROP && isTarget) {
                continue;
            }

            timeline.add(new GeneratedEvent(timeline.size() + 1, event, OccurrenceKind.NORMAL));
            if (faultType == FaultType.DUPLICATE && isTarget) {
                timeline.add(new GeneratedEvent(timeline.size() + 1, event, OccurrenceKind.DUPLICATE));
            }
        }
        return timeline;
    }
}
