package com.trackify.user.controller;

import com.trackify.auth.application.LocalUserPrincipal;
import com.trackify.user.application.MeService;
import com.trackify.user.dto.MeResponse;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    public MeController(MeService meService) {
        this.meService = meService;
    }

    @GetMapping
    public MeResponse me(@AuthenticationPrincipal LocalUserPrincipal principal) {
        return meService.getCurrentUser(principal.userId());
    }
}
