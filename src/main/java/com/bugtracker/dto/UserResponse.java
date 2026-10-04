package com.bugtracker.dto;

import com.bugtracker.entity.RoleName;

import java.time.Instant;

public record UserResponse(
        Long id,
        String fullName,
        String email,
        RoleName role,
        boolean enabled,
        Instant createdAt
) {
}
