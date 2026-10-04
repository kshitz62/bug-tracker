package com.bugtracker.dto;

import com.bugtracker.entity.Severity;
import jakarta.validation.constraints.NotNull;

public record BugSeverityUpdateRequest(

        @NotNull(message = "Please select a severity.")
        Severity severity
) {
}
