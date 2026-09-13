package com.eric.eBank.transaction.idempotency.repo;

import com.eric.eBank.transaction.idempotency.entity.TransferIdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransferIdempotencyRecordRepo extends JpaRepository<TransferIdempotencyRecord, Long> {
    Optional<TransferIdempotencyRecord> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);
}
