package com.trackify.task.controller;

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
import com.trackify.auth.application.LocalUserDetailsService;
import com.trackify.auth.application.LocalUserPrincipal;
import com.trackify.common.exception.ForbiddenException;
import com.trackify.common.exception.NotFoundException;
import com.trackify.config.JacksonConfig;
import com.trackify.config.SecurityConfig;
import com.trackify.config.WebConfig;
import com.trackify.task.application.TaskCommandService;
import com.trackify.task.application.TaskQueryService;
import com.trackify.task.dto.TaskResponse;
import com.trackify.task.dto.UpdateTaskRequest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * TASK-055: verifies {@code PATCH /api/tasks/{taskId}} behaviour.
 *
 * <ul>
 *   <li>Happy path partial update — only title sent; description/priority/dates
 *       come back unchanged (as returned by the stubbed service).
 *   <li>Not found — service throws {@link NotFoundException} → HTTP 404.
 *   <li>Validation failure — blank title (min=1) → HTTP 400.
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
class TaskUpdateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskQueryService taskQueryService;

    @MockitoBean
    private TaskCommandService taskCommandService;

    @MockitoBean
    private LocalUserDetailsService localUserDetailsService;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static RequestPostProcessor auth(UUID userId) {
        LocalUserPrincipal principal = new LocalUserPrincipal(userId, "carol", "hashed-pw");
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private static TaskResponse taskWithTitle(UUID taskId, UUID projectId, UUID userId, String title) {
        UUID columnId = UUID.randomUUID();
        Instant now = Instant.now();
        return new TaskResponse(
                taskId,
                projectId,
                columnId,
                userId,
                title,
                "Original description",
                "TODO",
                "MEDIUM",
                0.0,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                null,
                now,
                now
        );
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    /**
     * Happy path: send only {@code title}; description, priority, and dates are
     * unchanged (the stub returns the original values, proving the controller
     * passes them through without modification).
     */
    @Test
    void partialUpdateWithOnlyTitleReturns200AndPreservesOtherFields() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        // Stub: service returns a response where only title changed
        TaskResponse stub = taskWithTitle(taskId, projectId, userId, "Updated title");
        when(taskCommandService.updateTask(eq(taskId), eq(userId), any(UpdateTaskRequest.class)))
                .thenReturn(stub);

        // Only title in the request body — all other fields absent
        String body = "{\"title\":\"Updated title\"}";

        mockMvc.perform(patch("/api/tasks/{taskId}", taskId)
                        .with(auth(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId.toString()))
                .andExpect(jsonPath("$.title").value("Updated title"))
                // Verify the service returned the untouched fields
                .andExpect(jsonPath("$.description").value("Original description"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.startDate").value("2026-05-01"))
                .andExpect(jsonPath("$.dueDate").value("2026-05-31"));

        verify(taskCommandService).updateTask(eq(taskId), eq(userId), any(UpdateTaskRequest.class));
    }

    @Test
    void missingTaskReturns404() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        when(taskCommandService.updateTask(eq(taskId), eq(userId), any(UpdateTaskRequest.class)))
                .thenThrow(new NotFoundException("Task not found"));

        String body = "{\"title\":\"Does not matter\"}";

        mockMvc.perform(patch("/api/tasks/{taskId}", taskId)
                        .with(auth(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Task not found"));
    }

    @Test
    void blankTitleFailsValidationWith400() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        // title present but blank — @Size(min=1) should reject
        String body = "{\"title\":\"\"}";

        mockMvc.perform(patch("/api/tasks/{taskId}", taskId)
                        .with(auth(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));

        verifyNoInteractions(taskCommandService);
    }

    /**
     * TASK-076: cross-workspace access denial. The service throws
     * {@link ForbiddenException} when the caller is not a member of the
     * task's owning workspace; the controller must surface that as HTTP 403
     * with the standard error envelope rather than leaking task data.
     */
    @Test
    void nonMemberReceivesForbidden403() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        when(taskCommandService.updateTask(eq(taskId), eq(userId), any(UpdateTaskRequest.class)))
                .thenThrow(new ForbiddenException("Project not accessible"));

        String body = "{\"title\":\"Hijack attempt\"}";

        mockMvc.perform(patch("/api/tasks/{taskId}", taskId)
                        .with(auth(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Project not accessible"));
    }

    @Test
    void unauthenticatedRequestIsRejectedWith401BeforeControllerRuns() throws Exception {
        UUID taskId = UUID.randomUUID();

        String body = "{\"title\":\"Any title\"}";

        mockMvc.perform(patch("/api/tasks/{taskId}", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(taskCommandService);
    }
}
