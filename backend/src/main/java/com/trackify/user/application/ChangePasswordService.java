package com.trackify.user.application;

import com.trackify.auth.application.AuthProperties;
import com.trackify.common.exception.NotFoundException;
import com.trackify.user.domain.User;
import com.trackify.user.infrastructure.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ChangePasswordService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties authProperties;

    public ChangePasswordService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthProperties authProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authProperties = authProperties;
    }

    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        int minLength = authProperties.password().minLength();
        if (newPassword == null || newPassword.length() < minLength) {
            throw new IllegalArgumentException("New password must be at least " + minLength + " characters");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
