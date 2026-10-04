package com.bugtracker.controller;

import com.bugtracker.dto.AttachmentDownload;
import com.bugtracker.dto.AttachmentResponse;
import com.bugtracker.dto.BugAssignRequest;
import com.bugtracker.dto.BugDetailResponse;
import com.bugtracker.dto.BugFilterRequest;
import com.bugtracker.dto.BugPriorityUpdateRequest;
import com.bugtracker.dto.BugRequest;
import com.bugtracker.dto.BugSeverityUpdateRequest;
import com.bugtracker.dto.BugStatusUpdateRequest;
import com.bugtracker.dto.BugSummaryResponse;
import com.bugtracker.dto.BugUpdateRequest;
import com.bugtracker.dto.CommentRequest;
import com.bugtracker.dto.CommentResponse;
import com.bugtracker.dto.PageResponse;
import com.bugtracker.security.UserPrincipal;
import com.bugtracker.service.AttachmentService;
import com.bugtracker.service.BugService;
import com.bugtracker.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Bug CRUD, search/filter and workflow endpoints.
 */
@RestController
@RequestMapping("/api/bugs")
public class BugController {

    private final BugService bugService;
    private final CommentService commentService;
    private final AttachmentService attachmentService;

    public BugController(BugService bugService,
                         CommentService commentService,
                         AttachmentService attachmentService) {
        this.bugService = bugService;
        this.commentService = commentService;
        this.attachmentService = attachmentService;
    }

    /**
     * Paged, searchable, filterable and sortable bug list.
     * Example: {@code /api/bugs?search=login&status=OPEN&severity=HIGH&page=0&size=10&sortBy=createdAt&sortDirection=desc}
     */
    @GetMapping
    public ResponseEntity<PageResponse<BugSummaryResponse>> list(
            @ModelAttribute BugFilterRequest filter,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(bugService.searchBugs(filter, principal));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BugDetailResponse> get(@PathVariable Long id,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(bugService.getBug(id, principal));
    }

    @PostMapping
    public ResponseEntity<BugDetailResponse> create(@Valid @RequestBody BugRequest request,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bugService.createBug(request, principal));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BugDetailResponse> update(@PathVariable Long id,
                                                    @Valid @RequestBody BugUpdateRequest request,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(bugService.updateBug(id, request, principal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        bugService.deleteBug(id, principal);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<BugDetailResponse> changeStatus(@PathVariable Long id,
                                                          @Valid @RequestBody BugStatusUpdateRequest request,
                                                          @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(bugService.changeStatus(id, request, principal));
    }

    @PatchMapping("/{id}/priority")
    public ResponseEntity<BugDetailResponse> changePriority(@PathVariable Long id,
                                                            @Valid @RequestBody BugPriorityUpdateRequest request,
                                                            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(bugService.changePriority(id, request, principal));
    }

    @PatchMapping("/{id}/severity")
    public ResponseEntity<BugDetailResponse> changeSeverity(@PathVariable Long id,
                                                            @Valid @RequestBody BugSeverityUpdateRequest request,
                                                            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(bugService.changeSeverity(id, request, principal));
    }

    @PatchMapping("/{id}/assign")
    public ResponseEntity<BugDetailResponse> assign(@PathVariable Long id,
                                                    @RequestBody BugAssignRequest request,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(bugService.assignDeveloper(id, request, principal));
    }

    // ------------------------------------------------------------------
    // Comments
    // ------------------------------------------------------------------

    @GetMapping("/{id}/comments")
    public ResponseEntity<List<CommentResponse>> listComments(@PathVariable Long id,
                                                              @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(commentService.getComments(id, principal));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentResponse> addComment(@PathVariable Long id,
                                                      @Valid @RequestBody CommentRequest request,
                                                      @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.addComment(id, request, principal));
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id,
                                              @PathVariable Long commentId,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        commentService.deleteComment(commentId, principal);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------
    // Attachments
    // ------------------------------------------------------------------

    @GetMapping("/{id}/attachments")
    public ResponseEntity<List<AttachmentResponse>> listAttachments(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(attachmentService.getAttachments(id, principal));
    }

    @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttachmentResponse> uploadAttachment(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attachmentService.upload(id, file, principal));
    }

    /**
     * Streams the stored evidence back to the caller. Images are served inline so
     * they can be previewed, other types are downloaded as an attachment.
     */
    @GetMapping("/{id}/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long id,
                                                       @PathVariable Long attachmentId,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        AttachmentDownload download = attachmentService.download(id, attachmentId, principal);
        String contentType = download.attachment().getContentType();
        MediaType mediaType = contentType == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(contentType);
        boolean inline = download.attachment().getStoredFileName() != null;
        ContentDisposition disposition = (inline
                ? ContentDisposition.inline()
                : ContentDisposition.attachment())
                .filename(download.attachment().getOriginalFileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentLength(download.attachment().getFileSize())
                .body(download.resource());
    }

    @DeleteMapping("/{id}/attachments/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(@PathVariable Long id,
                                                 @PathVariable Long attachmentId,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        attachmentService.delete(id, attachmentId, principal);
        return ResponseEntity.noContent().build();
    }
}
