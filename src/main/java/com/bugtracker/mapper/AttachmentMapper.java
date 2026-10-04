package com.bugtracker.mapper;

import com.bugtracker.dto.AttachmentResponse;
import com.bugtracker.entity.Attachment;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
public class AttachmentMapper {

    public AttachmentResponse toResponse(Attachment attachment) {
        if (attachment == null) {
            return null;
        }
        Long bugId = attachment.getBug() == null ? null : attachment.getBug().getId();
        return new AttachmentResponse(
                attachment.getId(),
                bugId,
                attachment.getOriginalFileName(),
                attachment.getContentType(),
                attachment.getFileSize(),
                attachment.getUploadedBy() == null ? null : attachment.getUploadedBy().getFullName(),
                attachment.getUploadedAt(),
                bugId == null ? null : "/api/bugs/" + bugId + "/attachments/" + attachment.getId() + "/download"
        );
    }

    public List<AttachmentResponse> toResponseList(Collection<Attachment> attachments) {
        return attachments.stream().map(this::toResponse).toList();
    }
}
