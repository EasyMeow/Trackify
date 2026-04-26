package com.trackify.project.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Response payload for project listing/detail endpoints (TASK-039 onwards).
 * Entities never cross this boundary (architecture.md §7).
 */
public record ProjectResponse(
        UUID id,
        UUID workspaceId,
        String name,
        String slug,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}
