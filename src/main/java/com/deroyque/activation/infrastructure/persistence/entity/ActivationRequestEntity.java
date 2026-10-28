package com.deroyque.activation.infrastructure.persistence.entity;

import com.deroyque.activation.domain.model.ActivationRequest;
import com.deroyque.activation.domain.model.ActivationStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "activation_requests", uniqueConstraints = {
        @UniqueConstraint(name = "uk_activation_requests_external_id", columnNames = "external_id")
})
public class ActivationRequestEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "external_id", nullable = false, length = 100, updatable = false)
    private String externalId;

    @Column(name = "customer_id", nullable = false, length = 100)
    private String customerId;

    @Column(name = "product_code", nullable = false, length = 100)
    private String productCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ActivationStatus status;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ActivationRequestEntity() {
    }

    public ActivationRequestEntity(UUID id,
                                   String externalId,
                                   String customerId,
                                   String productCode,
                                   ActivationStatus status,
                                   String failureReason,
                                   Long version,
                                   Instant createdAt,
                                   Instant updatedAt) {
        this.id = id;
        this.externalId = externalId;
        this.customerId = customerId;
        this.productCode = productCode;
        this.status = status;
        this.failureReason = failureReason;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ActivationRequestEntity fromDomain(ActivationRequest domain) {
        return new ActivationRequestEntity(
                domain.getId(),
                domain.getExternalId(),
                domain.getCustomerId(),
                domain.getProductCode(),
                domain.getStatus(),
                domain.getFailureReason(),
                domain.getVersion(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }

    public ActivationRequest toDomain() {
        return new ActivationRequest(
                this.id,
                this.externalId,
                this.customerId,
                this.productCode,
                this.status,
                this.failureReason,
                this.version,
                this.createdAt,
                this.updatedAt
        );
    }

    public UUID getId() { return id; }
    public String getExternalId() { return externalId; }
    public String getCustomerId() { return customerId; }
    public String getProductCode() { return productCode; }
    public ActivationStatus getStatus() { return status; }
    public void setStatus(ActivationStatus status) { this.status = status; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
