CREATE TABLE outbox_event (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type  VARCHAR(64) NOT NULL,
    aggregate_id    UUID NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload         JSONB NOT NULL,
    occurred_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at    TIMESTAMPTZ,
    publish_attempts INTEGER NOT NULL DEFAULT 0,
    last_error      TEXT
);

CREATE INDEX idx_outbox_event_unpublished
    ON outbox_event (occurred_at)
    WHERE published_at IS NULL;

CREATE TABLE workspace_member_cache (
    id                   UUID PRIMARY KEY,
    workspace_id         UUID NOT NULL,
    user_id              VARCHAR(120) NOT NULL,
    display_name         VARCHAR(120) NOT NULL,
    role                 VARCHAR(50),
    active               BOOLEAN NOT NULL DEFAULT true,
    synced_at            TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uk_workspace_member_cache_workspace_user UNIQUE (workspace_id, user_id)
);

CREATE INDEX idx_workspace_member_cache_workspace_id
    ON workspace_member_cache (workspace_id);

CREATE INDEX idx_workspace_member_cache_user_id
    ON workspace_member_cache (user_id);
