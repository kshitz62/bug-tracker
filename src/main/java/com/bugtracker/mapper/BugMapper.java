package com.bugtracker.mapper;

import com.bugtracker.dto.BugDetailResponse;
import com.bugtracker.dto.BugRequest;
import com.bugtracker.dto.BugSummaryResponse;
import com.bugtracker.dto.BugUpdateRequest;
import com.bugtracker.entity.Bug;
import org.springframework.stereotype.Component;

/**
 * Converts between {@link Bug} entities and their API representations.
 */
@Component
public class BugMapper {

    private final CommentMapper commentMapper;
    private final AttachmentMapper attachmentMapper;
    private final BugHistoryMapper historyMapper;

    public BugMapper(CommentMapper commentMapper,
                     AttachmentMapper attachmentMapper,
                     BugHistoryMapper historyMapper) {
        this.commentMapper = commentMapper;
        this.attachmentMapper = attachmentMapper;
        this.historyMapper = historyMapper;
    }

    public Bug toEntity(BugRequest request) {
        Bug bug = new Bug();
        bug.setTitle(request.title().trim());
        bug.setDescription(request.description().trim());
        bug.setStepsToReproduce(request.stepsToReproduce().trim());
        bug.setExpectedResult(request.expectedResult().trim());
        bug.setActualResult(request.actualResult().trim());
        bug.setEnvironment(trimToNull(request.environment()));
        bug.setSeverity(request.severity());
        bug.setPriority(request.priority());
        return bug;
    }

    /**
     * Copies the editable fields of an update request onto the managed entity.
     * Status is intentionally not part of this payload.
     */
    public void applyUpdate(Bug bug, BugUpdateRequest request) {
        bug.setTitle(request.title().trim());
        bug.setDescription(request.description().trim());
        bug.setStepsToReproduce(request.stepsToReproduce().trim());
        bug.setExpectedResult(request.expectedResult().trim());
        bug.setActualResult(request.actualResult().trim());
        bug.setEnvironment(trimToNull(request.environment()));
        bug.setSeverity(request.severity());
        bug.setPriority(request.priority());
        bug.setResolution(trimToNull(request.resolution()));
    }

    public BugSummaryResponse toSummary(Bug bug) {
        return new BugSummaryResponse(
                bug.getId(),
                bug.getBugCode(),
                bug.getTitle(),
                bug.getSeverity(),
                bug.getPriority(),
                bug.getStatus(),
                bug.getEnvironment(),
                bug.getReporter() == null ? null : bug.getReporter().getId(),
                bug.getReporter() == null ? null : bug.getReporter().getFullName(),
                bug.getAssignedDeveloper() == null ? null : bug.getAssignedDeveloper().getId(),
                bug.getAssignedDeveloper() == null ? null : bug.getAssignedDeveloper().getFullName(),
                bug.getComments().size(),
                bug.getAttachments().size(),
                bug.getCreatedAt(),
                bug.getUpdatedAt()
        );
    }

    public BugDetailResponse toDetail(Bug bug) {
        return new BugDetailResponse(
                bug.getId(),
                bug.getBugCode(),
                bug.getTitle(),
                bug.getDescription(),
                bug.getStepsToReproduce(),
                bug.getExpectedResult(),
                bug.getActualResult(),
                bug.getEnvironment(),
                bug.getSeverity(),
                bug.getPriority(),
                bug.getStatus(),
                bug.getResolution(),
                bug.getReporter() == null ? null : bug.getReporter().getId(),
                bug.getReporter() == null ? null : bug.getReporter().getFullName(),
                bug.getReporter() == null ? null : bug.getReporter().getEmail(),
                bug.getAssignedDeveloper() == null ? null : bug.getAssignedDeveloper().getId(),
                bug.getAssignedDeveloper() == null ? null : bug.getAssignedDeveloper().getFullName(),
                bug.getAssignedDeveloper() == null ? null : bug.getAssignedDeveloper().getEmail(),
                bug.getCreatedAt(),
                bug.getUpdatedAt(),
                commentMapper.toResponseList(bug.getComments()),
                attachmentMapper.toResponseList(bug.getAttachments()),
                historyMapper.toResponseList(bug.getHistory())
        );
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
