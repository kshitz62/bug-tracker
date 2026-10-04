package com.bugtracker.dto;

import com.bugtracker.entity.RoleName;
import jakarta.validation.constraints.NotNull;

public record UserRoleUpdateRequest(

        @NotNull(message = "Please select a role.")
        RoleName role
) {
}
