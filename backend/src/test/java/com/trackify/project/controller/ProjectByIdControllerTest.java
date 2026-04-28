package com.trackify.project.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trackify.auth.application.LocalUserPrincipal;
import com.trackify.common.exception.ForbiddenException;
import com.trackify.common.exception.NotFoundException;
import com.trackify.config.JacksonConfig;
import com.trackify.config.SecurityConfig;
import com.trackify.config.WebConfig;
import com.trackify.project.application.ProjectQueryService;
import com.trackify.project.application.ProjectUpdateService;
import com.trackify.project.dto.ProjectResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * TASK-040: verifies {@code GET /api/projects/{projectId}} behaviour.
 *
 * <ul>
 *   <li>Found path — workspace member receives the project DTO with HTTP 200.
 *   <li>Missing path — project does not exist, service throws {@link NotFoundException},
 *       response is HTTP 404 with the standard error envelope.
 *   <li>Forbidden path — project exists but caller is not a member of its workspace,
 *       service throws {@link ForbiddenException}, response is HTTP 403.
 *   <li>Unauthenticated request — Spring Security rejects with 401 before the
 *       controller method is invoked.
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
class ProjectByIdControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectQueryService projectQueryService;

    @MockitoBean
    private ProjectUpdateService projectUpdateService;

    @Test
    void memberReceivesProjectForFoundProject() throws Exception {
        UUID userId = UUID.randomUUID();
        LocalUserPrincipal principal = new LocalUserPrincipal(userId, "alice", "hashed-pw");
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());

        UUID workspaceId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-04-25T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-04-26T11:00:00Z");

        ProjectResponse serviceResult = new ProjectResponse(
                projectId, workspaceId, "Alpha", "alpha", "first project", createdAt, updatedAt);
        when(projectQueryService.getById(projectId, userId)).thenReturn(serviceResult);

        mockMvc.perform(get("/api/projects/{projectId}", projectId).with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projectId.toString()))
                .andExpect(jsonPath("$.workspaceId").value(workspaceId.toString()))
                .andExpect(jsonPath("$.name").value("Alpha"))
                .andExpect(jsonPath("$.slug").value("alpha"))
                .andExpect(jsonPath("$.description").value("first project"));
    }

    @Test
    void missingProjectReturns404() throws Exception {
        UUID userId = UUID.randomUUID();
        LocalUserPrincipal principal = new LocalUserPrincipal(userId, "alice", "hashed-pw");
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());

        UUID projectId = UUID.randomUUID();
        when(projectQueryService.getById(projectId, userId))
                .thenThrow(new NotFoundException("Project not found"));

        mockMvc.perform(get("/api/projects/{projectId}", projectId).with(authentication(auth)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Project not found"));
    }

    @Test
    void nonMemberReceivesForbidden() throws Exception {
        UUID userId = UUID.randomUUID();
        LocalUserPrincipal principal = new LocalUserPrincipal(userId, "bob", "hashed-pw");
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());

        UUID projectId = UUID.randomUUID();
        when(projectQueryService.getById(projectId, userId))
                .thenThrow(new ForbiddenException("Project not accessible"));

        mockMvc.perform(get("/api/projects/{projectId}", projectId).with(authentication(auth)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Project not accessible"));
    }

    @Test
    void unauthenticatedRequestIsRejectedWith401BeforeControllerRuns() throws Exception {
        UUID projectId = UUID.randomUUID();
        mockMvc.perform(get("/api/projects/{projectId}", projectId))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(projectQueryService);
    }
}
