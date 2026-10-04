package com.bugtracker.repository;

import com.bugtracker.entity.Bug;
import com.bugtracker.entity.BugStatus;
import com.bugtracker.entity.Priority;
import com.bugtracker.entity.Severity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BugRepository extends JpaRepository<Bug, Long>, JpaSpecificationExecutor<Bug> {

    Optional<Bug> findByBugCodeIgnoreCase(String bugCode);

    boolean existsByBugCodeIgnoreCase(String bugCode);

    long countByStatus(BugStatus status);

    long countBySeverity(Severity severity);

    long countByPriority(Priority priority);

    long countByAssignedDeveloperId(Long developerId);

    long countByAssignedDeveloperIdAndStatus(Long developerId, BugStatus status);

    long countByAssignedDeveloperIsNull();

    /**
     * @return aggregated {@code [BugStatus, Long]} pairs for the dashboard status chart.
     */
    @Query("select b.status, count(b) from Bug b group by b.status order by b.status asc")
    List<Object[]> countGroupedByStatus();

    /**
     * @return aggregated {@code [Severity, Long]} pairs for the dashboard severity chart.
     */
    @Query("select b.severity, count(b) from Bug b group by b.severity order by b.severity asc")
    List<Object[]> countGroupedBySeverity();

    /**
     * @return aggregated {@code [Priority, Long]} pairs for the dashboard priority chart.
     */
    @Query("select b.priority, count(b) from Bug b group by b.priority order by b.priority asc")
    List<Object[]> countGroupedByPriority();

    /**
     * Used by the reporter based dashboard view.
     */
    @Query("select count(b) from Bug b where b.reporter.id = :reporterId")
    long countByReporterId(@Param("reporterId") Long reporterId);

    Page<Bug> findByAssignedDeveloperId(Long developerId, Pageable pageable);
}
