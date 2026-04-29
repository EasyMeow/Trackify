package com.trackify.user.application;

import com.trackify.common.exception.NotFoundException;
import com.trackify.user.domain.User;
import com.trackify.user.dto.MeResponse;
import com.trackify.user.infrastructure.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Service
public class UpdateMeService {

    private static final long MAX_AVATAR_BYTES = 1_048_576L;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final UserRepository userRepository;

    public UpdateMeService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public MeResponse update(UUID userId, String displayName, MultipartFile avatar) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        if (displayName != null) {
            if (displayName.isBlank()) {
                throw new IllegalArgumentException("Display name must not be blank");
            }
            user.setDisplayName(displayName.trim());
        }

        if (avatar != null && !avatar.isEmpty()) {
            if (avatar.getSize() > MAX_AVATAR_BYTES) {
                throw new IllegalArgumentException("Avatar file must not exceed 1 MB");
            }
            String contentType = avatar.getContentType();
            if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
                throw new IllegalArgumentException("Avatar must be JPEG, PNG, or WebP");
            }
            try {
                user.setAvatar(avatar.getBytes());
                user.setAvatarContentType(contentType);
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to read avatar file");
            }
        }

        userRepository.save(user);
        return new MeResponse(user.getId(), user.getLogin(), user.getEmail(), user.getDisplayName());
    }
}
