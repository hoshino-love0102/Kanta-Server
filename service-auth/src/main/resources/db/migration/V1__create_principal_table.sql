CREATE TABLE principal (
    user_id       UUID PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    display_name  VARCHAR(100) NOT NULL,
    role          VARCHAR(32)  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

COMMENT ON TABLE principal IS 'user-service 원장의 로컬 캐시. OAuth callback에서 gRPC 응답으로 즉시 upsert(선반영), user.created/updated Kafka로 후속 idempotent upsert.';
