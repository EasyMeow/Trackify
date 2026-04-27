package com.trackify.task.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request body for {@code PATCH /api/tasks/{taskId}/move} (TASK-059).
 *
 * <p>Both fields are required. The client is responsible for computing
 * {@code sortOrder} as a fractional value between the neighboring tasks
 * (e.g. {@code (prev + next) / 2}); this matches the {@code DOUBLE PRECISION}
 * column type chosen specifically to allow inserts without a full re-numbering
 * pass (V7 migration comment).
 *
 * <ul>
 *   <li>{@code columnId} — UUID of the destination {@code board_columns} row.
 *       Must belong to the same project as the task being moved (validated
 *       in the service layer).
 *   <li>{@code sortOrder} — new sort position within the destination column.
 *       {@link Double} (boxed) so {@code @NotNull} can reject a missing field;
 *       a primitive {@code double} would silently default to 0.0.
 * </ul>
 */
public record MoveTaskRequest(
        @NotNull(message = "columnId is required")
        UUID columnId,

        @NotNull(message = "sortOrder is required")
        Double sortOrder
) {
}
