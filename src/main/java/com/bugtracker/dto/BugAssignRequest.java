package com.bugtracker.dto;

/**
 * Assignment request. Sending {@code null} (or omitting the field) un-assigns the bug.
 */
public record BugAssignRequest(
        Long assignedDeveloperId
) {
}
