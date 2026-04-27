package com.trackify.board.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trackify.auth.application.LocalUserPrincipal;
import com.trackify.board.application.BoardQueryService;
import com.trackify.board.dto.BoardColumnResponse;
import com.trackify.board.dto.BoardResponse;
import com.trackify.board.dto.BoardTaskCardResponse;
import com.trackify.common.exception.ForbiddenException;
import com.trackify.common.exception.NotFoundException;
import com.trackify.config.JacksonConfig;
import com.trackify.config.SecurityConfig;
import com.trackify.config.WebConfig;

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
 * TASK-047: verifies {@code GET /api/projects/{projectId}/board} behaviour.
 *
 * <ul>
 *   <li>Happy path — member receives columns in position order, tasks within
 *       each column in sortOrder order, with all card fields serialised.
 *   <li>Empty project — columns are returned with empty task lists.
 *   <li>Non-member — service throws {@link ForbiddenException}, response is 403
 *       with the standard error envelope.
 *   <li>Missing project — service throws {@link NotFoundException}, response is 404.
 *   <li>Unauthenticated — Spring Security rejects with 401 before the controller
 *       method is invoked.
 * </ul>
 */
@WebMvcTest(controllers = BoardController.class)
@Import({SecurityConfig.class, WebConfig.class, JacksonConfig.class})
@TestPropertySource(properties = {
        "trackify.cors.allowed-origin=http://localhost:5173",
        "trackify.cors.allowed-methods=GET,POST,PUT,PATCH,DELETE,OPTIONS",
        "trackify.cors.allowed-headers=*",
        "trackify.cors.allow-credentials=true"
})
class BoardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BoardQueryService boardQueryService;

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
    void memberReceivesBoardWithOrderedColumnsAndTasks() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();

        UUID colTodo = UUID.randomUUID();
        UUID colInProgress = UUID.randomUUID();
        UUID colDone = UUID.randomUUID();

        UUID taskA = UUID.randomUUID();
        UUID taskB = UUID.randomUUID();
        UUID taskC = UUID.randomUUID();

        LocalDate start = LocalDate.of(2026, 4, 1);
        LocalDate due = LocalDate.of(2026, 4, 30);

        BoardTaskCardResponse cardA = new BoardTaskCardResponse(
                taskA, "Task A", "TODO", "HIGH", 1.0, start, due);
        BoardTaskCardResponse cardB = new BoardTaskCardResponse(
                taskB, "Task B", "TODO", "MEDIUM", 2.0, null, null);
        BoardTaskCardResponse cardC = new BoardTaskCardResponse(
                taskC, "Task C", "IN_PROGRESS", "LOW", 1.0, null, due);

        BoardColumnResponse todo = new BoardColumnResponse(colTodo, "Todo", 0, List.of(cardA, cardB));
        BoardColumnResponse inProgress = new BoardColumnResponse(colInProgress, "In Progress", 1, List.of(cardC));
        BoardColumnResponse done = new BoardColumnResponse(colDone, "Done", 2, List.of());

        BoardResponse boardResponse = new BoardResponse(projectId, List.of(todo, inProgress, done));
        when(boardQueryService.getBoard(projectId, userId)).thenReturn(boardResponse);

        mockMvc.perform(get("/api/projects/{projectId}/board", projectId).with(auth(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(projectId.toString()))
                // Columns in position order
                .andExpect(jsonPath("$.columns.length()").value(3))
                .andExpect(jsonPath("$.columns[0].id").value(colTodo.toString()))
                .andExpect(jsonPath("$.columns[0].name").value("Todo"))
                .andExpect(jsonPath("$.columns[0].position").value(0))
                // Tasks within first column in sortOrder order
                .andExpect(jsonPath("$.columns[0].tasks.length()").value(2))
                .andExpect(jsonPath("$.columns[0].tasks[0].id").value(taskA.toString()))
                .andExpect(jsonPath("$.columns[0].tasks[0].title").value("Task A"))
                .andExpect(jsonPath("$.columns[0].tasks[0].status").value("TODO"))
                .andExpect(jsonPath("$.columns[0].tasks[0].priority").value("HIGH"))
                .andExpect(jsonPath("$.columns[0].tasks[0].sortOrder").value(1.0))
                .andExpect(jsonPath("$.columns[0].tasks[0].startDate").value("2026-04-01"))
                .andExpect(jsonPath("$.columns[0].tasks[0].dueDate").value("2026-04-30"))
                .andExpect(jsonPath("$.columns[0].tasks[1].id").value(taskB.toString()))
                .andExpect(jsonPath("$.columns[0].tasks[1].sortOrder").value(2.0))
                // Second column
                .andExpect(jsonPath("$.columns[1].id").value(colInProgress.toString()))
                .andExpect(jsonPath("$.columns[1].name").value("In Progress"))
                .andExpect(jsonPath("$.columns[1].position").value(1))
                .andExpect(jsonPath("$.columns[1].tasks.length()").value(1))
                .andExpect(jsonPath("$.columns[1].tasks[0].id").value(taskC.toString()))
                // Third column
                .andExpect(jsonPath("$.columns[2].id").value(colDone.toString()))
                .andExpect(jsonPath("$.columns[2].name").value("Done"))
                .andExpect(jsonPath("$.columns[2].position").value(2))
                .andExpect(jsonPath("$.columns[2].tasks.length()").value(0));
    }

    @Test
    void emptyProjectReturnsColumnsWithEmptyTaskLists() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();

        UUID colTodo = UUID.randomUUID();
        UUID colInProgress = UUID.randomUUID();
        UUID colDone = UUID.randomUUID();

        BoardColumnResponse todo = new BoardColumnResponse(colTodo, "Todo", 0, List.of());
        BoardColumnResponse inProgress = new BoardColumnResponse(colInProgress, "In Progress", 1, List.of());
        BoardColumnResponse done = new BoardColumnResponse(colDone, "Done", 2, List.of());

        BoardResponse boardResponse = new BoardResponse(projectId, List.of(todo, inProgress, done));
        when(boardQueryService.getBoard(projectId, userId)).thenReturn(boardResponse);

        mockMvc.perform(get("/api/projects/{projectId}/board", projectId).with(auth(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(projectId.toString()))
                .andExpect(jsonPath("$.columns.length()").value(3))
                .andExpect(jsonPath("$.columns[0].tasks.length()").value(0))
                .andExpect(jsonPath("$.columns[1].tasks.length()").value(0))
                .andExpect(jsonPath("$.columns[2].tasks.length()").value(0));
    }

    @Test
    void nonMemberReceivesForbidden() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();

        when(boardQueryService.getBoard(projectId, userId))
                .thenThrow(new ForbiddenException("Project not accessible"));

        mockMvc.perform(get("/api/projects/{projectId}/board", projectId).with(auth(userId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Project not accessible"));
    }

    @Test
    void missingProjectReturns404() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();

        when(boardQueryService.getBoard(projectId, userId))
                .thenThrow(new NotFoundException("Project not found"));

        mockMvc.perform(get("/api/projects/{projectId}/board", projectId).with(auth(userId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Project not found"));
    }

    @Test
    void unauthenticatedRequestIsRejectedWith401BeforeControllerRuns() throws Exception {
        UUID projectId = UUID.randomUUID();
        mockMvc.perform(get("/api/projects/{projectId}/board", projectId))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(boardQueryService);
    }
}
