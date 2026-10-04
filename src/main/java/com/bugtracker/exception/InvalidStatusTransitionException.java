package com.bugtracker.exception;

import com.bugtracker.entity.BugStatus;

/**
 * Thrown when a caller attempts a status transition that the workflow does not
 * allow (for example CLOSED to IN_PROGRESS without reopening). Mapped to HTTP 409.
 */
public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(BugStatus from, BugStatus to) {
        super("Invalid status transition from " + from + " to " + to + ".");
    }

    public InvalidStatusTransitionException(String message) {
        super(message);
    }
}
