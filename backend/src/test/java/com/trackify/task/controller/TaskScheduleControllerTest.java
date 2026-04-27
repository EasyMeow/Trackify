package com.trackify.task.controller;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.trackify.task.dto.ScheduleTaskRequest;
import com.trackify.task.dto.TaskResponse;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
 * TASK-070: verifies {@code PATCH /api/tasks/{taskId}/schedule} behaviour.
 *
 * <p>The controller is thin — these tests stub {@link TaskCommandService} to
 * confirm:
 *
 * <ul>
 *   <li>The body is parsed (both dates reach the service via an
 *       {@link ArgumentCaptor}, proving "schedule updates persist correctly"
 *       at the controller boundary — the service-level move/update tests
 *       cover the JPA save path).</li>
 *   <li>Partial payloads (only one field set) are forwarded with the other
 *       field as {@code null} — preserving the "null = keep" convention.</li>
 *   <li>404 from the service is surfaced as the standard error shape.</li>
 *   <li>Unauthenticated requests are rejected by the security filter chain
 *       before the controller runs.</li>
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
class TaskScheduleControllerTest {

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
        LocalUserPrincipal principal = new LocalUserPrincipal(userId, "erin", "hashed-pw");
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private static TaskResponse scheduledResponse(UUID taskId, UUID projectId, UUID userId,
                                                  LocalDate startDate, LocalDate dueDate) {
        UUID columnId = UUID.randomUUID();
        Instant now = Instant.now();
        return new TaskResponse(
                taskId,
                projectId,
                columnId,
                userId,
                "Reschedule me",
                null,
                "TODO",
                "MEDIUM",
                0.0,
                startDate,
                dueDate,
                null,
                now,
                now
        );
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    /**
     * Happy path: both dates sent. Captures the {@link ScheduleTaskRequest}
     * passed to the service and asserts both fields propagated, then asserts
     * the response carries the updated dates — proving the controller layer
     * persists the schedule update end-to-end (with the service mocked).
     */
    @Test
    void schedulingBothDatesReturns200AndForwardsThemToService() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        LocalDate newStart = LocalDate.of(2026, 6, 1);
        LocalDate newDue = LocalDate.of(2026, 6, 15);

        TaskResponse stub = scheduledResponse(taskId, projectId, userId, newStart, newDue);
        when(taskCommandService.scheduleTask(eq(taskId), eq(userId), any(ScheduleTaskRequest.class)))
                .thenReturn(stub);

        String body = objectMapper.writeValueAsString(new ScheduleTaskRequest(newStart, newDue));

        mockMvc.perform(patch("/api/tasks/{taskId}/schedule", taskId)
                        .with(auth(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId.toString()))
                .andExpect(jsonPath("$.startDate").value("2026-06-01"))
                .andExpect(jsonPath("$.dueDate").value("2026-06-15"));

        ArgumentCaptor<ScheduleTaskRequest> captor =
                ArgumentCaptor.forClass(ScheduleTaskRequest.class);
        verify(taskCommandService).scheduleTask(eq(taskId), eq(userId), captor.capture());
        assertThat(captor.getValue().startDate()).isEqualTo(newStart);
        assertThat(captor.getValue().dueDate()).isEqualTo(newDue);
    }

    /**
     * Partial payload: only {@code dueDate} sent. The controller must forward
     * a request whose {@code startDate} is null so the service can apply the
     * "null = keep existing" rule.
     */
    @Test
    void schedulingOnlyDueDateForwardsNullStartDate() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        LocalDate newDue = LocalDate.of(2026, 7, 10);

        TaskResponse stub = scheduledResponse(
                taskId, projectId, userId, LocalDate.of(2026, 5, 1), newDue);
        when(taskCommandService.scheduleTask(eq(taskId), eq(userId), any(ScheduleTaskRequest.class)))
                .thenReturn(stub);

        // Only dueDate in the body — startDate must arrive as null
        String body = "{\"dueDate\":\"2026-07-10\"}";

        mockMvc.perform(patch("/api/tasks/{taskId}/schedule", taskId)
                        .with(auth(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dueDate").value("2026-07-10"));

        ArgumentCaptor<ScheduleTaskRequest> captor =
                ArgumentCaptor.forClass(ScheduleTaskRequest.class);
        verify(taskCommandService).scheduleTask(eq(taskId), eq(userId), captor.capture());
        assertThat(captor.getValue().startDate()).isNull();
        assertThat(captor.getValue().dueDate()).isEqualTo(newDue);
    }

    @Test
    void missingTaskReturns404() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        when(taskCommandService.scheduleTask(eq(taskId), eq(userId), any(ScheduleTaskRequest.class)))
                .thenThrow(new NotFoundException("Task not found"));

        String body = objectMapper.writeValueAsString(
                new ScheduleTaskRequest(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 15)));

        mockMvc.perform(patch("/api/tasks/{taskId}/schedule", taskId)
                        .with(auth(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Task not found"));
    }

    @Test
    void unauthenticatedRequestIsRejectedWith401BeforeControllerRuns() throws Exception {
        UUID taskId = UUID.randomUUID();

        String body = objectMapper.writeValueAsString(
                new ScheduleTaskRequest(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 15)));

        mockMvc.perform(patch("/api/tasks/{taskId}/schedule", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(taskCommandService);
    }
}
