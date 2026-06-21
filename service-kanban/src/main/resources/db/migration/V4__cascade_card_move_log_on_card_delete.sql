ALTER TABLE card_move_log
    DROP CONSTRAINT card_move_log_card_id_fkey;

ALTER TABLE card_move_log
    ADD CONSTRAINT card_move_log_card_id_fkey
    FOREIGN KEY (card_id)
    REFERENCES card (id)
    ON DELETE CASCADE;
