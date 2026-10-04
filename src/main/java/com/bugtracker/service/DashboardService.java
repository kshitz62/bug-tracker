package com.bugtracker.service;

import com.bugtracker.dto.DashboardStatisticsResponse;
import com.bugtracker.entity.BugStatus;
import com.bugtracker.entity.Priority;
import com.bugtracker.entity.Severity;
import com.bugtracker.repository.BugRepository;
import com.bugtracker.security.UserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates the metrics rendered on the dashboard.
 */
@Service
public class DashboardService {

    private final BugRepository bugRepository;

    public DashboardService(BugRepository bugRepository) {
        this.bugRepository = bugRepository;
    }

    @Transactional(readOnly = true)
    public DashboardStatisticsResponse getStatistics(UserPrincipal actor) {
        if (actor == null) {
            throw new AccessDeniedException("Authentication is required to access this resource.");
        }
        long assignedToMe = actor.getId() == null ? 0L : bugRepository.countByAssignedDeveloperId(actor.getId());

        return new DashboardStatisticsResponse(
                bugRepository.count(),
                bugRepository.countByStatus(BugStatus.OPEN),
                bugRepository.countByStatus(BugStatus.IN_PROGRESS),
                bugRepository.countByStatus(BugStatus.RESOLVED),
                bugRepository.countByStatus(BugStatus.CLOSED),
                bugRepository.countByStatus(BugStatus.REOPENED),
                bugRepository.countBySeverity(Severity.CRITICAL),
                bugRepository.countByPriority(Priority.HIGH),
                bugRepository.countByPriority(Priority.URGENT),
                bugRepository.countByAssignedDeveloperIsNull(),
                assignedToMe,
                toDistribution(bugRepository.countGroupedByStatus(), BugStatus.values()),
                toDistribution(bugRepository.countGroupedBySeverity(), Severity.values()),
                toDistribution(bugRepository.countGroupedByPriority(), Priority.values())
        );
    }

    /**
     * Converts {@code [enum, count]} rows into an ordered map that always contains
     * every enum constant (zero filled) so charts render consistently.
     */
    private static Map<String, Long> toDistribution(List<Object[]> rows, Enum<?>[] allValues) {
        Map<String, Long> distribution = new LinkedHashMap<>();
        for (Enum<?> value : allValues) {
            distribution.put(value.name(), 0L);
        }
        for (Object[] row : rows) {
            Object key = row[0];
            Object count = row[1];
            if (key instanceof Enum<?> enumKey && count instanceof Number number) {
                distribution.put(enumKey.name(), number.longValue());
            }
        }
        return distribution;
    }
}
