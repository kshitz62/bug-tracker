package com.bugtracker.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Single-row-per-key counter table used to generate gap-free, human readable
 * bug codes (BUG-0001, BUG-0002, ...) without relying on the auto increment id.
 */
@Entity
@Table(name = "sequences")
public class SequenceCounter {

    public static final String BUG_CODE_SEQUENCE = "bug_code";

    @Id
    @Column(name = "sequence_name", length = 50)
    private String name;

    @Column(name = "next_value", nullable = false)
    private long nextValue;

    public SequenceCounter() {
    }

    public SequenceCounter(String name, long nextValue) {
        this.name = name;
        this.nextValue = nextValue;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getNextValue() {
        return nextValue;
    }

    public void setNextValue(long nextValue) {
        this.nextValue = nextValue;
    }
}
