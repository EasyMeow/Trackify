package com.trackify.user.dto;

import java.util.UUID;

/**
 * Response payload for {@code GET /api/me} (TASK-028). Exposes
 * identity-relevant fields only. {@code passwordHash} is never included.
 */
public record MeResponse(
        UUID id,
        String login,
        String email,
        String displayName
) {
}
