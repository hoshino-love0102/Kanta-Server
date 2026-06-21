CREATE TABLE card_move_log (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    card_id              UUID NOT NULL REFERENCES card (id),
    moved_by_member_id   UUID,
    moved_by_type        VARCHAR(16) NOT NULL DEFAULT 'USER',
    from_status          VARCHAR(32) NOT NULL,
    to_status            VARCHAR(32) NOT NULL,
    moved_at             TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_moved_by_type CHECK (moved_by_type IN ('USER', 'SYSTEM')),
    CONSTRAINT chk_moved_by_consistency CHECK (
        (moved_by_type = 'USER' AND moved_by_member_id IS NOT NULL) OR
        (moved_by_type = 'SYSTEM' AND moved_by_member_id IS NULL)
    )
);

CREATE INDEX idx_card_move_log_card_id ON card_move_log (card_id);

COMMENT ON COLUMN card_move_log.moved_by_member_id IS 'moved_by_type=SYSTEM이면 NULL';
COMMENT ON COLUMN card_move_log.moved_by_type IS 'USER: API 명세 1.6 경로(PassportHolder로 계산) | SYSTEM: webhook 등 내부 이벤트 처리';
