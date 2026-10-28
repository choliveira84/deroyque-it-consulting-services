package com.deroyque.activation.infrastructure.web.dto;

import com.deroyque.activation.domain.model.ActivationRequest;
import com.deroyque.activation.domain.model.ActivationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Representation of an activation request and its lifecycle status")
public record ActivationResponseDto(
        @Schema(description = "Internal unique UUID of the activation request", example = "d3b07384-d113-4ec6-8968-36a46dc49f50")
        UUID id,

        @Schema(description = "Client idempotency key", example = "REQ-EXT-001")
        String externalId,

        @Schema(description = "Customer identifier", example = "CUST-1001")
        String customerId,

        @Schema(description = "Product SKU code", example = "FIBER-500")
        String productCode,

        @Schema(description = "Current lifecycle status of the activation request", example = "PENDING")
        ActivationStatus status,

        @Schema(description = "Error reason if the activation request failed", example = "Provider rejected activation", nullable = true)
        String failureReason,

        @Schema(description = "Timestamp when the request was initially accepted", example = "2026-10-28T18:50:00Z")
        Instant createdAt,

        @Schema(description = "Timestamp when the request was last updated", example = "2026-10-28T18:50:01Z")
        Instant updatedAt
) {
    public static ActivationResponseDto fromDomain(ActivationRequest domain) {
        return new ActivationResponseDto(
                domain.getId(),
                domain.getExternalId(),
                domain.getCustomerId(),
                domain.getProductCode(),
                domain.getStatus(),
                domain.getFailureReason(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}
