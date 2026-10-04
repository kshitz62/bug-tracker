package com.bugtracker.repository;

import com.bugtracker.entity.SequenceCounter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SequenceCounterRepository extends JpaRepository<SequenceCounter, String> {

    /**
     * Row level write lock guaranteeing unique, sequential bug codes under concurrency.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SequenceCounter s where s.name = :name")
    Optional<SequenceCounter> findWithLockByName(@Param("name") String name);
}
