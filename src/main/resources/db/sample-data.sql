BEGIN;

WITH new_project AS (
    INSERT INTO projects (name, description)
    VALUES ('Payment Processing Demo', 'Sample ordered payment event flow for Review 1')
    RETURNING id
),
new_events AS (
    INSERT INTO events (project_id, sequence_no, event_name, sample_payload)
    SELECT project.id, sample.sequence_no, sample.event_name, sample.sample_payload
    FROM new_project AS project
    CROSS JOIN (
        VALUES
            (1, 'ORDER_CREATED', '{"orderId":"ORD-1001"}'::jsonb),
            (2, 'PAYMENT_COMPLETED', '{"orderId":"ORD-1001","paymentId":"PAY-1001"}'::jsonb),
            (3, 'INVENTORY_RESERVED', '{"orderId":"ORD-1001"}'::jsonb),
            (4, 'ORDER_CONFIRMED', '{"orderId":"ORD-1001"}'::jsonb)
    ) AS sample(sequence_no, event_name, sample_payload)
    RETURNING id, event_name
),
new_experiment AS (
    INSERT INTO experiments (project_id, name, fault_type, target_event_id, status)
    SELECT project.id, 'Duplicate payment completion', 'DUPLICATE', target.id, 'COMPLETED'
    FROM new_project AS project
    JOIN new_events AS target ON target.event_name = 'PAYMENT_COMPLETED'
    RETURNING id
)
INSERT INTO experiment_events (experiment_id, event_id, execution_order, occurrence_kind)
SELECT experiment.id, event.id, step.execution_order, step.occurrence_kind
FROM new_experiment AS experiment
CROSS JOIN (
    VALUES
        (1, 'ORDER_CREATED', 'NORMAL'),
        (2, 'PAYMENT_COMPLETED', 'NORMAL'),
        (3, 'PAYMENT_COMPLETED', 'DUPLICATE'),
        (4, 'INVENTORY_RESERVED', 'NORMAL'),
        (5, 'ORDER_CONFIRMED', 'NORMAL')
) AS step(execution_order, event_name, occurrence_kind)
JOIN new_events AS event ON event.event_name = step.event_name;

COMMIT;
