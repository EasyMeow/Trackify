package com.trackify.auth.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed binding for {@code trackify.auth.*} (TASK-025). Consumed by security
 * setup ({@link AuthSecurityBeans}) and, later, by the registration and login
 * services (TASK-026, TASK-027).
 *
 * <p>{@link Password#minLength} drives request validation; {@link Password#encoderStrength}
 * drives the BCrypt cost factor. {@link Bootstrap#admin} carries optional
 * seed-user values that are blank in normal environments.
 */
@ConfigurationProperties("trackify.auth")
public record AuthProperties(Password password, Bootstrap bootstrap) {

    public record Password(int encoderStrength, int minLength) {}

    public record Bootstrap(Admin admin) {
        public record Admin(String login, String email, String password) {}
    }
}
