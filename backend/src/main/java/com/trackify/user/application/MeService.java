package com.trackify.user.application;

import com.trackify.common.exception.NotFoundException;
import com.trackify.user.domain.User;
import com.trackify.user.dto.MeResponse;
import com.trackify.user.infrastructure.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Application service for the {@code GET /api/me} use case (TASK-028).
 * Loads the full {@link User} row by id — carrying the mutable fields
 * ({@code email}, {@code displayName}) that are not stored on the principal —
 * and maps it to a {@link MeResponse} DTO. The principal is intentionally kept
 * lean (only {@code userId} and {@code username}) so that profile updates do
 * not require a session re-issue.
 */
@Service
public class MeService {

    private final UserRepository userRepository;

    public MeService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public MeResponse getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        return new MeResponse(user.getId(), user.getLogin(), user.getEmail(), user.getDisplayName());
    }
}
