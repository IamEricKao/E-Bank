ALTER TABLE transfer_idempotency_records
DROP INDEX uk_transfer_idempotency_reference,
DROP COLUMN transfer_reference,
    ADD COLUMN transfer_id BIGINT NULL AFTER request_hash,
    ADD CONSTRAINT uk_transfer_idempotency_transfer_id
        UNIQUE (transfer_id);