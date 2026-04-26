package com.trackify.common.exception;

/**
 * Thrown when a create/update would violate a uniqueness constraint that the
 * application can pre-check and surface as a meaningful 409. Mapped to
 * HTTP 409 by {@link GlobalExceptionHandler}.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
