package com.deroyque.activation.infrastructure.persistence.entity;

import com.deroyque.activation.domain.model.OutboxMessage;
import com.deroyque.activation.domain.model.OutboxStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "activation_outbox")
public class ActivationOutboxEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OutboxStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ActivationOutboxEntity() {
    }

    public ActivationOutboxEntity(UUID id,
                                  UUID requestId,
                                  String idempotencyKey,
                                  String payload,
                                  OutboxStatus status,
                                  int retryCount,
                                  Instant nextRetryAt,
                                  Instant createdAt,
                                  Instant updatedAt) {
        this.id = id;
        this.requestId = requestId;
        this.idempotencyKey = idempotencyKey;
        this.payload = payload;
        this.status = status;
        this.retryCount = retryCount;
        this.nextRetryAt = nextRetryAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ActivationOutboxEntity fromDomain(OutboxMessage domain) {
        return new ActivationOutboxEntity(
                domain.getId(),
                domain.getRequestId(),
                domain.getIdempotencyKey(),
                domain.getPayload(),
                domain.getStatus(),
                domain.getRetryCount(),
                domain.getNextRetryAt(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }

    public OutboxMessage toDomain() {
        return new OutboxMessage(
                this.id,
                this.requestId,
                this.idempotencyKey,
                this.payload,
                this.status,
                this.retryCount,
                this.nextRetryAt,
                this.createdAt,
                this.updatedAt
        );
    }

    public UUID getId() { return id; }
    public UUID getRequestId() { return requestId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getPayload() { return payload; }
    public OutboxStatus getStatus() { return status; }
    public int getRetryCount() { return retryCount; }
    public Instant getNextRetryAt() { return nextRetryAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
