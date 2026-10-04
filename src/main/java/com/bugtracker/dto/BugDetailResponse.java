package com.bugtracker.dto;

import com.bugtracker.entity.BugStatus;
import com.bugtracker.entity.Priority;
import com.bugtracker.entity.Severity;

import java.time.Instant;
import java.util.List;

/**
 * Full bug representation returned by {@code GET /api/bugs/{id}}.
 */
public record BugDetailResponse(
        Long id,
        String bugCode,
        String title,
        String description,
        String stepsToReproduce,
        String expectedResult,
        String actualResult,
        String environment,
        Severity severity,
        Priority priority,
        BugStatus status,
        String resolution,
        Long reporterId,
        String reporterName,
        String reporterEmail,
        Long assignedDeveloperId,
        String assignedDeveloperName,
        String assignedDeveloperEmail,
        Instant createdAt,
        Instant updatedAt,
        List<CommentResponse> comments,
        List<AttachmentResponse> attachments,
        List<BugHistoryResponse> history
) {
}
