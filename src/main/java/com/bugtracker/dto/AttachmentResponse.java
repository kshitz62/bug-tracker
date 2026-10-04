package com.bugtracker.dto;

import java.time.Instant;

public record AttachmentResponse(
        Long id,
        Long bugId,
        String originalFileName,
        String contentType,
        long fileSize,
        String uploadedByName,
        Instant uploadedAt,
        String downloadUrl
) {
}
