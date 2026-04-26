package com.trackify.common.exception;

/**
 * Thrown by application services when a requested resource cannot be found
 * or the caller is not allowed to see it. Mapped to HTTP 404 by
 * {@link GlobalExceptionHandler}.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
