package com.eventproof.experiment;

import com.eventproof.model.EventDefinition;
import com.eventproof.model.FaultType;
import com.eventproof.model.GeneratedEvent;
import com.eventproof.model.OccurrenceKind;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExperimentEngineTest {
    private static final EventDefinition A = event(1, 1, "A");
    private static final EventDefinition B = event(2, 2, "B");
    private static final EventDefinition C = event(3, 3, "C");

    private final ExperimentEngine engine = new ExperimentEngine();

    @Test
    void normalKeepsThreeEvents() {
        assertTimeline(engine.generate(List.of(A, B, C), FaultType.NORMAL, null),
                List.of(A, B, C),
                List.of(OccurrenceKind.NORMAL, OccurrenceKind.NORMAL, OccurrenceKind.NORMAL));
    }

    @Test
    void duplicateFirstEvent() {
        assertTimeline(engine.generate(List.of(A, B, C), FaultType.DUPLICATE, A.getId()),
                List.of(A, A, B, C),
                List.of(OccurrenceKind.NORMAL, OccurrenceKind.DUPLICATE,
                        OccurrenceKind.NORMAL, OccurrenceKind.NORMAL));
    }

    @Test
    void duplicateMiddleEvent() {
        assertTimeline(engine.generate(List.of(A, B, C), FaultType.DUPLICATE, B.getId()),
                List.of(A, B, B, C),
                List.of(OccurrenceKind.NORMAL, OccurrenceKind.NORMAL,
                        OccurrenceKind.DUPLICATE, OccurrenceKind.NORMAL));
    }

    @Test
    void duplicateLastEvent() {
        assertTimeline(engine.generate(List.of(A, B, C), FaultType.DUPLICATE, C.getId()),
                List.of(A, B, C, C),
                List.of(OccurrenceKind.NORMAL, OccurrenceKind.NORMAL,
                        OccurrenceKind.NORMAL, OccurrenceKind.DUPLICATE));
    }

    @Test
    void dropFirstEvent() {
        assertTimeline(engine.generate(List.of(A, B, C), FaultType.DROP, A.getId()),
                List.of(B, C), List.of(OccurrenceKind.NORMAL, OccurrenceKind.NORMAL));
    }

    @Test
    void dropMiddleEvent() {
        assertTimeline(engine.generate(List.of(A, B, C), FaultType.DROP, B.getId()),
                List.of(A, C), List.of(OccurrenceKind.NORMAL, OccurrenceKind.NORMAL));
    }

    @Test
    void dropLastEvent() {
        assertTimeline(engine.generate(List.of(A, B, C), FaultType.DROP, C.getId()),
                List.of(A, B), List.of(OccurrenceKind.NORMAL, OccurrenceKind.NORMAL));
    }

    @Test
    void emptyNormalFlowProducesEmptyTimeline() {
        assertTimeline(engine.generate(List.of(), FaultType.NORMAL, null), List.of(), List.of());
    }

    @Test
    void duplicateRequiresTarget() {
        assertThrows(IllegalArgumentException.class,
                () -> engine.generate(List.of(A, B, C), FaultType.DUPLICATE, null));
    }

    @Test
    void dropRequiresTarget() {
        assertThrows(IllegalArgumentException.class,
                () -> engine.generate(List.of(A, B, C), FaultType.DROP, null));
    }

    @Test
    void rejectsTargetNotInFlow() {
        assertThrows(IllegalArgumentException.class,
                () -> engine.generate(List.of(A, B, C), FaultType.DUPLICATE, 99L));
        assertThrows(IllegalArgumentException.class,
                () -> engine.generate(List.of(A, B, C), FaultType.DROP, 99L));
    }

    @Test
    void normalRejectsTarget() {
        assertThrows(IllegalArgumentException.class,
                () -> engine.generate(List.of(A, B, C), FaultType.NORMAL, B.getId()));
    }

    @Test
    void sortsEventsBySequenceNumber() {
        assertTimeline(engine.generate(List.of(C, A, B), FaultType.NORMAL, null),
                List.of(A, B, C),
                List.of(OccurrenceKind.NORMAL, OccurrenceKind.NORMAL, OccurrenceKind.NORMAL));
    }

    @Test
    void doesNotMutateCallerList() {
        List<EventDefinition> input = new ArrayList<>(List.of(C, A, B));
        List<EventDefinition> originalOrder = List.copyOf(input);

        assertTimeline(engine.generate(input, FaultType.DUPLICATE, B.getId()),
                List.of(A, B, B, C),
                List.of(OccurrenceKind.NORMAL, OccurrenceKind.NORMAL,
                        OccurrenceKind.DUPLICATE, OccurrenceKind.NORMAL));
        assertEquals(originalOrder, input);
    }

    @Test
    void rejectsNullInputList() {
        assertThrows(IllegalArgumentException.class,
                () -> engine.generate(null, FaultType.NORMAL, null));
    }

    @Test
    void rejectsNullFaultType() {
        assertThrows(IllegalArgumentException.class,
                () -> engine.generate(List.of(A), null, null));
    }

    @Test
    void rejectsDuplicateEventIds() {
        EventDefinition anotherEventWithBId = event(B.getId(), 4, "D");
        assertThrows(IllegalArgumentException.class,
                () -> engine.generate(List.of(A, B, anotherEventWithBId), FaultType.NORMAL, null));
    }

    @Test
    void rejectsDuplicateSequenceNumbers() {
        EventDefinition anotherEventAtBPosition = event(4, B.getSequenceNo(), "D");
        assertThrows(IllegalArgumentException.class,
                () -> engine.generate(List.of(A, B, anotherEventAtBPosition), FaultType.NORMAL, null));
    }

    @Test
    void rejectsNullEventInList() {
        List<EventDefinition> input = new ArrayList<>(List.of(A, B));
        input.add(null);
        assertThrows(IllegalArgumentException.class,
                () -> engine.generate(input, FaultType.NORMAL, null));
    }

    @Test
    void emptyDuplicateFlowHasNoValidTarget() {
        assertThrows(IllegalArgumentException.class,
                () -> engine.generate(List.of(), FaultType.DUPLICATE, A.getId()));
    }

    @Test
    void emptyDropFlowHasNoValidTarget() {
        assertThrows(IllegalArgumentException.class,
                () -> engine.generate(List.of(), FaultType.DROP, A.getId()));
    }

    private static EventDefinition event(long id, int sequenceNo, String name) {
        return new EventDefinition(id, 1, sequenceNo, name, null, null);
    }

    private static void assertTimeline(List<GeneratedEvent> actual,
                                       List<EventDefinition> expectedSources,
                                       List<OccurrenceKind> expectedKinds) {
        assertEquals(expectedSources.size(), expectedKinds.size());
        assertEquals(expectedSources.size(), actual.size());

        for (int index = 0; index < actual.size(); index++) {
            assertEquals(index + 1, actual.get(index).getExecutionOrder());
            assertSame(expectedSources.get(index), actual.get(index).getSourceEvent());
            assertEquals(expectedKinds.get(index), actual.get(index).getOccurrenceKind());
        }
    }
}
