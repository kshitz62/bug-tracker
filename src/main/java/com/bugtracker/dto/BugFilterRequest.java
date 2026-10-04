package com.bugtracker.dto;

import com.bugtracker.entity.BugStatus;
import com.bugtracker.entity.Priority;
import com.bugtracker.entity.Severity;

/**
 * Query parameters accepted by {@code GET /api/bugs}: searching, filtering,
 * sorting and pagination combined.
 */
public class BugFilterRequest {

    private static final int MAX_PAGE_SIZE = 100;

    /** Free text matched (case insensitive) against bug code, title and description. */
    private String search;

    private BugStatus status;

    private Severity severity;

    private Priority priority;

    private Long assigneeId;

    private Long reporterId;

    /** When true only unassigned bugs are returned. */
    private Boolean unassigned;

    private int page = 0;

    private int size = 10;

    private String sortBy = "createdAt";

    private String sortDirection = "desc";

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public BugStatus getStatus() {
        return status;
    }

    public void setStatus(BugStatus status) {
        this.status = status;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public Long getReporterId() {
        return reporterId;
    }

    public void setReporterId(Long reporterId) {
        this.reporterId = reporterId;
    }

    public Boolean getUnassigned() {
        return unassigned;
    }

    public void setUnassigned(Boolean unassigned) {
        this.unassigned = unassigned;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = Math.max(page, 0);
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy.trim();
    }

    public String getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(String sortDirection) {
        this.sortDirection = (sortDirection == null || sortDirection.isBlank()) ? "desc" : sortDirection.trim();
    }

    public boolean isDescending() {
        return !"asc".equalsIgnoreCase(sortDirection);
    }
}
