package com.trackify.project.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackify.auth.application.LocalUserPrincipal;
import com.trackify.common.exception.ForbiddenException;
import com.trackify.config.JacksonConfig;
import com.trackify.config.SecurityConfig;
import com.trackify.config.WebConfig;
import com.trackify.project.application.ProjectCreateService;
import com.trackify.project.application.ProjectQueryService;
import com.trackify.project.dto.ProjectCreateRequest;
import com.trackify.project.dto.ProjectResponse;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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
import java.util.UUID;

/**
 * TASK-039 / TASK-041: verifies collection endpoints on
 * {@code /api/workspaces/{workspaceId}/projects}.
 *
 * <ul>
 *   <li>GET happy path, forbidden, empty list, unauthenticated (TASK-039).
 *   <li>POST happy path (201 + Location), forbidden, validation 400,
 *       unauthenticated 401 (TASK-041).
 * </ul>
 */
@WebMvcTest(controllers = ProjectController.class)
@Import({SecurityConfig.class, WebConfig.class, JacksonConfig.class})
@TestPropertySource(properties = {
        "trackify.cors.allowed-origin=http://localhost:5173",
        "trackify.cors.allowed-methods=GET,POST,PUT,PATCH,DELETE,OPTIONS",
        "trackify.cors.allowed-headers=*",
        "trackify.cors.allow-credentials=true"
})
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProjectQueryService projectQueryService;

    @MockitoBean
    private ProjectCreateService projectCreateService;

    // -------------------------------------------------------------------------
    // GET /api/workspaces/{workspaceId}/projects  (TASK-039)
    // -------------------------------------------------------------------------

    @Test
    void memberReceivesProjectsForTheirWorkspace() throws Exception {
        UUID userId = UUID.randomUUID();
        LocalUserPrincipal principal = new LocalUserPrincipal(userId, "alice", "hashed-pw");
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());

        UUID workspaceId = UUID.randomUUID();
        UUID projectId1 = UUID.randomUUID();
        UUID projectId2 = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-04-25T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-04-26T11:00:00Z");

        List<ProjectResponse> serviceResult = List.of(
                new ProjectResponse(projectId1, workspaceId, "Alpha", "alpha", "first project", createdAt, updatedAt),
                new ProjectResponse(projectId2, workspaceId, "Beta", "beta", null, createdAt, updatedAt)
        );
        when(projectQueryService.listForWorkspace(workspaceId, userId)).thenReturn(serviceResult);

        mockMvc.perform(get("/api/workspaces/{workspaceId}/projects", workspaceId).with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(projectId1.toString()))
                .andExpect(jsonPath("$[0].workspaceId").value(workspaceId.toString()))
                .andExpect(jsonPath("$[0].name").value("Alpha"))
                .andExpect(jsonPath("$[0].slug").value("alpha"))
                .andExpect(jsonPath("$[0].description").value("first project"))
                .andExpect(jsonPath("$[1].id").value(projectId2.toString()))
                .andExpect(jsonPath("$[1].name").value("Beta"));
    }

    @Test
    void nonMemberReceivesForbidden() throws Exception {
        UUID userId = UUID.randomUUID();
        LocalUserPrincipal principal = new LocalUserPrincipal(userId, "bob", "hashed-pw");
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());

        UUID otherWorkspaceId = UUID.randomUUID();
        when(projectQueryService.listForWorkspace(otherWorkspaceId, userId))
                .thenThrow(new ForbiddenException("Workspace not accessible"));

        mockMvc.perform(get("/api/workspaces/{workspaceId}/projects", otherWorkspaceId).with(authentication(auth)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Workspace not accessible"));
    }

    @Test
    void memberWithNoProjectsReceivesEmptyList() throws Exception {
        UUID userId = UUID.randomUUID();
        LocalUserPrincipal principal = new LocalUserPrincipal(userId, "alice", "hashed-pw");
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());

        UUID workspaceId = UUID.randomUUID();
        when(projectQueryService.listForWorkspace(workspaceId, userId)).thenReturn(List.of());

        mockMvc.perform(get("/api/workspaces/{workspaceId}/projects", workspaceId).with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void unauthenticatedGetRequestIsRejectedWith401() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        mockMvc.perform(get("/api/workspaces/{workspaceId}/projects", workspaceId))
                .andExpect(status().isUnauthorized());
        Mockito.verifyNoInteractions(projectQueryService);
    }

    // -------------------------------------------------------------------------
    // POST /api/workspaces/{workspaceId}/projects  (TASK-041)
    // -------------------------------------------------------------------------

    @Test
    void memberCanCreateProject() throws Exception {
        UUID userId = UUID.randomUUID();
        LocalUserPrincipal principal = new LocalUserPrincipal(userId, "alice", "hashed-pw");
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());

        UUID workspaceId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Instant now = Instant.parse("2026-04-27T08:00:00Z");

        ProjectCreateRequest requestBody = new ProjectCreateRequest("My Project", null, "a description");
        ProjectResponse serviceResult = new ProjectResponse(
                projectId, workspaceId, "My Project", "my-project", "a description", now, now);

        when(projectCreateService.create(eq(workspaceId), eq(userId), any(ProjectCreateRequest.class)))
                .thenReturn(serviceResult);

        mockMvc.perform(post("/api/workspaces/{workspaceId}/projects", workspaceId)
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.containsString("/api/workspaces/" + workspaceId + "/projects/" + projectId)))
                .andExpect(jsonPath("$.id").value(projectId.toString()))
                .andExpect(jsonPath("$.workspaceId").value(workspaceId.toString()))
                .andExpect(jsonPath("$.name").value("My Project"))
                .andExpect(jsonPath("$.slug").value("my-project"))
                .andExpect(jsonPath("$.description").value("a description"));

        Mockito.verify(projectCreateService).create(eq(workspaceId), eq(userId), any(ProjectCreateRequest.class));
    }

    @Test
    void nonMemberCannotCreateProject() throws Exception {
        UUID userId = UUID.randomUUID();
        LocalUserPrincipal principal = new LocalUserPrincipal(userId, "bob", "hashed-pw");
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());

        UUID workspaceId = UUID.randomUUID();
        ProjectCreateRequest requestBody = new ProjectCreateRequest("Sneaky Project", null, null);

        when(projectCreateService.create(eq(workspaceId), eq(userId), any(ProjectCreateRequest.class)))
                .thenThrow(new ForbiddenException("Workspace not accessible"));

        mockMvc.perform(post("/api/workspaces/{workspaceId}/projects", workspaceId)
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Workspace not accessible"));
    }

    @Test
    void blankNameIsRejectedWith400() throws Exception {
        UUID userId = UUID.randomUUID();
        LocalUserPrincipal principal = new LocalUserPrincipal(userId, "alice", "hashed-pw");
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());

        UUID workspaceId = UUID.randomUUID();
        // name is blank — should fail @NotBlank
        String badBody = """
                {"name":"  ","slug":null,"description":null}
                """;

        mockMvc.perform(post("/api/workspaces/{workspaceId}/projects", workspaceId)
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));

        Mockito.verifyNoInteractions(projectCreateService);
    }

    @Test
    void unauthenticatedPostRequestIsRejectedWith401() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        String body = """
                {"name":"Some Project","slug":null,"description":null}
                """;
        mockMvc.perform(post("/api/workspaces/{workspaceId}/projects", workspaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
        Mockito.verifyNoInteractions(projectCreateService);
    }
}
