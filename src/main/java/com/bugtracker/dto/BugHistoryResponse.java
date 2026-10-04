package com.bugtracker.dto;

import java.time.Instant;

public record BugHistoryResponse(
        Long id,
        String fieldName,
        String oldValue,
        String newValue,
        String changedByName,
        Instant changedAt
) {
}
