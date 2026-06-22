CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE workspace (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name         VARCHAR(120) NOT NULL,
    github_org   VARCHAR(200),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE workspace_member (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id      UUID NOT NULL REFERENCES workspace (id),
    user_id           VARCHAR(120),
    email             VARCHAR(200),
    display_name      VARCHAR(120),
    github_username   VARCHAR(120),
    role              VARCHAR(20) NOT NULL,
    status            VARCHAR(20) NOT NULL,
    joined_at         TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uk_workspace_member_workspace_email UNIQUE (workspace_id, email)
);

CREATE INDEX idx_workspace_member_workspace_id ON workspace_member (workspace_id);
CREATE INDEX idx_workspace_member_user_id ON workspace_member (user_id);
