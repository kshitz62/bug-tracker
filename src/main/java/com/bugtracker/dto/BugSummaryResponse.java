package com.bugtracker.dto;

import com.bugtracker.entity.BugStatus;
import com.bugtracker.entity.Priority;
import com.bugtracker.entity.Severity;

import java.time.Instant;

/**
 * Row projection used by the bug list / search results table.
 */
public record BugSummaryResponse(
        Long id,
        String bugCode,
        String title,
        Severity severity,
        Priority priority,
        BugStatus status,
        String environment,
        Long reporterId,
        String reporterName,
        Long assignedDeveloperId,
        String assignedDeveloperName,
        long commentCount,
        long attachmentCount,
        Instant createdAt,
        Instant updatedAt
) {
}
