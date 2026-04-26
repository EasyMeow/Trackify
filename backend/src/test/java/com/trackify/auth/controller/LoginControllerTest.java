package com.trackify.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trackify.auth.application.AuthSecurityBeans;
import com.trackify.auth.application.LocalUserDetailsService;
import com.trackify.config.JacksonConfig;
import com.trackify.config.SecurityConfig;
import com.trackify.config.WebConfig;
import com.trackify.user.domain.User;
import com.trackify.user.infrastructure.UserRepository;

import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;
import java.util.UUID;

/**
 * Proves the TASK-027 login flow: a valid credential pair authenticates the
 * caller, persists a {@link SecurityContext} into the HTTP session, and
 * returns a 200 with the principal's identity. An invalid password returns
 * 401 with the shared {@link com.trackify.common.response.ApiError} envelope
 * and never establishes a session.
 */
@WebMvcTest(controllers = LoginController.class)
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
class LoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void successfulLoginEstablishesAuthenticatedSessionAndReturnsPrincipalIdentity() throws Exception {
        UUID userId = UUID.randomUUID();
        String rawPassword = "hunter2hunter";
        User user = new User("alice", "alice@example.com",
                passwordEncoder.encode(rawPassword), "Alice");
        ReflectionTestUtils.setField(user, "id", userId);
        when(userRepository.findByLogin("alice")).thenReturn(Optional.of(user));

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"alice\",\"password\":\"" + rawPassword + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.login").value("alice"))
                .andReturn();

        HttpSession session = result.getRequest().getSession(false);
        assertThat(session).as("HTTP session created on successful login").isNotNull();

        SecurityContext stored = (SecurityContext) session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(stored).as("SecurityContext persisted to the session").isNotNull();
        assertThat(stored.getAuthentication().isAuthenticated()).isTrue();
        assertThat(stored.getAuthentication().getName()).isEqualTo("alice");
        assertThat(stored.getAuthentication().getPrincipal()).isInstanceOf(UserDetails.class);
    }

    @Test
    void invalidPasswordReturnsUnauthorizedAndDoesNotEstablishSession() throws Exception {
        User user = new User("alice", "alice@example.com",
                passwordEncoder.encode("hunter2hunter"), "Alice");
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        when(userRepository.findByLogin("alice")).thenReturn(Optional.of(user));

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"alice\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"))
                .andReturn();

        HttpSession session = result.getRequest().getSession(false);
        if (session != null) {
            Object stored = session.getAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            assertThat(stored).as("no SecurityContext persisted on failed login").isNull();
        }
    }

    @Test
    void unknownLoginReturnsUnauthorized() throws Exception {
        when(userRepository.findByLogin("ghost")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"ghost\",\"password\":\"hunter2hunter\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
    }
}
