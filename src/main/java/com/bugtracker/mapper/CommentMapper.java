package com.bugtracker.mapper;

import com.bugtracker.dto.CommentResponse;
import com.bugtracker.entity.Comment;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
public class CommentMapper {

    public CommentResponse toResponse(Comment comment) {
        if (comment == null) {
            return null;
        }
        return new CommentResponse(
                comment.getId(),
                comment.getBug() == null ? null : comment.getBug().getId(),
                comment.getAuthor() == null ? null : comment.getAuthor().getId(),
                comment.getAuthor() == null ? null : comment.getAuthor().getFullName(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }

    public List<CommentResponse> toResponseList(Collection<Comment> comments) {
        return comments.stream().map(this::toResponse).toList();
    }
}
