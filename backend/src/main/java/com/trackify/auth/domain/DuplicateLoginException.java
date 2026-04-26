package com.trackify.auth.domain;

/**
 * Thrown when {@link com.trackify.auth.application.UserRegistrationService}
 * is asked to create a user with a login that is already taken. The service
 * checks before insert so the caller gets a typed signal instead of a raw
 * {@link org.springframework.dao.DataIntegrityViolationException} from the
 * unique index on {@code users.login}.
 */
public class DuplicateLoginException extends RuntimeException {

    public DuplicateLoginException(String login) {
        super("login already in use: " + login);
    }
}
