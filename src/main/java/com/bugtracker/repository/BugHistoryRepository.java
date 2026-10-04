package com.bugtracker.repository;

import com.bugtracker.entity.BugHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BugHistoryRepository extends JpaRepository<BugHistory, Long> {

    List<BugHistory> findByBugIdOrderByChangedAtDesc(Long bugId);
}
