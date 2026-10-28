package com.deroyque.activation.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Generic paginated response envelope")
public record PaginatedResponseDto<T>(
        @Schema(description = "List of items in the current page")
        List<T> content,

        @Schema(description = "Zero-based page index", example = "0")
        int page,

        @Schema(description = "Page size", example = "20")
        int size,

        @Schema(description = "Total number of elements across all pages", example = "42")
        long totalElements,

        @Schema(description = "Total number of pages", example = "3")
        int totalPages
) {
}
