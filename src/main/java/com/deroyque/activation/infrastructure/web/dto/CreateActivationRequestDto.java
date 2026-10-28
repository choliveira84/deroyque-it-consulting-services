package com.deroyque.activation.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateActivationRequestDto(
        @NotBlank(message = "externalId is required and must not be blank")
        @Size(max = 100, message = "externalId cannot exceed 100 characters")
        String externalId,

        @NotBlank(message = "customerId is required and must not be blank")
        @Size(max = 100, message = "customerId cannot exceed 100 characters")
        String customerId,

        @NotBlank(message = "productCode is required and must not be blank")
        @Size(max = 100, message = "productCode cannot exceed 100 characters")
        String productCode
) {
}
