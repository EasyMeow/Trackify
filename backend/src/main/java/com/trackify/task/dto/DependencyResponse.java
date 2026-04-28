package com.trackify.task.dto;

import java.time.Instant;
import java.util.UUID;

public record DependencyResponse(
        UUID id,
        UUID predecessorTaskId,
        UUID successorTaskId,
        Instant createdAt
) {
}
