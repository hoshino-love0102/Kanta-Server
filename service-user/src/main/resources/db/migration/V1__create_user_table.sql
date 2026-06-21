CREATE TABLE app_user (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    display_name  VARCHAR(100) NOT NULL,
    role          VARCHAR(32)  NOT NULL DEFAULT 'MEMBER',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT chk_user_role CHECK (role IN ('MEMBER', 'ADMIN'))
);

CREATE INDEX idx_app_user_email ON app_user (email);

COMMENT ON TABLE app_user IS 'User 원장(source of truth). service-auth는 이 레코드를 직접 참조하지 않고 UpsertUser gRPC 응답 또는 user.created/updated 이벤트로만 동기화.';
COMMENT ON COLUMN app_user.role IS 'global role (workspace별 role과는 별개, service-workspace의 member.role과 혼동 주의)';
