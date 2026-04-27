package com.trackify.timeline.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trackify.auth.application.LocalUserPrincipal;
import com.trackify.common.exception.ForbiddenException;
import com.trackify.common.exception.NotFoundException;
import com.trackify.config.JacksonConfig;
import com.trackify.config.SecurityConfig;
import com.trackify.config.WebConfig;
import com.trackify.timeline.application.TimelineService;
import com.trackify.timeline.dto.TimelineResponse;
import com.trackify.timeline.dto.TimelineTaskResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * TASK-067: verifies {@code GET /api/projects/{projectId}/timeline} behaviour.
 *
 * <ul>
 *   <li>Happy path — member receives timeline with task ids, titles, start dates, and due dates.
 *   <li>Empty project — timeline is returned with empty task list.
 *   <li>Non-member — service throws {@link ForbiddenException}, response is 403.
 *   <li>Missing project — service throws {@link NotFoundException}, response is 404.
 *   <li>Unauthenticated — Spring Security rejects with 401 before controller runs.
 * </ul>
 */
@WebMvcTest(controllers = TimelineController.class)
@Import({SecurityConfig.class, WebConfig.class, JacksonConfig.class})
@TestPropertySource(properties = {
        "trackify.cors.allowed-origin=http://localhost:5173",
        "trackify.cors.allowed-methods=GET,POST,PUT,PATCH,DELETE,OPTIONS",
        "trackify.cors.allowed-headers=*",
        "trackify.cors.allow-credentials=true"
})
class TimelineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TimelineService timelineService;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static RequestPostProcessor auth(UUID userId) {
        LocalUserPrincipal principal = new LocalUserPrincipal(userId, "alice", "hashed-pw");
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    void memberReceivesTimelineWithTaskFields() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        LocalDate start = LocalDate.of(2026, 4, 1);
        LocalDate due = LocalDate.of(2026, 4, 30);

        TimelineTaskResponse taskRow = new TimelineTaskResponse(
                taskId, "Build timeline endpoint", "IN_PROGRESS", "HIGH", start, due);

        TimelineResponse response = new TimelineResponse(projectId, List.of(taskRow));
        when(timelineService.getTimeline(projectId, userId)).thenReturn(response);

        mockMvc.perform(get("/api/projects/{projectId}/timeline", projectId).with(auth(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(projectId.toString()))
                .andExpect(jsonPath("$.tasks.length()").value(1))
                .andExpect(jsonPath("$.tasks[0].id").value(taskId.toString()))
                .andExpect(jsonPath("$.tasks[0].title").value("Build timeline endpoint"))
                .andExpect(jsonPath("$.tasks[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.tasks[0].priority").value("HIGH"))
                .andExpect(jsonPath("$.tasks[0].startDate").value("2026-04-01"))
                .andExpect(jsonPath("$.tasks[0].dueDate").value("2026-04-30"));
    }

    @Test
    void emptyProjectReturnsEmptyTaskList() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();

        TimelineResponse response = new TimelineResponse(projectId, List.of());
        when(timelineService.getTimeline(projectId, userId)).thenReturn(response);

        mockMvc.perform(get("/api/projects/{projectId}/timeline", projectId).with(auth(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(projectId.toString()))
                .andExpect(jsonPath("$.tasks.length()").value(0));
    }

    @Test
    void nonMemberReceivesForbidden() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();

        when(timelineService.getTimeline(projectId, userId))
                .thenThrow(new ForbiddenException("Project not accessible"));

        mockMvc.perform(get("/api/projects/{projectId}/timeline", projectId).with(auth(userId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Project not accessible"));
    }

    @Test
    void missingProjectReturns404() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();

        when(timelineService.getTimeline(projectId, userId))
                .thenThrow(new NotFoundException("Project not found"));

        mockMvc.perform(get("/api/projects/{projectId}/timeline", projectId).with(auth(userId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Project not found"));
    }

    @Test
    void unauthenticatedRequestIsRejectedWith401BeforeControllerRuns() throws Exception {
        UUID projectId = UUID.randomUUID();
        mockMvc.perform(get("/api/projects/{projectId}/timeline", projectId))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(timelineService);
    }
}
