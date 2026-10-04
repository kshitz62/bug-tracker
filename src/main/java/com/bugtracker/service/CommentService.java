package com.bugtracker.service;

import com.bugtracker.dto.CommentRequest;
import com.bugtracker.dto.CommentResponse;
import com.bugtracker.entity.Bug;
import com.bugtracker.entity.BugHistory;
import com.bugtracker.entity.Comment;
import com.bugtracker.entity.User;
import com.bugtracker.exception.ResourceNotFoundException;
import com.bugtracker.mapper.CommentMapper;
import com.bugtracker.repository.BugRepository;
import com.bugtracker.repository.CommentRepository;
import com.bugtracker.security.UserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Comments posted against bugs. Each addition is mirrored into the bug audit trail.
 */
@Service
public class CommentService {

    private static final int HISTORY_SNIPPET_LENGTH = 200;

    private final CommentRepository commentRepository;
    private final BugRepository bugRepository;
    private final UserService userService;
    private final CommentMapper commentMapper;
    private final BugAuthorizationService authorizationService;

    public CommentService(CommentRepository commentRepository,
                          BugRepository bugRepository,
                          UserService userService,
                          CommentMapper commentMapper,
                          BugAuthorizationService authorizationService) {
        this.commentRepository = commentRepository;
        this.bugRepository = bugRepository;
        this.userService = userService;
        this.commentMapper = commentMapper;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long bugId, UserPrincipal actor) {
        requireActor(actor);
        requireBug(bugId);
        return commentMapper.toResponseList(commentRepository.findByBugIdOrderByCreatedAtAsc(bugId));
    }

    @Transactional
    public CommentResponse addComment(Long bugId, CommentRequest request, UserPrincipal actor) {
        Bug bug = requireBug(bugId);
        authorizationService.assertCanComment(bug, actor);
        User author = userService.getEntityById(actor.getId());

        Comment comment = new Comment(author, request.content().trim());
        comment.setBug(bug);

        // Persist (and flush) the comment first so the response carries its id and timestamp.
        Comment saved = commentRepository.saveAndFlush(comment);

        bug.addComment(saved);
        bug.addHistory(new BugHistory("COMMENT_ADDED", null, snippet(request.content()), author));

        return commentMapper.toResponse(saved);
    }

    @Transactional
    public void deleteComment(Long commentId, UserPrincipal actor) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));
        requireActor(actor);
        boolean isAuthor = comment.getAuthor() != null
                && Objects.equals(comment.getAuthor().getId(), actor.getId());
        if (!isAuthor && !authorizationService.isAdmin(actor)) {
            throw new AccessDeniedException("Access denied. Only the comment author or an administrator may delete it.");
        }
        commentRepository.delete(comment);
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

    private static String snippet(String content) {
        String trimmed = content.trim();
        return trimmed.length() <= HISTORY_SNIPPET_LENGTH
                ? trimmed
                : trimmed.substring(0, HISTORY_SNIPPET_LENGTH) + "...";
    }
}
