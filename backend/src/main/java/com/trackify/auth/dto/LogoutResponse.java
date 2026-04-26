package com.trackify.auth.dto;

/**
 * Minimal response payload for {@code POST /api/logout} (TASK-029).
 */
public record LogoutResponse(boolean success) {
}
