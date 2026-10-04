package com.bugtracker.mapper;

import com.bugtracker.dto.BugHistoryResponse;
import com.bugtracker.entity.BugHistory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
public class BugHistoryMapper {

    public BugHistoryResponse toResponse(BugHistory history) {
        if (history == null) {
            return null;
        }
        return new BugHistoryResponse(
                history.getId(),
                history.getFieldName(),
                history.getOldValue(),
                history.getNewValue(),
                history.getChangedBy() == null ? null : history.getChangedBy().getFullName(),
                history.getChangedAt()
        );
    }

    public List<BugHistoryResponse> toResponseList(Collection<BugHistory> history) {
        return history.stream().map(this::toResponse).toList();
    }
}
