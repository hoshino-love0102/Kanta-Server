CREATE TABLE oauth_account (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID NOT NULL REFERENCES principal (user_id),
    provider         VARCHAR(16) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    linked_at        TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_oauth_provider_account UNIQUE (provider, provider_user_id),
    CONSTRAINT uq_oauth_user_provider UNIQUE (user_id, provider),
    CONSTRAINT chk_oauth_provider CHECK (provider IN ('GITHUB', 'GOOGLE'))
);

CREATE INDEX idx_oauth_account_user_id ON oauth_account (user_id);
