package com.deroyque.activation.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for creating or replaying a product activation")
public record CreateActivationRequestDto(
        @Schema(description = "Unique client-supplied idempotency key", example = "REQ-EXT-001", maxLength = 100)
        @NotBlank(message = "externalId is required and must not be blank")
        @Size(max = 100, message = "externalId cannot exceed 100 characters")
        String externalId,

        @Schema(description = "Unique customer identifier", example = "CUST-1001", maxLength = 100)
        @NotBlank(message = "customerId is required and must not be blank")
        @Size(max = 100, message = "customerId cannot exceed 100 characters")
        String customerId,

        @Schema(description = "Product or service SKU code to activate", example = "FIBER-500", maxLength = 100)
        @NotBlank(message = "productCode is required and must not be blank")
        @Size(max = 100, message = "productCode cannot exceed 100 characters")
        String productCode
) {
}
