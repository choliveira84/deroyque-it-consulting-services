package com.deroyque.activation.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class OutboxMessage {

    private final UUID id;
    private final UUID requestId;
    private final String idempotencyKey;
    private final String payload;
    private OutboxStatus status;
    private int retryCount;
    private Instant nextRetryAt;
    private final Instant createdAt;
    private Instant updatedAt;

    public OutboxMessage(UUID id,
                         UUID requestId,
                         String idempotencyKey,
                         String payload,
                         OutboxStatus status,
                         int retryCount,
                         Instant nextRetryAt,
                         Instant createdAt,
                         Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.requestId = Objects.requireNonNull(requestId, "requestId must not be null");
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        this.payload = Objects.requireNonNull(payload, "payload must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.retryCount = retryCount;
        this.nextRetryAt = nextRetryAt;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static OutboxMessage createPending(UUID requestId, String idempotencyKey, String payload) {
        Instant now = Instant.now();
        return new OutboxMessage(
                UUID.randomUUID(),
                requestId,
                idempotencyKey,
                payload,
                OutboxStatus.PENDING,
                0,
                now,
                now,
                now
        );
    }

    public void markSent() {
        this.status = OutboxStatus.SENT;
        this.updatedAt = Instant.now();
    }

    public void scheduleRetry(long backoffSeconds) {
        this.retryCount++;
        this.nextRetryAt = Instant.now().plusSeconds(backoffSeconds);
        this.updatedAt = Instant.now();
    }

    public void markFailed() {
        this.status = OutboxStatus.FAILED;
        this.updatedAt = Instant.now();
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
