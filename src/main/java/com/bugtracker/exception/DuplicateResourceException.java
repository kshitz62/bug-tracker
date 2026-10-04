package com.bugtracker.exception;

/**
 * Thrown when creating or updating would violate a uniqueness constraint
 * (for example a duplicate email). Mapped to HTTP 409.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
