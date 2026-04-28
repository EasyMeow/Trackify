package com.trackify.board.dto;

import java.time.Instant;
import java.util.UUID;

public record ColumnResponse(
        UUID id,
        UUID projectId,
        String name,
        int position,
        Instant createdAt,
        Instant updatedAt
) {
}
