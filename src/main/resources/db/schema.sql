BEGIN;

CREATE TABLE projects (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(300),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT projects_name_not_blank CHECK (btrim(name) <> '')
);

CREATE TABLE events (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE RESTRICT,
    sequence_no INTEGER NOT NULL,
    event_name VARCHAR(100) NOT NULL,
    sample_payload JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT events_project_sequence_unique UNIQUE (project_id, sequence_no),
    CONSTRAINT events_sequence_positive CHECK (sequence_no > 0),
    CONSTRAINT events_name_not_blank CHECK (btrim(event_name) <> '')
);

CREATE TABLE experiments (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE RESTRICT,
    name VARCHAR(120) NOT NULL,
    fault_type VARCHAR(10) NOT NULL,
    target_event_id BIGINT REFERENCES events(id) ON DELETE RESTRICT,
    status VARCHAR(10) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT experiments_name_not_blank CHECK (btrim(name) <> ''),
    CONSTRAINT experiments_fault_type_valid CHECK (fault_type IN ('NORMAL', 'DUPLICATE', 'DROP')),
    CONSTRAINT experiments_status_valid CHECK (status IN ('PENDING', 'COMPLETED')),
    CONSTRAINT experiments_target_matches_fault CHECK (
        (fault_type = 'NORMAL' AND target_event_id IS NULL)
        OR (fault_type IN ('DUPLICATE', 'DROP') AND target_event_id IS NOT NULL)
    )
);

CREATE TABLE experiment_events (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    experiment_id BIGINT NOT NULL REFERENCES experiments(id) ON DELETE RESTRICT,
    event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE RESTRICT,
    execution_order INTEGER NOT NULL,
    occurrence_kind VARCHAR(10) NOT NULL,
    CONSTRAINT experiment_events_order_unique UNIQUE (experiment_id, execution_order),
    CONSTRAINT experiment_events_order_positive CHECK (execution_order > 0),
    CONSTRAINT experiment_events_kind_valid CHECK (occurrence_kind IN ('NORMAL', 'DUPLICATE'))
);

COMMIT;
