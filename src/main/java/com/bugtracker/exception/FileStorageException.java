package com.bugtracker.exception;

/**
 * Thrown when file storage fails validation or the underlying write fails.
 * Mapped to HTTP 400.
 */
public class FileStorageException extends RuntimeException {

    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
