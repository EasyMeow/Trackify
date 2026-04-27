package com.trackify.task.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.trackify.task.dto.CreateTaskRequest;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * TASK-050: verifies {@code POST /api/projects/{projectId}/tasks} behaviour.
 *
 * <ul>
 *   <li>Happy path — authenticated member creates a task; expects HTTP 201 with full body.
 *   <li>Validation failure — empty title → HTTP 400 with error envelope.
 *   <li>Forbidden — service throws {@link ForbiddenException} → HTTP 403.
 *   <li>Not found — service throws {@link NotFoundException} → HTTP 404.
 *   <li>Unauthenticated — Spring Security rejects before controller runs → HTTP 401.
 * </ul>
 */
@WebMvcTest(controllers = TaskController.class)
@Import({SecurityConfig.class, WebConfig.class, JacksonConfig.class})
@TestPropertySource(properties = {
        "trackify.cors.allowed-origin=http://localhost:5173",
        "trackify.cors.allowed-methods=GET,POST,PUT,PATCH,DELETE,OPTIONS",
        "trackify.cors.allowed-headers=*",
        "trackify.cors.allow-credentials=true"
})
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskCommandService taskCommandService;

    @MockitoBean
    private LocalUserDetailsService localUserDetailsService;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static RequestPostProcessor auth(UUID userId) {
        LocalUserPrincipal principal = new LocalUserPrincipal(userId, "alice", "hashed-pw");
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private static TaskResponse sampleResponse(UUID projectId, UUID userId) {
        UUID taskId = UUID.randomUUID();
        UUID columnId = UUID.randomUUID();
        Instant now = Instant.now();
        return new TaskResponse(
                taskId,
                projectId,
                columnId,
                userId,
                "Write docs",
                null,
                "TODO",
                "HIGH",
                0.0,
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
    void authenticatedMemberCanCreateTaskAndReceives201() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        TaskResponse stubResponse = sampleResponse(projectId, userId);

        when(taskCommandService.createTask(eq(projectId), eq(userId), any(CreateTaskRequest.class)))
                .thenReturn(stubResponse);

        String body = objectMapper.writeValueAsString(
                new CreateTaskRequest("Write docs", null, "HIGH", null, null));

        mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                        .with(auth(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(stubResponse.id().toString()))
                .andExpect(jsonPath("$.projectId").value(projectId.toString()))
                .andExpect(jsonPath("$.columnId").value(stubResponse.columnId().toString()))
                .andExpect(jsonPath("$.createdBy").value(userId.toString()))
                .andExpect(jsonPath("$.title").value("Write docs"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.sortOrder").value(0.0));

        // Prove the service was called with the correct projectId and userId
        verify(taskCommandService).createTask(eq(projectId), eq(userId), any(CreateTaskRequest.class));
    }

    @Test
    void emptyTitleReturnsValidationFailure400() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();

        String body = objectMapper.writeValueAsString(
                new CreateTaskRequest("", null, null, null, null));

        mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                        .with(auth(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));

        verifyNoInteractions(taskCommandService);
    }

    @Test
    void nonMemberReceivesForbidden403() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();

        when(taskCommandService.createTask(eq(projectId), eq(userId), any(CreateTaskRequest.class)))
                .thenThrow(new ForbiddenException("Project not accessible"));

        String body = objectMapper.writeValueAsString(
                new CreateTaskRequest("Write docs", null, null, null, null));

        mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                        .with(auth(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Project not accessible"));
    }

    @Test
    void missingProjectReturns404() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();

        when(taskCommandService.createTask(eq(projectId), eq(userId), any(CreateTaskRequest.class)))
                .thenThrow(new NotFoundException("Project not found"));

        String body = objectMapper.writeValueAsString(
                new CreateTaskRequest("Write docs", null, null, null, null));

        mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                        .with(auth(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Project not found"));
    }

    @Test
    void unauthenticatedRequestIsRejectedWith401BeforeControllerRuns() throws Exception {
        UUID projectId = UUID.randomUUID();

        String body = objectMapper.writeValueAsString(
                new CreateTaskRequest("Write docs", null, null, null, null));

        mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(taskCommandService);
    }

    @Test
    void optionalFieldsCanBeOmittedAndTaskIsStillCreated() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        TaskResponse stubResponse = sampleResponse(projectId, userId);

        when(taskCommandService.createTask(eq(projectId), eq(userId), any(CreateTaskRequest.class)))
                .thenReturn(stubResponse);

        // Only title provided — all optional fields absent
        String body = "{\"title\":\"Minimal task\"}";

        mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                        .with(auth(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectId").value(projectId.toString()));

        verify(taskCommandService).createTask(eq(projectId), eq(userId), any(CreateTaskRequest.class));
    }

    @Test
    void startDateAndDueDateAreSerializedAsIsoDate() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID columnId = UUID.randomUUID();
        Instant now = Instant.now();

        LocalDate start = LocalDate.of(2026, 5, 1);
        LocalDate due = LocalDate.of(2026, 5, 31);

        TaskResponse stubResponse = new TaskResponse(
                taskId, projectId, columnId, userId,
                "Dated task", null, "TODO", "MEDIUM", 0.0,
                start, due, null, now, now
        );

        when(taskCommandService.createTask(eq(projectId), eq(userId), any(CreateTaskRequest.class)))
                .thenReturn(stubResponse);

        String body = objectMapper.writeValueAsString(
                new CreateTaskRequest("Dated task", null, null, start, due));

        mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                        .with(auth(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.startDate").value("2026-05-01"))
                .andExpect(jsonPath("$.dueDate").value("2026-05-31"));
    }
}
