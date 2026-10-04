package com.bugtracker.service;

import com.bugtracker.entity.Bug;
import com.bugtracker.entity.BugStatus;
import com.bugtracker.entity.RoleName;
import com.bugtracker.exception.ResourceNotFoundException;
import com.bugtracker.repository.SequenceCounterRepository;
import com.bugtracker.entity.SequenceCounter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generates sequential, human readable bug codes (BUG-0001, BUG-0002, ...).
 * A pessimistic lock on the counter row keeps codes unique under concurrent writes.
 */
@Service
public class BugCodeGenerator {

    private final SequenceCounterRepository sequenceCounterRepository;

    public BugCodeGenerator(SequenceCounterRepository sequenceCounterRepository) {
        this.sequenceCounterRepository = sequenceCounterRepository;
    }

    @Transactional
    public String nextBugCode() {
        SequenceCounter counter = sequenceCounterRepository
                .findWithLockByName(SequenceCounter.BUG_CODE_SEQUENCE)
                .orElseGet(() -> sequenceCounterRepository.save(
                        new SequenceCounter(SequenceCounter.BUG_CODE_SEQUENCE, 1L)));
        long value = counter.getNextValue();
        counter.setNextValue(value + 1);
        sequenceCounterRepository.save(counter);
        return format(value);
    }

    static String format(long value) {
        return String.format("BUG-%04d", value);
    }
}
