package com.trackify.project.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackify.auth.application.LocalUserPrincipal;
import com.trackify.common.exception.ForbiddenException;
import com.trackify.common.exception.NotFoundException;
import com.trackify.config.JacksonConfig;
import com.trackify.config.SecurityConfig;
import com.trackify.config.WebConfig;
import com.trackify.project.application.ProjectQueryService;
import com.trackify.project.application.ProjectUpdateService;
import com.trackify.project.dto.ProjectPatchRequest;
import com.trackify.project.dto.ProjectResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * TASK-094: verifies {@code PATCH /api/projects/{projectId}} behaviour.
 *
 * <ul>
 *   <li>Happy path — member patches only the fields provided and receives 200 with the updated DTO.
 *   <li>Missing project — service throws {@link NotFoundException}, response is 404.
 *   <li>Forbidden — non-member gets 403.
 *   <li>Invalid body — blank name fails validation and returns 400.
 *   <li>Unauthenticated — Spring Security rejects with 401.
 * </ul>
 */
@WebMvcTest(controllers = ProjectByIdController.class)
@Import({SecurityConfig.class, WebConfig.class, JacksonConfig.class})
@TestPropertySource(properties = {
        "trackify.cors.allowed-origin=http://localhost:5173",
        "trackify.cors.allowed-methods=GET,POST,PUT,PATCH,DELETE,OPTIONS",
        "trackify.cors.allowed-headers=*",
        "trackify.cors.allow-credentials=true"
})
class ProjectPatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProjectQueryService projectQueryService;

    @MockitoBean
    private ProjectUpdateService projectUpdateService;

    private Authentication authFor(UUID userId, String login) {
        LocalUserPrincipal principal = new LocalUserPrincipal(userId, login, "hashed-pw");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }

    @Test
    void memberPatchesNameAndDescriptionReturns200() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        Instant now = Instant.now();

        ProjectResponse updated = new ProjectResponse(
                projectId, workspaceId, "New Name", "new-name", "New desc", now, now);
        when(projectUpdateService.patch(eq(projectId), eq(userId), any(ProjectPatchRequest.class)))
                .thenReturn(updated);

        String body = objectMapper.writeValueAsString(Map.of("name", "New Name", "description", "New desc"));

        mockMvc.perform(patch("/api/projects/{projectId}", projectId)
                        .with(authentication(authFor(userId, "alice")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.description").value("New desc"));
    }

    @Test
    void partialPatchWithOnlyNameReturns200() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        Instant now = Instant.now();

        ProjectResponse updated = new ProjectResponse(
                projectId, workspaceId, "Renamed", "renamed", null, now, now);
        when(projectUpdateService.patch(eq(projectId), eq(userId), any(ProjectPatchRequest.class)))
                .thenReturn(updated);

        String body = objectMapper.writeValueAsString(Map.of("name", "Renamed"));

        mockMvc.perform(patch("/api/projects/{projectId}", projectId)
                        .with(authentication(authFor(userId, "alice")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"));
    }

    @Test
    void missingProjectReturns404() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();

        when(projectUpdateService.patch(eq(projectId), eq(userId), any(ProjectPatchRequest.class)))
                .thenThrow(new NotFoundException("Project not found"));

        String body = objectMapper.writeValueAsString(Map.of("name", "Any"));

        mockMvc.perform(patch("/api/projects/{projectId}", projectId)
                        .with(authentication(authFor(userId, "alice")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void nonMemberReceivesForbidden() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();

        when(projectUpdateService.patch(eq(projectId), eq(userId), any(ProjectPatchRequest.class)))
                .thenThrow(new ForbiddenException("Project not accessible"));

        String body = objectMapper.writeValueAsString(Map.of("name", "Any"));

        mockMvc.perform(patch("/api/projects/{projectId}", projectId)
                        .with(authentication(authFor(userId, "bob")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void blankNameFailsValidationWith400() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();

        String body = objectMapper.writeValueAsString(Map.of("name", ""));

        mockMvc.perform(patch("/api/projects/{projectId}", projectId)
                        .with(authentication(authFor(userId, "alice")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        verifyNoInteractions(projectUpdateService);
    }

    @Test
    void unauthenticatedRequestIsRejectedWith401() throws Exception {
        UUID projectId = UUID.randomUUID();
        String body = objectMapper.writeValueAsString(Map.of("name", "X"));

        mockMvc.perform(patch("/api/projects/{projectId}", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(projectUpdateService);
    }
}
