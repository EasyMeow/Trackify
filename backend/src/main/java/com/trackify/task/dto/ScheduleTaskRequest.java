package com.trackify.task.dto;

import java.time.LocalDate;

/**
 * Request body for {@code PATCH /api/tasks/{taskId}/schedule} (TASK-070).
 *
 * <p>Focused scheduling endpoint used by the Gantt timeline view when a bar is
 * dragged or resized. Both fields are optional/nullable: a null field means
 * "do not change" — same convention as {@link UpdateTaskRequest}. When the
 * Gantt UI moves a bar it sends both dates together; sending neither is a
 * harmless no-op that just re-reads the task.
 *
 * <p>Cross-field validation (e.g. {@code dueDate >= startDate}) is intentionally
 * not enforced here — it mirrors {@link UpdateTaskRequest}'s behaviour and keeps
 * the two endpoints consistent. The Gantt frontend (TASK-071) is responsible for
 * not submitting inverted ranges; the bar widget already nudges {@code end}
 * forward by one day on its own when needed.
 */
public record ScheduleTaskRequest(
        LocalDate startDate,
        LocalDate dueDate
) {
}
