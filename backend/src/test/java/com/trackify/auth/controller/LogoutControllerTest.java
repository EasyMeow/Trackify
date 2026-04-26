package com.trackify.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trackify.auth.application.LocalUserPrincipal;
import com.trackify.config.JacksonConfig;
import com.trackify.config.SecurityConfig;
import com.trackify.config.WebConfig;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

/**
 * TASK-029: verifies {@code POST /api/logout} invalidates the current HTTP
 * session and that unauthenticated callers are rejected with 401 before the
 * handler runs.
 */
@WebMvcTest(controllers = LogoutController.class)
@Import({SecurityConfig.class, WebConfig.class, JacksonConfig.class})
@TestPropertySource(properties = {
        "trackify.cors.allowed-origin=http://localhost:5173",
        "trackify.cors.allowed-methods=GET,POST,PUT,PATCH,DELETE,OPTIONS",
        "trackify.cors.allowed-headers=*",
        "trackify.cors.allow-credentials=true"
})
class LogoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void authenticatedLogoutInvalidatesSessionAndClearsSecurityContext() throws Exception {
        MockHttpSession session = new MockHttpSession();
        LocalUserPrincipal principal = new LocalUserPrincipal(
                UUID.randomUUID(), "alice", "hashed-pw");
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of());
        // Seed the session with a SecurityContext like a real signed-in caller would.
        org.springframework.security.core.context.SecurityContext ctx =
                org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, ctx);

        mockMvc.perform(post("/api/logout").session(session).with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(session.isInvalid())
                .as("HTTP session is invalidated after logout")
                .isTrue();
    }

    @Test
    void unauthenticatedLogoutReturns401() throws Exception {
        mockMvc.perform(post("/api/logout"))
                .andExpect(status().isUnauthorized());
    }
}
