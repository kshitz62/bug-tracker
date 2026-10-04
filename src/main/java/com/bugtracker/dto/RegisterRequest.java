package com.bugtracker.dto;

import com.bugtracker.entity.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Self-service registration payload. Only TESTER and DEVELOPER may be self selected.
 */
public record RegisterRequest(

        @NotBlank(message = "Full name is required.")
        @Size(min = 3, max = 100, message = "Full name must be between 3 and 100 characters.")
        String fullName,

        @NotBlank(message = "Email is required.")
        @Email(message = "Please provide a valid email address.")
        @Size(max = 150, message = "Email must not exceed 150 characters.")
        String email,

        @NotBlank(message = "Password is required.")
        @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "Password must contain at least one letter and one number."
        )
        String password,

        @NotNull(message = "Please select a role.")
        RoleName role
) {
}
