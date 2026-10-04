package com.bugtracker.service;

import com.bugtracker.dto.AttachmentDownload;
import com.bugtracker.dto.AttachmentResponse;
import com.bugtracker.entity.Attachment;
import com.bugtracker.entity.Bug;
import com.bugtracker.entity.BugHistory;
import com.bugtracker.entity.User;
import com.bugtracker.exception.FileStorageException;
import com.bugtracker.exception.ResourceNotFoundException;
import com.bugtracker.mapper.AttachmentMapper;
import com.bugtracker.repository.AttachmentRepository;
import com.bugtracker.repository.BugRepository;
import com.bugtracker.security.UserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

/**
 * Uploads, lists, downloads and deletes bug evidence files.
 */
@Service
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final BugRepository bugRepository;
    private final UserService userService;
    private final FileStorageService fileStorageService;
    private final AttachmentMapper attachmentMapper;
    private final BugAuthorizationService authorizationService;

    public AttachmentService(AttachmentRepository attachmentRepository,
                             BugRepository bugRepository,
                             UserService userService,
                             FileStorageService fileStorageService,
                             AttachmentMapper attachmentMapper,
                             BugAuthorizationService authorizationService) {
        this.attachmentRepository = attachmentRepository;
        this.bugRepository = bugRepository;
        this.userService = userService;
        this.fileStorageService = fileStorageService;
        this.attachmentMapper = attachmentMapper;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> getAttachments(Long bugId, UserPrincipal actor) {
        requireActor(actor);
        requireBug(bugId);
        return attachmentMapper.toResponseList(attachmentRepository.findByBugIdOrderByUploadedAtAsc(bugId));
    }

    @Transactional
    public AttachmentResponse upload(Long bugId, MultipartFile file, UserPrincipal actor) {
        Bug bug = requireBug(bugId);
        authorizationService.assertCanComment(bug, actor);
        User uploader = userService.getEntityById(actor.getId());

        // Validates size, extension and content type before anything touches the disk.
        fileStorageService.validate(file);
        String storedName = fileStorageService.store(file);

        Attachment attachment = new Attachment();
        attachment.setOriginalFileName(safeDisplayName(file.getOriginalFilename()));
        attachment.setStoredFileName(storedName);
        attachment.setContentType(file.getContentType());
        attachment.setFileSize(file.getSize());
        attachment.setUploadedBy(uploader);
        attachment.setBug(bug);

        // Persist (and flush) the attachment first so the response carries its id.
        Attachment saved = attachmentRepository.saveAndFlush(attachment);

        bug.addAttachment(saved);
        bug.addHistory(new BugHistory("ATTACHMENT_ADDED", null, saved.getOriginalFileName(), uploader));

        return attachmentMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AttachmentDownload download(Long bugId, Long attachmentId, UserPrincipal actor) {
        requireActor(actor);
        Attachment attachment = requireAttachment(bugId, attachmentId);
        return new AttachmentDownload(attachment, fileStorageService.loadAsResource(attachment.getStoredFileName()));
    }

    @Transactional
    public void delete(Long bugId, Long attachmentId, UserPrincipal actor) {
        requireActor(actor);
        Attachment attachment = requireAttachment(bugId, attachmentId);
        boolean isUploader = attachment.getUploadedBy() != null
                && Objects.equals(attachment.getUploadedBy().getId(), actor.getId());
        if (!isUploader && !authorizationService.isAdmin(actor)) {
            throw new AccessDeniedException(
                    "Access denied. Only the uploader or an administrator may delete this attachment.");
        }
        fileStorageService.delete(attachment.getStoredFileName());
        Bug bug = attachment.getBug();
        bug.getAttachments().remove(attachment);
        bug.addHistory(new BugHistory("ATTACHMENT_REMOVED", attachment.getOriginalFileName(), null,
                userService.getEntityById(actor.getId())));
    }

    private Attachment requireAttachment(Long bugId, Long attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", attachmentId));
        if (attachment.getBug() == null || !Objects.equals(attachment.getBug().getId(), bugId)) {
            throw new ResourceNotFoundException("Attachment " + attachmentId + " does not belong to bug " + bugId);
        }
        return attachment;
    }

    private Bug requireBug(Long bugId) {
        return bugRepository.findById(bugId)
                .orElseThrow(() -> new ResourceNotFoundException("Bug", bugId));
    }

    private static void requireActor(UserPrincipal actor) {
        if (actor == null) {
            throw new AccessDeniedException("Authentication is required to access this resource.");
        }
    }

    /**
     * Keeps only the base name so a crafted upload name cannot escape the display
     * layer or the storage directory.
     */
    private static String safeDisplayName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            throw new FileStorageException("The uploaded file must have a name.");
        }
        String normalised = originalName.replace('\\', '/');
        int slashIndex = normalised.lastIndexOf('/');
        String baseName = slashIndex >= 0 ? normalised.substring(slashIndex + 1) : normalised;
        return baseName.trim();
    }
}
