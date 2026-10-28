package com.deroyque.activation.domain.model;

import com.deroyque.activation.domain.exception.InvalidStateTransitionException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class ActivationRequest {

    private final UUID id;
    private final String externalId;
    private final String customerId;
    private final String productCode;
    private ActivationStatus status;
    private String failureReason;
    private final Long version;
    private final Instant createdAt;
    private Instant updatedAt;

    public ActivationRequest(UUID id,
                             String externalId,
                             String customerId,
                             String productCode,
                             ActivationStatus status,
                             String failureReason,
                             Long version,
                             Instant createdAt,
                             Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.externalId = Objects.requireNonNull(externalId, "externalId must not be null");
        this.customerId = Objects.requireNonNull(customerId, "customerId must not be null");
        this.productCode = Objects.requireNonNull(productCode, "productCode must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.failureReason = failureReason;
        this.version = version;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static ActivationRequest create(String externalId, String customerId, String productCode) {
        if (externalId == null || externalId.isBlank()) {
            throw new IllegalArgumentException("externalId must not be blank");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customerId must not be blank");
        }
        if (productCode == null || productCode.isBlank()) {
            throw new IllegalArgumentException("productCode must not be blank");
        }

        Instant now = Instant.now();
        return new ActivationRequest(
                UUID.randomUUID(),
                externalId.trim(),
                customerId.trim(),
                productCode.trim(),
                ActivationStatus.PENDING,
                null,
                0L,
                now,
                now
        );
    }

    public void startProcessing() {
        if (this.status.isTerminal()) {
            throw new InvalidStateTransitionException(this.status, ActivationStatus.PROCESSING);
        }
        this.status = ActivationStatus.PROCESSING;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        if (this.status == ActivationStatus.COMPLETED) {
            return; // Idempotent
        }
        if (this.status != ActivationStatus.PROCESSING) {
            throw new InvalidStateTransitionException(this.status, ActivationStatus.COMPLETED);
        }
        this.status = ActivationStatus.COMPLETED;
        this.failureReason = null;
        this.updatedAt = Instant.now();
    }

    public void fail(String reason) {
        if (this.status == ActivationStatus.COMPLETED) {
            throw new InvalidStateTransitionException(this.status, ActivationStatus.FAILED);
        }
        this.status = ActivationStatus.FAILED;
        this.failureReason = reason;
        this.updatedAt = Instant.now();
    }

    public boolean matchesPayload(String customerId, String productCode) {
        return Objects.equals(this.customerId, customerId) && Objects.equals(this.productCode, productCode);
    }

    // Getters
    public UUID getId() { return id; }
    public String getExternalId() { return externalId; }
    public String getCustomerId() { return customerId; }
    public String getProductCode() { return productCode; }
    public ActivationStatus getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ActivationRequest that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
