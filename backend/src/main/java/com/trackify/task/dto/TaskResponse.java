package com.trackify.task.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Full task shape returned by task write and detail endpoints (TASK-050, TASK-052).
 * Reused across task-level responses so callers always see the same field set.
 */
public record TaskResponse(
        UUID id,
        UUID projectId,
        UUID columnId,
        UUID createdBy,
        String title,
        String description,
        String status,
        String priority,
        double sortOrder,
        LocalDate startDate,
        LocalDate dueDate,
        BigDecimal estimatedHours,
        Instant createdAt,
        Instant updatedAt
) {
}
