package com.trackify.workspace.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trackify.auth.application.LocalUserPrincipal;
import com.trackify.config.JacksonConfig;
import com.trackify.config.SecurityConfig;
import com.trackify.config.WebConfig;
import com.trackify.workspace.application.WorkspaceCreateService;
import com.trackify.workspace.application.WorkspaceQueryService;
import com.trackify.workspace.application.WorkspaceRenameService;
import com.trackify.workspace.dto.WorkspaceResponse;

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
 * TASK-035: verifies {@code GET /api/workspaces} behaviour.
 *
 * <ul>
 *   <li>Authenticated user with N memberships gets exactly those N workspaces —
 *       another user's workspaces are invisible because the service is stubbed to
 *       return only the caller's data.
 *   <li>Unauthenticated request is rejected with 401 by Spring Security before
 *       the controller method is invoked.
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
class WorkspaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkspaceQueryService workspaceQueryService;

    @MockitoBean
    private WorkspaceCreateService workspaceCreateService;

    @MockitoBean
    private WorkspaceRenameService workspaceRenameService;

    // ---------------------------------------------------------------------------
    // Happy path: authenticated user gets exactly their own workspaces
    // ---------------------------------------------------------------------------

    @Test
    void authenticatedUserReceivesTheirOwnWorkspaces() throws Exception {
        UUID userId = UUID.randomUUID();
        LocalUserPrincipal principal = new LocalUserPrincipal(userId, "alice", "hashed-pw");
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of());

        UUID wsId1 = UUID.randomUUID();
        UUID wsId2 = UUID.randomUUID();
        List<WorkspaceResponse> serviceResult = List.of(
                new WorkspaceResponse(wsId1, "Alice's workspace", "alice"),
                new WorkspaceResponse(wsId2, "Team Alpha", "team-alpha")
        );
        when(workspaceQueryService.listForUser(userId)).thenReturn(serviceResult);

        mockMvc.perform(get("/api/workspaces").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(wsId1.toString()))
                .andExpect(jsonPath("$[0].name").value("Alice's workspace"))
                .andExpect(jsonPath("$[0].slug").value("alice"))
                .andExpect(jsonPath("$[1].id").value(wsId2.toString()))
                .andExpect(jsonPath("$[1].name").value("Team Alpha"))
                .andExpect(jsonPath("$[1].slug").value("team-alpha"));
    }

    @Test
    void otherUsersWorkspacesAreNotVisible() throws Exception {
        UUID aliceId = UUID.randomUUID();
        LocalUserPrincipal alicePrincipal = new LocalUserPrincipal(aliceId, "alice", "hashed-pw");
        Authentication aliceAuth = new UsernamePasswordAuthenticationToken(
                alicePrincipal, null, List.of());

        // Service returns only Alice's workspace when called with Alice's id.
        UUID aliceWsId = UUID.randomUUID();
        when(workspaceQueryService.listForUser(aliceId))
                .thenReturn(List.of(new WorkspaceResponse(aliceWsId, "Alice's workspace", "alice")));

        // Bob is a different user — his workspaces are never returned to Alice.
        UUID bobId = UUID.randomUUID();
        UUID bobWsId = UUID.randomUUID();
        when(workspaceQueryService.listForUser(bobId))
                .thenReturn(List.of(new WorkspaceResponse(bobWsId, "Bob's workspace", "bob")));

        // Alice's request must contain only Alice's workspace.
        mockMvc.perform(get("/api/workspaces").with(authentication(aliceAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(aliceWsId.toString()))
                .andExpect(jsonPath("$[0].slug").value("alice"));
    }

    @Test
    void userWithNoMembershipsReceivesEmptyList() throws Exception {
        UUID userId = UUID.randomUUID();
        LocalUserPrincipal principal = new LocalUserPrincipal(userId, "newuser", "hashed-pw");
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of());

        when(workspaceQueryService.listForUser(userId)).thenReturn(List.of());

        mockMvc.perform(get("/api/workspaces").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ---------------------------------------------------------------------------
    // Security: unauthenticated requests must be rejected
    // ---------------------------------------------------------------------------

    @Test
    void unauthenticatedRequestIsRejectedWith401BeforeControllerRuns() throws Exception {
        mockMvc.perform(get("/api/workspaces"))
                .andExpect(status().isUnauthorized());
    }
}
