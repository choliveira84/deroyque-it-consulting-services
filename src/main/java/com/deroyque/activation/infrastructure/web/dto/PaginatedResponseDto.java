package com.deroyque.activation.infrastructure.web.dto;

import java.util.List;

public record PaginatedResponseDto<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
