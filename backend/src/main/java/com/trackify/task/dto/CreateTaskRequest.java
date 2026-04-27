package com.trackify.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request body for {@code POST /api/projects/{projectId}/tasks} (TASK-050).
 *
 * <ul>
 *   <li>{@code title} is required and capped at 255 characters.
 *   <li>{@code description}, {@code startDate}, {@code dueDate} are optional.
 *   <li>{@code priority} is optional — null defaults to "MEDIUM" in the service;
 *       non-null values are validated in the service against LOW|MEDIUM|HIGH|URGENT.
 *   <li>{@code status}, {@code columnId}, {@code sortOrder}, and {@code estimatedHours}
 *       are intentionally absent: the service sets them via defaults/resolution.
 * </ul>
 */
public record CreateTaskRequest(
        @NotBlank(message = "Title must not be blank")
        @Size(max = 255, message = "Title must be at most 255 characters")
        String title,

        String description,

        String priority,

        LocalDate startDate,

        LocalDate dueDate
) {
}
