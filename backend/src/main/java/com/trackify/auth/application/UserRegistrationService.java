package com.trackify.auth.application;

import com.trackify.auth.domain.DuplicateLoginException;
import com.trackify.user.domain.User;
import com.trackify.user.infrastructure.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates local-credential users (TASK-026). Validates the password against the
 * configured minimum length, rejects duplicate logins, hashes the password with
 * the auth-module {@link PasswordEncoder}, and persists exactly one {@link User}
 * row carrying only the hash.
 */
@Service
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties authProperties;

    public UserRegistrationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthProperties authProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authProperties = authProperties;
    }

    @Transactional
    public User register(RegisterUserCommand command) {
        int minLength = authProperties.password().minLength();
        if (command.rawPassword() == null || command.rawPassword().length() < minLength) {
            throw new IllegalArgumentException("password must be at least " + minLength + " characters");
        }
        if (userRepository.existsByLogin(command.login())) {
            throw new DuplicateLoginException(command.login());
        }
        String passwordHash = passwordEncoder.encode(command.rawPassword());
        User user = new User(command.login(), command.email(), passwordHash, command.displayName());
        return userRepository.save(user);
    }
}
