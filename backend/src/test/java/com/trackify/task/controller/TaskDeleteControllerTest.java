package com.trackify.task.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trackify.auth.application.LocalUserDetailsService;
import com.trackify.auth.application.LocalUserPrincipal;
import com.trackify.common.exception.ForbiddenException;
import com.trackify.common.exception.NotFoundException;
import com.trackify.config.JacksonConfig;
import com.trackify.config.SecurityConfig;
import com.trackify.config.WebConfig;
import com.trackify.task.application.TaskCommandService;
import com.trackify.task.application.TaskQueryService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

/**
 * TASK-103: verifies {@code DELETE /api/tasks/{taskId}} behaviour.
 *
 * <ul>
 *   <li>Happy path — member deletes a task; expects HTTP 204 No Content.
 *   <li>Missing task — service throws {@link NotFoundException} → HTTP 404.
 *   <li>Forbidden — service throws {@link ForbiddenException} → HTTP 403.
 *   <li>Unauthenticated — Spring Security rejects before controller runs → HTTP 401.
 * </ul>
 */
@WebMvcTest(controllers = TaskDetailController.class)
@Import({SecurityConfig.class, WebConfig.class, JacksonConfig.class})
@TestPropertySource(properties = {
        "trackify.cors.allowed-origin=http://localhost:5173",
        "trackify.cors.allowed-methods=GET,POST,PUT,PATCH,DELETE,OPTIONS",
        "trackify.cors.allowed-headers=*",
        "trackify.cors.allow-credentials=true"
})
class TaskDeleteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskQueryService taskQueryService;

    @MockitoBean
    private TaskCommandService taskCommandService;

    @MockitoBean
    private LocalUserDetailsService localUserDetailsService;

    private static RequestPostProcessor auth(UUID userId) {
        LocalUserPrincipal principal = new LocalUserPrincipal(userId, "alice", "hashed-pw");
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @Test
    void memberDeletesTaskAndReceives204() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        doNothing().when(taskCommandService).deleteTask(eq(taskId), eq(userId));

        mockMvc.perform(delete("/api/tasks/{taskId}", taskId)
                        .with(auth(userId)))
                .andExpect(status().isNoContent());

        verify(taskCommandService).deleteTask(taskId, userId);
    }

    @Test
    void missingTaskReturns404() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        doThrow(new NotFoundException("Task not found"))
                .when(taskCommandService).deleteTask(eq(taskId), eq(userId));

        mockMvc.perform(delete("/api/tasks/{taskId}", taskId)
                        .with(auth(userId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Task not found"));
    }

    @Test
    void nonMemberReceivesForbidden403() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        doThrow(new ForbiddenException("Project not accessible"))
                .when(taskCommandService).deleteTask(eq(taskId), eq(userId));

        mockMvc.perform(delete("/api/tasks/{taskId}", taskId)
                        .with(auth(userId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Project not accessible"));
    }

    @Test
    void unauthenticatedRequestIsRejectedWith401() throws Exception {
        UUID taskId = UUID.randomUUID();

        mockMvc.perform(delete("/api/tasks/{taskId}", taskId))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(taskCommandService);
    }
}
