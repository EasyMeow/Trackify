package com.trackify.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /api/auth/register} (TASK-084).
 * Structural validation (non-blank, well-formed email) happens here.
 * Password minimum-length validation is enforced by
 * {@link com.trackify.auth.application.UserRegistrationService}, which
 * consults {@link com.trackify.auth.application.AuthProperties} at runtime.
 */
public record RegisterRequest(
        @NotBlank String login,
        @NotBlank @Email String email,
        @NotBlank String displayName,
        @NotBlank String password
) {
}
