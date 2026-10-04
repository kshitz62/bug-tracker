package com.bugtracker.dto;

import java.util.Map;

/**
 * Aggregated dashboard metrics plus chart distributions.
 */
public record DashboardStatisticsResponse(
        long totalBugs,
        long openBugs,
        long inProgressBugs,
        long resolvedBugs,
        long closedBugs,
        long reopenedBugs,
        long criticalBugs,
        long highPriorityBugs,
        long urgentPriorityBugs,
        long unassignedBugs,
        long assignedToMe,
        Map<String, Long> statusDistribution,
        Map<String, Long> severityDistribution,
        Map<String, Long> priorityDistribution
) {
}
