package com.trackify.task.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request body for {@code POST /api/tasks/{taskId}/dependencies} (TASK-072).
 *
 * <p>The path's {@code taskId} is the successor (the task that depends on
 * something else); {@code predecessorTaskId} is the task that must finish
 * first.
 */
public record CreateDependencyRequest(
        @NotNull(message = "predecessorTaskId is required")
        UUID predecessorTaskId
) {
}
