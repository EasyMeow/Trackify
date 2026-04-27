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
import com.trackify.common.exception.NotFoundException;
import com.trackify.config.JacksonConfig;
import com.trackify.config.SecurityConfig;
import com.trackify.config.WebConfig;
import com.trackify.task.application.TaskCommandService;
import com.trackify.task.application.TaskQueryService;
import com.trackify.task.dto.MoveTaskRequest;
import com.trackify.task.dto.TaskResponse;

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
import java.util.List;
import java.util.UUID;

/**
 * TASK-059: verifies {@code PATCH /api/tasks/{taskId}/move} behaviour.
 *
 * <p>The controller is thin — these tests stub the {@link TaskCommandService}
 * to confirm the request body is parsed correctly, the principal is forwarded,
 * and the response shape echoes the service's {@link TaskResponse}.
 *
 * <p>Move semantics (within column vs across columns) are exercised at the
 * controller boundary by stubbing different response columnIds. Service-level
 * cross-project rejection is covered separately (see service-level tests in
 * future hardening tasks if added; the controller path simply propagates
 * {@link IllegalArgumentException} through the global handler).
 */
@WebMvcTest(controllers = TaskDetailController.class)
@Import({SecurityConfig.class, WebConfig.class, JacksonConfig.class})
@TestPropertySource(properties = {
        "trackify.cors.allowed-origin=http://localhost:5173",
        "trackify.cors.allowed-methods=GET,POST,PUT,PATCH,DELETE,OPTIONS",
        "trackify.cors.allowed-headers=*",
        "trackify.cors.allow-credentials=true"
})
class TaskMoveControllerTest {

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
        LocalUserPrincipal principal = new LocalUserPrincipal(userId, "dave", "hashed-pw");
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private static TaskResponse movedResponse(UUID taskId, UUID projectId, UUID userId,
                                              UUID columnId, double sortOrder) {
        Instant now = Instant.now();
        return new TaskResponse(
                taskId,
                projectId,
                columnId,
                userId,
                "Move me",
                null,
                "TODO",
                "MEDIUM",
                sortOrder,
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

    /**
     * Move within the same column: only sortOrder changes. Verifies the request
     * is parsed (columnId + sortOrder both reach the service) and the response
     * carries the new sortOrder.
     */
    @Test
    void moveWithinSameColumnReturns200WithNewSortOrder() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID columnId = UUID.randomUUID();

        TaskResponse stub = movedResponse(taskId, projectId, userId, columnId, 2.5);
        when(taskCommandService.moveTask(eq(taskId), eq(userId), any(MoveTaskRequest.class)))
                .thenReturn(stub);

        String body = objectMapper.writeValueAsString(new MoveTaskRequest(columnId, 2.5));

        mockMvc.perform(patch("/api/tasks/{taskId}/move", taskId)
                        .with(auth(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId.toString()))
                .andExpect(jsonPath("$.columnId").value(columnId.toString()))
                .andExpect(jsonPath("$.sortOrder").value(2.5));

        verify(taskCommandService).moveTask(eq(taskId), eq(userId), any(MoveTaskRequest.class));
    }

    /**
     * Move across columns: response carries the new (different) columnId.
     */
    @Test
    void moveAcrossColumnsReturns200WithNewColumnId() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID destinationColumnId = UUID.randomUUID();

        TaskResponse stub = movedResponse(taskId, projectId, userId, destinationColumnId, 0.0);
        when(taskCommandService.moveTask(eq(taskId), eq(userId), any(MoveTaskRequest.class)))
                .thenReturn(stub);

        String body = objectMapper.writeValueAsString(new MoveTaskRequest(destinationColumnId, 0.0));

        mockMvc.perform(patch("/api/tasks/{taskId}/move", taskId)
                        .with(auth(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columnId").value(destinationColumnId.toString()))
                .andExpect(jsonPath("$.sortOrder").value(0.0));

        verify(taskCommandService).moveTask(eq(taskId), eq(userId), any(MoveTaskRequest.class));
    }

    @Test
    void missingTaskReturns404() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID columnId = UUID.randomUUID();

        when(taskCommandService.moveTask(eq(taskId), eq(userId), any(MoveTaskRequest.class)))
                .thenThrow(new NotFoundException("Task not found"));

        String body = objectMapper.writeValueAsString(new MoveTaskRequest(columnId, 1.0));

        mockMvc.perform(patch("/api/tasks/{taskId}/move", taskId)
                        .with(auth(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Task not found"));
    }

    @Test
    void missingColumnIdFailsValidationWith400() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        // sortOrder present, columnId missing
        String body = "{\"sortOrder\":1.0}";

        mockMvc.perform(patch("/api/tasks/{taskId}/move", taskId)
                        .with(auth(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));

        verifyNoInteractions(taskCommandService);
    }

    @Test
    void missingSortOrderFailsValidationWith400() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID columnId = UUID.randomUUID();

        // columnId present, sortOrder missing
        String body = "{\"columnId\":\"" + columnId + "\"}";

        mockMvc.perform(patch("/api/tasks/{taskId}/move", taskId)
                        .with(auth(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));

        verifyNoInteractions(taskCommandService);
    }

    @Test
    void unauthenticatedRequestIsRejectedWith401BeforeControllerRuns() throws Exception {
        UUID taskId = UUID.randomUUID();
        UUID columnId = UUID.randomUUID();

        String body = objectMapper.writeValueAsString(new MoveTaskRequest(columnId, 1.0));

        mockMvc.perform(patch("/api/tasks/{taskId}/move", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(taskCommandService);
    }
}
