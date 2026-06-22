CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE github_webhook_event (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    delivery_id   VARCHAR(120) NOT NULL,
    payload_hash  VARCHAR(64) NOT NULL,
    event_type    VARCHAR(40) NOT NULL,
    received_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uk_github_webhook_event_delivery_id UNIQUE (delivery_id)
);

CREATE TABLE commit_card_link (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    board_id               UUID NOT NULL,
    commit_sha             VARCHAR(64) NOT NULL,
    commit_message         TEXT NOT NULL,
    candidate_card_id      UUID,
    candidate_card_title   VARCHAR(500),
    similarity_score       DOUBLE PRECISION,
    match_status           VARCHAR(24) NOT NULL,
    card_id                UUID,
    new_card_id            UUID,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ
);

CREATE INDEX idx_commit_card_link_match_status ON commit_card_link (match_status);
