package com.trackify.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trackify.auth.application.AuthSecurityBeans;
import com.trackify.auth.application.LocalUserDetailsService;
import com.trackify.auth.application.RegisterUserCommand;
import com.trackify.auth.application.UserRegistrationService;
import com.trackify.auth.domain.DuplicateLoginException;
import com.trackify.config.JacksonConfig;
import com.trackify.config.SecurityConfig;
import com.trackify.config.WebConfig;
import com.trackify.user.domain.User;
import com.trackify.user.infrastructure.UserRepository;
import com.trackify.workspace.application.WorkspaceBootstrapService;

import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

/**
 * Proves the TASK-084 registration flow:
 * - valid payload → 201 + MeResponse shape + authenticated session established.
 * - duplicate login → 409 + ApiError.
 * - invalid body (blank login, bad email, short password) → 400 + ApiError.
 */
@WebMvcTest(controllers = RegisterController.class)
@Import({SecurityConfig.class, WebConfig.class, JacksonConfig.class,
        AuthSecurityBeans.class, LocalUserDetailsService.class})
@TestPropertySource(properties = {
        "trackify.cors.allowed-origin=http://localhost:5173",
        "trackify.cors.allowed-methods=GET,POST,PUT,PATCH,DELETE,OPTIONS",
        "trackify.cors.allowed-headers=*",
        "trackify.cors.allow-credentials=true",
        "trackify.auth.password.encoder-strength=4",
        "trackify.auth.password.min-length=8",
        "trackify.auth.bootstrap.admin.login=",
        "trackify.auth.bootstrap.admin.email=",
        "trackify.auth.bootstrap.admin.password="
})
class RegisterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRegistrationService userRegistrationService;

    @MockitoBean
    private WorkspaceBootstrapService workspaceBootstrapService;

    // Required by LocalUserDetailsService (imported transitively via AuthSecurityBeans chain)
    @MockitoBean
    private UserRepository userRepository;

    // --- helper ---

    private static User fakeUser(UUID id, String login, String email, String displayName) {
        User u = new User(login, email, "hash", displayName);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    // --- test 1: happy path ---

    @Test
    void validRegistrationReturns201WithMePayloadAndEstablishesSession() throws Exception {
        UUID userId = UUID.randomUUID();
        User saved = fakeUser(userId, "bob", "bob@example.com", "Bob");

        when(userRegistrationService.register(any(RegisterUserCommand.class))).thenReturn(saved);
        doNothing().when(workspaceBootstrapService).ensurePersonalWorkspace(eq(userId), eq("bob"));

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "login": "bob",
                                  "email": "bob@example.com",
                                  "displayName": "Bob",
                                  "password": "hunter2x"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.login").value("bob"))
                .andExpect(jsonPath("$.email").value("bob@example.com"))
                .andExpect(jsonPath("$.displayName").value("Bob"))
                .andReturn();

        HttpSession session = result.getRequest().getSession(false);
        assertThat(session).as("HTTP session created on successful registration").isNotNull();

        SecurityContext stored = (SecurityContext) session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(stored).as("SecurityContext persisted to session").isNotNull();
        assertThat(stored.getAuthentication().isAuthenticated()).isTrue();
        assertThat(stored.getAuthentication().getName()).isEqualTo("bob");
    }

    // --- test 2: duplicate login → 409 ---

    @Test
    void duplicateLoginReturns409WithApiErrorShape() throws Exception {
        when(userRegistrationService.register(any(RegisterUserCommand.class)))
                .thenThrow(new DuplicateLoginException("bob"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "login": "bob",
                                  "email": "bob@example.com",
                                  "displayName": "Bob",
                                  "password": "hunter2x"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("DUPLICATE_LOGIN"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    // --- test 3a: blank login → 400 ---

    @Test
    void blankLoginReturns400WithApiErrorShape() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "login": "",
                                  "email": "bob@example.com",
                                  "displayName": "Bob",
                                  "password": "hunter2x"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    // --- test 3b: malformed email → 400 ---

    @Test
    void malformedEmailReturns400WithApiErrorShape() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "login": "bob",
                                  "email": "not-an-email",
                                  "displayName": "Bob",
                                  "password": "hunter2x"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    // --- test 3c: short password → 400 (service throws IllegalArgumentException) ---

    @Test
    void shortPasswordReturns400WithApiErrorShape() throws Exception {
        when(userRegistrationService.register(any(RegisterUserCommand.class)))
                .thenThrow(new IllegalArgumentException("password must be at least 8 characters"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "login": "bob",
                                  "email": "bob@example.com",
                                  "displayName": "Bob",
                                  "password": "short"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }
}
