package com.trackify.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trackify.auth.application.LocalUserPrincipal;
import com.trackify.config.JacksonConfig;
import com.trackify.config.SecurityConfig;
import com.trackify.config.WebConfig;
import com.trackify.user.application.ChangePasswordService;
import com.trackify.user.application.MeService;
import com.trackify.user.application.UpdateMeService;
import com.trackify.user.dto.MeResponse;

import org.springframework.http.MediaType;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

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

    @MockitoBean
    private UpdateMeService updateMeService;

    @MockitoBean
    private ChangePasswordService changePasswordService;

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

    @Test
    void patchDisplayNameOnly_returns200WithUpdatedName() throws Exception {
        UUID userId = UUID.randomUUID();
        LocalUserPrincipal principal = new LocalUserPrincipal(userId, "alice", "hashed-pw");
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());

        MeResponse updated = new MeResponse(userId, "alice", "alice@example.com", "NewName");
        when(updateMeService.update(eq(userId), eq("NewName"), isNull())).thenReturn(updated);

        mockMvc.perform(MockMvcRequestBuilders.multipart(HttpMethod.PATCH, "/api/me")
                        .param("displayName", "NewName")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("NewName"));
    }

    @Test
    void patchAvatar_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        LocalUserPrincipal principal = new LocalUserPrincipal(userId, "alice", "hashed-pw");
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());

        MockMultipartFile avatarFile = new MockMultipartFile(
                "avatar", "avatar.jpg", "image/jpeg", new byte[100]);

        MeResponse updated = new MeResponse(userId, "alice", "alice@example.com", "Alice");
        when(updateMeService.update(eq(userId), isNull(), any())).thenReturn(updated);

        mockMvc.perform(MockMvcRequestBuilders.multipart(HttpMethod.PATCH, "/api/me")
                        .file(avatarFile)
                        .with(authentication(auth)))
                .andExpect(status().isOk());
    }

    @Test
    void patchDisplayName_blankValue_serviceThrowsIllegalArgument_returns400() throws Exception {
        UUID userId = UUID.randomUUID();
        LocalUserPrincipal principal = new LocalUserPrincipal(userId, "alice", "hashed-pw");
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());

        when(updateMeService.update(eq(userId), eq("  "), isNull()))
                .thenThrow(new IllegalArgumentException("Display name must not be blank"));

        mockMvc.perform(MockMvcRequestBuilders.multipart(HttpMethod.PATCH, "/api/me")
                        .param("displayName", "  ")
                        .with(authentication(auth)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void patchOversizedAvatar_serviceThrowsIllegalArgument_returns400() throws Exception {
        UUID userId = UUID.randomUUID();
        LocalUserPrincipal principal = new LocalUserPrincipal(userId, "alice", "hashed-pw");
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());

        MockMultipartFile bigFile = new MockMultipartFile(
                "avatar", "big.jpg", "image/jpeg", new byte[1_048_577]);
        when(updateMeService.update(eq(userId), isNull(), any()))
                .thenThrow(new IllegalArgumentException("Avatar file must not exceed 1 MB"));

        mockMvc.perform(MockMvcRequestBuilders.multipart(HttpMethod.PATCH, "/api/me")
                        .file(bigFile)
                        .with(authentication(auth)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patchMe_unauthenticated_returns401() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.multipart(HttpMethod.PATCH, "/api/me")
                        .param("displayName", "NewName"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postPassword_success_returns200WithMessage() throws Exception {
        UUID userId = UUID.randomUUID();
        LocalUserPrincipal principal = new LocalUserPrincipal(userId, "alice", "hashed-pw");
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());

        doNothing().when(changePasswordService).changePassword(userId, "old-pass", "new-pass-ok");

        mockMvc.perform(post("/api/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"old-pass\",\"newPassword\":\"new-pass-ok\"}")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password updated successfully"));
    }

    @Test
    void postPassword_wrongCurrentPassword_returns400() throws Exception {
        UUID userId = UUID.randomUUID();
        LocalUserPrincipal principal = new LocalUserPrincipal(userId, "alice", "hashed-pw");
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());

        doThrow(new IllegalArgumentException("Current password is incorrect"))
                .when(changePasswordService).changePassword(userId, "wrong", "new-pass-ok");

        mockMvc.perform(post("/api/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"wrong\",\"newPassword\":\"new-pass-ok\"}")
                        .with(authentication(auth)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void postPassword_newPasswordTooShort_returns400() throws Exception {
        UUID userId = UUID.randomUUID();
        LocalUserPrincipal principal = new LocalUserPrincipal(userId, "alice", "hashed-pw");
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());

        doThrow(new IllegalArgumentException("New password must be at least 8 characters"))
                .when(changePasswordService).changePassword(userId, "old-pass", "short");

        mockMvc.perform(post("/api/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"old-pass\",\"newPassword\":\"short\"}")
                        .with(authentication(auth)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void postPassword_blankFields_returns400() throws Exception {
        UUID userId = UUID.randomUUID();
        LocalUserPrincipal principal = new LocalUserPrincipal(userId, "alice", "hashed-pw");
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());

        mockMvc.perform(post("/api/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"\",\"newPassword\":\"\"}")
                        .with(authentication(auth)))
                .andExpect(status().isBadRequest());
    }
}
