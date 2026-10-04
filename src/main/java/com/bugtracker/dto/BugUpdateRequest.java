package com.bugtracker.dto;

import com.bugtracker.entity.Priority;
import com.bugtracker.entity.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload used when editing an existing bug. Status is changed through the
 * dedicated workflow endpoint so that transition rules stay enforced.
 */
public record BugUpdateRequest(

        @NotBlank(message = "Title is required.")
        @Size(max = 200, message = "Title must not exceed 200 characters.")
        String title,

        @NotBlank(message = "Description is required.")
        @Size(max = 5000, message = "Description must not exceed 5000 characters.")
        String description,

        @NotBlank(message = "Steps to reproduce are required.")
        @Size(max = 5000, message = "Steps to reproduce must not exceed 5000 characters.")
        String stepsToReproduce,

        @NotBlank(message = "Expected result is required.")
        @Size(max = 3000, message = "Expected result must not exceed 3000 characters.")
        String expectedResult,

        @NotBlank(message = "Actual result is required.")
        @Size(max = 3000, message = "Actual result must not exceed 3000 characters.")
        String actualResult,

        @Size(max = 200, message = "Environment must not exceed 200 characters.")
        String environment,

        @NotNull(message = "Please select a severity.")
        Severity severity,

        @NotNull(message = "Please select a priority.")
        Priority priority,

        Long assignedDeveloperId,

        @Size(max = 3000, message = "Resolution must not exceed 3000 characters.")
        String resolution
) {
}
