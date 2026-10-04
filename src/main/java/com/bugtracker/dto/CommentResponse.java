package com.bugtracker.dto;

import java.time.Instant;

public record CommentResponse(
        Long id,
        Long bugId,
        Long authorId,
        String authorName,
        String content,
        Instant createdAt
) {
}
