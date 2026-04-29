package com.trackify.board.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A single task card as returned by the board read endpoint (TASK-047).
 * Contains only the fields the Kanban UI needs; full task detail lives behind
 * {@code GET /api/tasks/{taskId}}.
 */
public record BoardTaskCardResponse(
        UUID id,
        String title,
        String status,
        String priority,
        double sortOrder,
        LocalDate startDate,
        LocalDate dueDate,
        Instant createdAt
) {
}
