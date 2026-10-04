package com.bugtracker.dto;

import java.util.List;

/**
 * Framework agnostic page envelope so that clients do not depend on Spring's
 * internal {@code Page} JSON shape.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        String sortBy,
        String sortDirection
) {
    public static <T> PageResponse<T> of(List<T> content,
                                         int page,
                                         int size,
                                         long totalElements,
                                         String sortBy,
                                         String sortDirection) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(
                content,
                page,
                size,
                totalElements,
                totalPages,
                page <= 0,
                page >= totalPages - 1,
                sortBy,
                sortDirection
        );
    }
}
