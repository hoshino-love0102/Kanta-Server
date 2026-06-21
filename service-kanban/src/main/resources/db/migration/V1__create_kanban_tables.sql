CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE board (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL,
    name         VARCHAR(120) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_board_workspace_id ON board (workspace_id);

CREATE TABLE card (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    board_id            UUID NOT NULL REFERENCES board (id),
    title               VARCHAR(200) NOT NULL,
    status              VARCHAR(32) NOT NULL,
    source              VARCHAR(32) NOT NULL,
    assignee_member_id  UUID,
    due_date            DATE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_card_board_id ON card (board_id);
CREATE INDEX idx_card_board_status ON card (board_id, status);
CREATE INDEX idx_card_assignee_member_id ON card (assignee_member_id);
