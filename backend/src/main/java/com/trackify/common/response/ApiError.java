package com.trackify.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Uniform JSON envelope returned by the global exception handler.
 * The same shape is used for validation errors, not-found errors, and
 * uncaught server errors (TASK-011) and is extended by TASK-077.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        int status,
        String error,
        String message,
        String path,
        Instant timestamp,
        List<FieldViolation> details
) {

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(status, error, message, path, Instant.now(), null);
    }

    public static ApiError of(int status, String error, String message, String path, List<FieldViolation> details) {
        return new ApiError(status, error, message, path, Instant.now(), details);
    }

    public record FieldViolation(String field, String message) {}
}
