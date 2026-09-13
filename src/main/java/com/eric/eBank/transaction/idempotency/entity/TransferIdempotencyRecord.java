package com.eric.eBank.transaction.idempotency.entity;

import com.eric.eBank.auth_users.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "transfer_idempotency_records",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_transfer_idempotency_user_key",
                        columnNames = {"user_id", "idempotency_key"}
                )
        }
)
@Data
@NoArgsConstructor
public class TransferIdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "idempotency_key", nullable = false, length = 36)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "transfer_id", unique = true)
    private Long transferId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
