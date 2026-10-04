package com.bugtracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(

        @NotBlank(message = "Comment cannot be empty.")
        @Size(max = 2000, message = "Comment must not exceed 2000 characters.")
        String content
) {
}
