package com.trackify.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /api/auth/login} (TASK-027).
 */
public record LoginRequest(@NotBlank String login, @NotBlank String password) {
}
