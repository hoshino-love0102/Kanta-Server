CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE meeting_note (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    board_id         UUID NOT NULL,
    creator_user_id  VARCHAR(120) NOT NULL,
    raw_text         TEXT NOT NULL,
    status           VARCHAR(20) NOT NULL,
    ai_summary       TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ
);

CREATE INDEX idx_meeting_note_board_id ON meeting_note (board_id);

CREATE TABLE action_item_candidate (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    meeting_note_id     UUID NOT NULL REFERENCES meeting_note (id) ON DELETE CASCADE,
    title               VARCHAR(500) NOT NULL,
    assignee_hint       VARCHAR(120),
    due_date_hint       VARCHAR(20),
    assignee_member_id  UUID,
    due_date            DATE,
    registered          BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_action_item_candidate_meeting_note_id ON action_item_candidate (meeting_note_id);
