package com.trackify.task.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request body for {@code PATCH /api/tasks/{taskId}} (TASK-055).
 *
 * <p>All fields are optional/nullable. A null field means "do not change".
 * Only content fields are mutable here; column and position are handled by
 * the separate move endpoint (TASK-058/059).
 *
 * <ul>
 *   <li>{@code title} — if non-null, must be 1–255 characters (blank caught by @Size min=1).
 *   <li>{@code description} — if non-null, replaces the existing description.
 *   <li>{@code priority} — if non-null, must be LOW|MEDIUM|HIGH|URGENT (validated in service).
 *   <li>{@code startDate} — if non-null, replaces start date.
 *   <li>{@code dueDate} — if non-null, replaces due date.
 * </ul>
 */
public record UpdateTaskRequest(
        @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
        String title,

        String description,

        String priority,

        LocalDate startDate,

        LocalDate dueDate
) {
}
