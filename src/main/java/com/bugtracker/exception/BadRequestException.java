package com.bugtracker.exception;

/**
 * Thrown for semantically invalid requests that pass bean validation,
 * e.g. assigning a bug to a user that is not a developer. Mapped to HTTP 400.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
