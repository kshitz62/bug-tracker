package com.bugtracker.dto;

import com.bugtracker.entity.Priority;
import jakarta.validation.constraints.NotNull;

public record BugPriorityUpdateRequest(

        @NotNull(message = "Please select a priority.")
        Priority priority
) {
}
