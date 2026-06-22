CREATE TABLE github_repo_board_mapping (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id   UUID NOT NULL REFERENCES workspace (id),
    github_repo    VARCHAR(200) NOT NULL,
    board_id       UUID NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uk_github_repo_board_mapping_github_repo UNIQUE (github_repo)
);

CREATE INDEX idx_github_repo_board_mapping_workspace_id ON github_repo_board_mapping (workspace_id);
