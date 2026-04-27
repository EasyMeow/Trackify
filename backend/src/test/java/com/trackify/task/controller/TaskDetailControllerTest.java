package com.trackify.task.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trackify.auth.application.LocalUserDetailsService;
import com.trackify.auth.application.LocalUserPrincipal;
import com.trackify.common.exception.ForbiddenException;
import com.trackify.common.exception.NotFoundException;
import com.trackify.config.JacksonConfig;
import com.trackify.config.SecurityConfig;
import com.trackify.config.WebConfig;
import com.trackify.task.application.TaskQueryService;
import com.trackify.task.dto.TaskResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * TASK-052: verifies {@code GET /api/tasks/{taskId}} behaviour.
 *
 * <ul>
 *   <li>Happy path — authenticated member fetches a task; expects HTTP 200 with full body.
 *   <li>Forbidden — service throws {@link ForbiddenException} → HTTP 403.
 *   <li>Not found — service throws {@link NotFoundException} → HTTP 404.
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
class TaskDetailControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskQueryService taskQueryService;

    @MockitoBean
    private LocalUserDetailsService localUserDetailsService;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static RequestPostProcessor auth(UUID userId) {
        LocalUserPrincipal principal = new LocalUserPrincipal(userId, "bob", "hashed-pw");
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private static TaskResponse sampleTask(UUID taskId, UUID projectId, UUID userId) {
        UUID columnId = UUID.randomUUID();
        Instant now = Instant.now();
        return new TaskResponse(
                taskId,
                projectId,
                columnId,
                userId,
                "Implement feature",
                "Some description",
                "IN_PROGRESS",
                "HIGH",
                1.0,
                null,
                null,
                null,
                now,
                now
        );
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    void authenticatedMemberCanFetchTaskAndReceives200() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        TaskResponse stub = sampleTask(taskId, projectId, userId);

        when(taskQueryService.getById(eq(taskId), eq(userId))).thenReturn(stub);

        mockMvc.perform(get("/api/tasks/{taskId}", taskId)
                        .with(auth(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId.toString()))
                .andExpect(jsonPath("$.projectId").value(projectId.toString()))
                .andExpect(jsonPath("$.columnId").value(stub.columnId().toString()))
                .andExpect(jsonPath("$.createdBy").value(userId.toString()))
                .andExpect(jsonPath("$.title").value("Implement feature"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.sortOrder").value(1.0));

        verify(taskQueryService).getById(eq(taskId), eq(userId));
    }

    @Test
    void nonMemberReceivesForbidden403() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        when(taskQueryService.getById(eq(taskId), eq(userId)))
                .thenThrow(new ForbiddenException("Project not accessible"));

        mockMvc.perform(get("/api/tasks/{taskId}", taskId)
                        .with(auth(userId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Project not accessible"));
    }

    @Test
    void missingTaskReturns404() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        when(taskQueryService.getById(eq(taskId), eq(userId)))
                .thenThrow(new NotFoundException("Task not found"));

        mockMvc.perform(get("/api/tasks/{taskId}", taskId)
                        .with(auth(userId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Task not found"));
    }

    @Test
    void unauthenticatedRequestIsRejectedWith401BeforeControllerRuns() throws Exception {
        UUID taskId = UUID.randomUUID();

        mockMvc.perform(get("/api/tasks/{taskId}", taskId))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(taskQueryService);
    }
}
