package com.trackify.project.controller;

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
import com.trackify.common.exception.ForbiddenException;
import com.trackify.common.exception.NotFoundException;
import com.trackify.config.JacksonConfig;
import com.trackify.config.SecurityConfig;
import com.trackify.config.WebConfig;
import com.trackify.project.application.ProjectDeleteService;
import com.trackify.project.application.ProjectQueryService;
import com.trackify.project.application.ProjectUpdateService;

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
 * TASK-095: verifies {@code DELETE /api/projects/{projectId}} behaviour.
 *
 * <ul>
 *   <li>Happy path — member deletes project → 204 No Content.
 *   <li>Missing project — service throws {@link NotFoundException}, response is 404.
 *   <li>Forbidden — non-member gets 403.
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
class ProjectDeleteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectQueryService projectQueryService;

    @MockitoBean
    private ProjectUpdateService projectUpdateService;

    @MockitoBean
    private ProjectDeleteService projectDeleteService;

    private Authentication authFor(UUID userId, String login) {
        LocalUserPrincipal principal = new LocalUserPrincipal(userId, login, "hashed-pw");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }

    @Test
    void memberDeletesProjectReturns204() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();

        doNothing().when(projectDeleteService).delete(eq(projectId), eq(userId));

        mockMvc.perform(delete("/api/projects/{projectId}", projectId)
                        .with(authentication(authFor(userId, "alice"))))
                .andExpect(status().isNoContent());

        verify(projectDeleteService).delete(projectId, userId);
    }

    @Test
    void missingProjectReturns404() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();

        doThrow(new NotFoundException("Project not found"))
                .when(projectDeleteService).delete(eq(projectId), eq(userId));

        mockMvc.perform(delete("/api/projects/{projectId}", projectId)
                        .with(authentication(authFor(userId, "alice"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void nonMemberReceivesForbidden() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();

        doThrow(new ForbiddenException("Project not accessible"))
                .when(projectDeleteService).delete(eq(projectId), eq(userId));

        mockMvc.perform(delete("/api/projects/{projectId}", projectId)
                        .with(authentication(authFor(userId, "bob"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void unauthenticatedRequestIsRejectedWith401() throws Exception {
        UUID projectId = UUID.randomUUID();

        mockMvc.perform(delete("/api/projects/{projectId}", projectId))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(projectDeleteService);
    }
}
