package com.trackify.workspace.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
import com.trackify.workspace.application.WorkspaceCreateService;
import com.trackify.workspace.application.WorkspaceQueryService;
import com.trackify.workspace.application.WorkspaceRenameService;
import com.trackify.workspace.dto.RenameWorkspaceRequest;
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
 * TASK-109: verifies {@code PATCH /api/workspaces/{id}} behaviour.
 *
 * <ul>
 *   <li>Owner can rename the workspace (200).
 *   <li>Non-member/non-owner is rejected (403).
 *   <li>Unknown workspace is rejected (404).
 *   <li>Blank name is rejected (400).
 *   <li>Unauthenticated request is rejected (401).
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
class WorkspaceRenameControllerTest {

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

    // -------------------------------------------------------------------------
    // Happy path: owner renames workspace → 200
    // -------------------------------------------------------------------------

    @Test
    void ownerCanRenameWorkspace() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID wsId = UUID.randomUUID();
        LocalUserPrincipal principal = new LocalUserPrincipal(userId, "alice", "hashed-pw");
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());

        WorkspaceResponse updated = new WorkspaceResponse(wsId, "Renamed Workspace", "renamed-workspace");
        when(workspaceRenameService.rename(eq(wsId), eq(userId), any(RenameWorkspaceRequest.class)))
                .thenReturn(updated);

        String body = objectMapper.writeValueAsString(new RenameWorkspaceRequest("Renamed Workspace"));

        mockMvc.perform(patch("/api/workspaces/" + wsId)
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(wsId.toString()))
                .andExpect(jsonPath("$.name").value("Renamed Workspace"))
                .andExpect(jsonPath("$.slug").value("renamed-workspace"));

        verify(workspaceRenameService).rename(eq(wsId), eq(userId), any(RenameWorkspaceRequest.class));
    }

    // -------------------------------------------------------------------------
    // Non-owner/non-member is rejected with 403
    // -------------------------------------------------------------------------

    @Test
    void nonOwnerIsRejectedWith403() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID wsId = UUID.randomUUID();
        LocalUserPrincipal principal = new LocalUserPrincipal(userId, "bob", "hashed-pw");
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());

        when(workspaceRenameService.rename(eq(wsId), eq(userId), any(RenameWorkspaceRequest.class)))
                .thenThrow(new ForbiddenException("Only workspace owners may rename the workspace."));

        String body = objectMapper.writeValueAsString(new RenameWorkspaceRequest("New Name"));

        mockMvc.perform(patch("/api/workspaces/" + wsId)
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    // -------------------------------------------------------------------------
    // Unknown workspace → 404
    // -------------------------------------------------------------------------

    @Test
    void unknownWorkspaceReturns404() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID wsId = UUID.randomUUID();
        LocalUserPrincipal principal = new LocalUserPrincipal(userId, "alice", "hashed-pw");
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());

        when(workspaceRenameService.rename(eq(wsId), eq(userId), any(RenameWorkspaceRequest.class)))
                .thenThrow(new NotFoundException("Workspace not found: " + wsId));

        String body = objectMapper.writeValueAsString(new RenameWorkspaceRequest("New Name"));

        mockMvc.perform(patch("/api/workspaces/" + wsId)
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // -------------------------------------------------------------------------
    // Blank name → 400 VALIDATION_FAILED
    // -------------------------------------------------------------------------

    @Test
    void blankNameIsRejectedWith400() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID wsId = UUID.randomUUID();
        LocalUserPrincipal principal = new LocalUserPrincipal(userId, "alice", "hashed-pw");
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());

        String body = "{\"name\":\"   \"}";

        mockMvc.perform(patch("/api/workspaces/" + wsId)
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));

        verifyNoInteractions(workspaceRenameService);
    }

    // -------------------------------------------------------------------------
    // Unauthenticated → 401
    // -------------------------------------------------------------------------

    @Test
    void unauthenticatedRequestIsRejectedWith401() throws Exception {
        UUID wsId = UUID.randomUUID();
        String body = "{\"name\":\"Whatever\"}";

        mockMvc.perform(patch("/api/workspaces/" + wsId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(workspaceRenameService);
    }
}
