package com.trackify.common.exception;

/**
 * Thrown by application services when the caller is authenticated but is not
 * permitted to access the requested resource. Mapped to HTTP 403 by
 * {@link GlobalExceptionHandler}.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
