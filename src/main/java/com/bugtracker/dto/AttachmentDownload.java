package com.bugtracker.dto;

import com.bugtracker.entity.Attachment;
import org.springframework.core.io.Resource;

/**
 * Pairs the attachment metadata with its binary content for streaming downloads.
 */
public record AttachmentDownload(Attachment attachment, Resource resource) {
}
