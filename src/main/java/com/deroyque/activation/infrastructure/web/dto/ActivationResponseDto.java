package com.deroyque.activation.infrastructure.web.dto;

import com.deroyque.activation.domain.model.ActivationRequest;
import com.deroyque.activation.domain.model.ActivationStatus;

import java.time.Instant;
import java.util.UUID;

public record ActivationResponseDto(
        UUID id,
        String externalId,
        String customerId,
        String productCode,
        ActivationStatus status,
        String failureReason,
        Instant createdAt,
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
