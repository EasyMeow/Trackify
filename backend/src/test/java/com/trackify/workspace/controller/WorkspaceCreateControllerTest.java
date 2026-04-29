package com.trackify.workspace.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackify.auth.application.LocalUserPrincipal;
import com.trackify.config.JacksonConfig;
import com.trackify.config.SecurityConfig;
import com.trackify.config.WebConfig;
import com.trackify.workspace.application.WorkspaceCreateService;
import com.trackify.workspace.application.WorkspaceDeleteService;
import com.trackify.workspace.application.WorkspaceQueryService;
import com.trackify.workspace.application.WorkspaceRenameService;
import com.trackify.workspace.dto.CreateWorkspaceRequest;
import com.trackify.workspace.dto.WorkspaceResponse;

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

import java.util.List;
import java.util.UUID;

/**
 * TASK-108: verifies {@code POST /api/workspaces} behaviour.
 *
 * <ul>
 *   <li>Valid request creates workspace with the current user as OWNER and returns 201.
 *   <li>Created workspace appears in the subsequent {@code GET /api/workspaces} response.
 *   <li>Blank name is rejected with 400 VALIDATION_FAILED.
 *   <li>Unauthenticated request is rejected with 401.
 * </ul>
 */
@WebMvcTest(controllers = WorkspaceController.class)
@Import({SecurityConfig.class, WebConfig.class, JacksonConfig.class})
@TestPropertySource(properties = {
        "trackify.cors.allowed-origin=http://localhost:5173",
        "trackify.cors.allowed-methods=GET,POST,PUT,PATCH,DELETE,OPTIONS",
        "trackify.cors.allowed-headers=*",
        "trackify.cors.allow-credentials=true"
})
class WorkspaceCreateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WorkspaceQueryService workspaceQueryService;

    @MockitoBean
    private WorkspaceCreateService workspaceCreateService;

    @MockitoBean
    private WorkspaceRenameService workspaceRenameService;

    @MockitoBean
    private WorkspaceDeleteService workspaceDeleteService;

    // -------------------------------------------------------------------------
    // Happy path: authenticated user creates a workspace and gets 201
    // -------------------------------------------------------------------------

    @Test
    void authenticatedUserCanCreateWorkspace() throws Exception {
        UUID userId = UUID.randomUUID();
        LocalUserPrincipal principal = new LocalUserPrincipal(userId, "alice", "hashed-pw");
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());

        UUID newWsId = UUID.randomUUID();
        WorkspaceResponse serviceResult = new WorkspaceResponse(newWsId, "My Team", "my-team");

        when(workspaceCreateService.create(eq(userId), any(CreateWorkspaceRequest.class)))
                .thenReturn(serviceResult);

        CreateWorkspaceRequest requestBody = new CreateWorkspaceRequest("My Team");

        mockMvc.perform(post("/api/workspaces")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.containsString("/api/workspaces/" + newWsId)))
                .andExpect(jsonPath("$.id").value(newWsId.toString()))
                .andExpect(jsonPath("$.name").value("My Team"))
                .andExpect(jsonPath("$.slug").value("my-team"));

        verify(workspaceCreateService).create(eq(userId), any(CreateWorkspaceRequest.class));
    }

    // -------------------------------------------------------------------------
    // Scenario (b): created workspace appears in GET /api/workspaces
    // -------------------------------------------------------------------------

    @Test
    void createdWorkspaceAppearsInSubsequentGetListing() throws Exception {
        UUID userId = UUID.randomUUID();
        LocalUserPrincipal principal = new LocalUserPrincipal(userId, "bob", "hashed-pw");
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());

        UUID existingWsId = UUID.randomUUID();
        UUID newWsId = UUID.randomUUID();

        WorkspaceResponse newWs = new WorkspaceResponse(newWsId, "New Workspace", "new-workspace");

        when(workspaceCreateService.create(eq(userId), any(CreateWorkspaceRequest.class)))
                .thenReturn(newWs);

        // After creation, the query service now returns both workspaces.
        when(workspaceQueryService.listForUser(userId)).thenReturn(List.of(
                new WorkspaceResponse(existingWsId, "Bob's workspace", "bob"),
                newWs
        ));

        // Step 1: create.
        mockMvc.perform(post("/api/workspaces")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateWorkspaceRequest("New Workspace"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(newWsId.toString()));

        // Step 2: list — the new workspace must be present.
        mockMvc.perform(get("/api/workspaces").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].id").value(newWsId.toString()))
                .andExpect(jsonPath("$[1].name").value("New Workspace"))
                .andExpect(jsonPath("$[1].slug").value("new-workspace"));
    }

    // -------------------------------------------------------------------------
    // Validation: blank name returns 400
    // -------------------------------------------------------------------------

    @Test
    void blankNameIsRejectedWith400() throws Exception {
        UUID userId = UUID.randomUUID();
        LocalUserPrincipal principal = new LocalUserPrincipal(userId, "alice", "hashed-pw");
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());

        String badBody = """
                {"name":"   "}
                """;

        mockMvc.perform(post("/api/workspaces")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));

        verifyNoInteractions(workspaceCreateService);
    }

    // -------------------------------------------------------------------------
    // Security: unauthenticated request returns 401
    // -------------------------------------------------------------------------

    @Test
    void unauthenticatedPostIsRejectedWith401() throws Exception {
        String body = """
                {"name":"Some Workspace"}
                """;

        mockMvc.perform(post("/api/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(workspaceCreateService);
    }
}
