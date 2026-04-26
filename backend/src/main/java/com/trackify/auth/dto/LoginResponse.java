package com.trackify.auth.dto;

import java.util.UUID;

/**
 * Minimal response payload for {@code POST /api/auth/login} (TASK-027). The
 * SPA hydrates the full profile from {@code GET /api/me} after login; this
 * payload only carries the bare identity needed to confirm the session was
 * established.
 */
public record LoginResponse(UUID userId, String login) {
}
