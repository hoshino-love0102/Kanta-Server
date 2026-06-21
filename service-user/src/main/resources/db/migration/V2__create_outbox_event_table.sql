CREATE TABLE outbox_event (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type   VARCHAR(64) NOT NULL,
    aggregate_id     UUID NOT NULL,
    event_type       VARCHAR(100) NOT NULL,
    payload          JSONB NOT NULL,
    occurred_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at     TIMESTAMPTZ,
    publish_attempts INTEGER NOT NULL DEFAULT 0,
    last_error       TEXT
);

CREATE INDEX idx_outbox_event_unpublished
    ON outbox_event (occurred_at)
    WHERE published_at IS NULL;

COMMENT ON COLUMN outbox_event.event_type IS '''user.created'' | ''user.updated''';
