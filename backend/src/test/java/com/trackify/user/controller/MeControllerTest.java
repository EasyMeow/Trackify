package com.trackify.user.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trackify.auth.application.LocalUserPrincipal;
import com.trackify.config.JacksonConfig;
import com.trackify.config.SecurityConfig;
import com.trackify.config.WebConfig;
import com.trackify.user.application.MeService;
import com.trackify.user.dto.MeResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

/**
 * TASK-028: verifies {@code GET /api/me} behaviour for authenticated and
 * unauthenticated callers.
 *
 * <ul>
 *   <li>Authenticated — installs a {@link LocalUserPrincipal} via the Spring
 *       Security {@code authentication()} request post-processor. Asserts HTTP 200
 *       and that the JSON body exposes {@code id}, {@code login}, {@code email},
 *       {@code displayName}, and that {@code passwordHash} is absent.
 *   <li>Unauthenticated — no security context. Spring Security's
 *       {@link org.springframework.security.web.authentication.HttpStatusEntryPoint}
 *       (configured with {@link org.springframework.http.HttpStatus#UNAUTHORIZED})
 *       intercepts the request before it reaches the controller and returns 401.
 * </ul>
 */
@WebMvcTest(controllers = MeController.class)
@Import({SecurityConfig.class, WebConfig.class, JacksonConfig.class})
@TestPropertySource(properties = {
        "trackify.cors.allowed-origin=http://localhost:5173",
        "trackify.cors.allowed-methods=GET,POST,PUT,PATCH,DELETE,OPTIONS",
        "trackify.cors.allowed-headers=*",
        "trackify.cors.allow-credentials=true"
})
class MeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MeService meService;

    @Test
    void authenticatedUserReceivesIdentityPayloadWithoutPasswordHash() throws Exception {
        UUID userId = UUID.randomUUID();
        LocalUserPrincipal principal = new LocalUserPrincipal(userId, "alice", "hashed-pw");
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of());

        MeResponse response = new MeResponse(userId, "alice", "alice@example.com", "Alice");
        when(meService.getCurrentUser(userId)).thenReturn(response);

        mockMvc.perform(get("/api/me").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.login").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.displayName").value("Alice"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void unauthenticatedRequestIsRejectedWith401BeforeControllerRuns() throws Exception {
        // Spring Security's HttpStatusEntryPoint(UNAUTHORIZED) fires before the
        // dispatcher servlet — MeService should never be called.
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());
    }
}
