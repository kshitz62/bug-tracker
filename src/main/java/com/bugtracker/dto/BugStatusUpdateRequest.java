package com.bugtracker.dto;

import com.bugtracker.entity.BugStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Workflow transition request. {@code resolution} is required when moving a bug
 * to RESOLVED and is mandatory before closing a bug.
 */
public record BugStatusUpdateRequest(

        @NotNull(message = "Please select a status.")
        BugStatus status,

        @Size(max = 3000, message = "Resolution must not exceed 3000 characters.")
        String resolution
) {
}
