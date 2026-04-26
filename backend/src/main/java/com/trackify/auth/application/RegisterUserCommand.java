package com.trackify.auth.application;

/**
 * Input for {@link UserRegistrationService#register(RegisterUserCommand)}.
 * The raw password is held only in-process and is hashed before the {@code User}
 * row is written.
 */
public record RegisterUserCommand(String login, String email, String displayName, String rawPassword) {
}
