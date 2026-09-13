CREATE TABLE transfer_idempotency_records (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    idempotency_key CHAR(36) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    transfer_reference CHAR(36) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    CONSTRAINT uk_transfer_idempotency_user_key
      UNIQUE (user_id, idempotency_key),

    CONSTRAINT uk_transfer_idempotency_reference
      UNIQUE (transfer_reference),

    CONSTRAINT fk_transfer_idempotency_user
      FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE = InnoDB;