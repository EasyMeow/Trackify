package com.trackify.auth.controller;

import com.trackify.auth.dto.LogoutResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Logout endpoint (TASK-029). Invalidates the current HTTP session — which
 * also evicts the row in {@code SPRING_SESSION} maintained by Spring Session
 * JDBC — and clears the {@code SecurityContextHolder} for the in-flight
 * thread.
 *
 * <p>The endpoint sits at {@code /api/logout} per architecture.md §11, so it
 * is matched by the catch-all {@code .requestMatchers("/api/**").authenticated()}
 * rule in {@link com.trackify.config.SecurityConfig}. Unauthenticated callers
 * get a 401 from the {@code HttpStatusEntryPoint} before this method runs;
 * authenticated callers always tear down their session.
 */
@RestController
public class LogoutController {

    @PostMapping("/api/logout")
    public LogoutResponse logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return new LogoutResponse(true);
    }
}
