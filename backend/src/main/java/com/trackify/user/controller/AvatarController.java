package com.trackify.user.controller;

import com.trackify.common.exception.NotFoundException;
import com.trackify.user.domain.User;
import com.trackify.user.infrastructure.UserRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class AvatarController {

    private final UserRepository userRepository;

    public AvatarController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/{id}/avatar")
    public ResponseEntity<byte[]> getAvatar(@PathVariable UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));

        byte[] bytes = user.getAvatar();
        if (bytes == null || bytes.length == 0) {
            throw new NotFoundException("No avatar for user: " + id);
        }

        String contentType = user.getAvatarContentType() != null
                ? user.getAvatarContentType()
                : "application/octet-stream";

        return ResponseEntity.ok()
                .header("Content-Type", contentType)
                .body(bytes);
    }
}
