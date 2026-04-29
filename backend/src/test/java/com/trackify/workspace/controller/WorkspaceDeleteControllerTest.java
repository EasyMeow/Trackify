package com.trackify.workspace.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trackify.auth.application.LocalUserPrincipal;
import com.trackify.common.exception.ConflictException;
import com.trackify.common.exception.ForbiddenException;
import com.trackify.common.exception.NotFoundException;
import com.trackify.config.JacksonConfig;
import com.trackify.config.SecurityConfig;
import com.trackify.config.WebConfig;
import com.trackify.workspace.application.WorkspaceCreateService;
import com.trackify.workspace.application.WorkspaceDeleteService;
import com.trackify.workspace.application.WorkspaceQueryService;
import com.trackify.workspace.application.WorkspaceRenameService;

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
 * TASK-110: verifies {@code DELETE /api/workspaces/{id}} behaviour.
 *
 * <ul>
 *   <li>Owner deletes workspace → 204 No Content.
 *   <li>Owner tries to delete last workspace → 409 Conflict.
 *   <li>Non-owner/non-member is rejected → 403 Forbidden.
 *   <li>Unknown workspace → 404 Not Found.
 *   <li>Unauthenticated request → 401 Unauthorized.
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
class WorkspaceDeleteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkspaceQueryService workspaceQueryService;

    @MockitoBean
    private WorkspaceCreateService workspaceCreateService;

    @MockitoBean
    private WorkspaceRenameService workspaceRenameService;

    @MockitoBean
    private WorkspaceDeleteService workspaceDeleteService;

    private Authentication authFor(UUID userId, String login) {
        LocalUserPrincipal principal = new LocalUserPrincipal(userId, login, "hashed-pw");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }

    @Test
    void ownerDeletesWorkspaceReturns204() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID wsId = UUID.randomUUID();

        doNothing().when(workspaceDeleteService).delete(eq(wsId), eq(userId));

        mockMvc.perform(delete("/api/workspaces/" + wsId)
                        .with(authentication(authFor(userId, "alice"))))
                .andExpect(status().isNoContent());

        verify(workspaceDeleteService).delete(wsId, userId);
    }

    @Test
    void lastWorkspaceDeletionIsRejectedWith409() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID wsId = UUID.randomUUID();

        doThrow(new ConflictException("Cannot delete the last workspace. Create another workspace first."))
                .when(workspaceDeleteService).delete(eq(wsId), eq(userId));

        mockMvc.perform(delete("/api/workspaces/" + wsId)
                        .with(authentication(authFor(userId, "alice"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    void nonOwnerIsRejectedWith403() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID wsId = UUID.randomUUID();

        doThrow(new ForbiddenException("Only workspace owners may delete the workspace."))
                .when(workspaceDeleteService).delete(eq(wsId), eq(userId));

        mockMvc.perform(delete("/api/workspaces/" + wsId)
                        .with(authentication(authFor(userId, "bob"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void unknownWorkspaceReturns404() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID wsId = UUID.randomUUID();

        doThrow(new NotFoundException("Workspace not found: " + wsId))
                .when(workspaceDeleteService).delete(eq(wsId), eq(userId));

        mockMvc.perform(delete("/api/workspaces/" + wsId)
                        .with(authentication(authFor(userId, "alice"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void unauthenticatedRequestIsRejectedWith401() throws Exception {
        UUID wsId = UUID.randomUUID();

        mockMvc.perform(delete("/api/workspaces/" + wsId))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(workspaceDeleteService);
    }
}
