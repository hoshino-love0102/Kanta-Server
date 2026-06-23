ALTER TABLE outbox_event ADD COLUMN event_id UUID;
UPDATE outbox_event SET event_id = id WHERE event_id IS NULL;
ALTER TABLE outbox_event ALTER COLUMN event_id SET NOT NULL;
ALTER TABLE outbox_event ADD CONSTRAINT uk_outbox_event_event_id UNIQUE (event_id);

COMMENT ON COLUMN outbox_event.event_id IS '컨슈머 idempotency key (= event payload의 eventId)';
