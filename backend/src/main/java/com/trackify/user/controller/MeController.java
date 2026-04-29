package com.trackify.user.controller;

import com.trackify.auth.application.LocalUserPrincipal;
import com.trackify.user.application.ChangePasswordService;
import com.trackify.user.application.MeService;
import com.trackify.user.application.UpdateMeService;
import com.trackify.user.dto.ChangePasswordRequest;
import com.trackify.user.dto.MeResponse;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Exposes the current signed-in user's identity (TASK-028).
 *
 * <p>Placed in {@code user.controller} because the response is a user-domain
 * projection. The endpoint sits at {@code /api/me} per architecture.md §11
 * (auth/session group), but its data source is the {@code user} domain — the
 * {@code user} package is the natural owner.
 *
 * <p>Security: {@code /api/me} is covered by the catch-all
 * {@code .requestMatchers("/api/**").authenticated()} rule in
 * {@link com.trackify.config.SecurityConfig}. Spring Security returns 401
 * before the controller method is invoked for unauthenticated requests.
 */
@RestController
@RequestMapping("/api/me")
public class MeController {

    private final MeService meService;
    private final UpdateMeService updateMeService;
    private final ChangePasswordService changePasswordService;

    public MeController(
            MeService meService,
            UpdateMeService updateMeService,
            ChangePasswordService changePasswordService) {
        this.meService = meService;
        this.updateMeService = updateMeService;
        this.changePasswordService = changePasswordService;
    }

    @GetMapping
    public MeResponse me(@AuthenticationPrincipal LocalUserPrincipal principal) {
        return meService.getCurrentUser(principal.userId());
    }

    @PatchMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MeResponse updateMe(
            @AuthenticationPrincipal LocalUserPrincipal principal,
            @RequestParam(required = false) String displayName,
            @RequestParam(required = false) MultipartFile avatar) {
        return updateMeService.update(principal.userId(), displayName, avatar);
    }

    @PostMapping("/password")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal LocalUserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        changePasswordService.changePassword(
                principal.userId(), request.currentPassword(), request.newPassword());
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }
}
