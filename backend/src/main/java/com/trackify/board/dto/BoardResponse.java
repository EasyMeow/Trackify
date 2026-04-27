package com.trackify.board.dto;

import java.util.List;
import java.util.UUID;

/**
 * Top-level board response for {@code GET /api/projects/{projectId}/board} (TASK-047).
 * Columns are ordered by their {@code position} field; tasks within each column
 * are ordered by {@code sortOrder}.
 */
public record BoardResponse(
        UUID projectId,
        List<BoardColumnResponse> columns
) {
}
