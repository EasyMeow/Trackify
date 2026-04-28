package com.trackify.board.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackify.auth.application.LocalUserPrincipal;
import com.trackify.board.application.BoardColumnService;
import com.trackify.board.dto.ColumnResponse;
import com.trackify.common.exception.ConflictException;
import com.trackify.common.exception.ForbiddenException;
import com.trackify.common.exception.NotFoundException;
import com.trackify.config.JacksonConfig;
import com.trackify.config.SecurityConfig;
import com.trackify.config.WebConfig;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * TASK-098 + TASK-099: verifies column creation and rename endpoints.
 *
 * <p>POST /api/projects/{projectId}/columns (TASK-098)
 * <ul>
 *   <li>Happy path — member creates a column; receives 201 with Location header and column DTO.
 *   <li>Missing project — service throws {@link NotFoundException}, response is 404.
 *   <li>Non-member — service throws {@link ForbiddenException}, response is 403.
 *   <li>Invalid body — blank name fails validation with 400.
 *   <li>Unauthenticated — Spring Security rejects with 401.
 * </ul>
 *
 * <p>PATCH /api/projects/{projectId}/columns/{columnId} (TASK-099)
 * <ul>
 *   <li>Happy path — member renames a column; receives 200 with updated DTO.
 *   <li>Column belongs to different project — service throws {@link NotFoundException}, response is 404.
 *   <li>Non-member — service throws {@link ForbiddenException}, response is 403.
 *   <li>Invalid body — blank name fails validation with 400.
 *   <li>Unauthenticated — Spring Security rejects with 401.
 * </ul>
 */
@WebMvcTest(controllers = ColumnController.class)
@Import({SecurityConfig.class, WebConfig.class, JacksonConfig.class})
@TestPropertySource(properties = {
        "trackify.cors.allowed-origin=http://localhost:5173",
        "trackify.cors.allowed-methods=GET,POST,PUT,PATCH,DELETE,OPTIONS",
        "trackify.cors.allowed-headers=*",
        "trackify.cors.allow-credentials=true"
})
class ColumnControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BoardColumnService boardColumnService;

    private Authentication authFor(UUID userId) {
        LocalUserPrincipal principal = new LocalUserPrincipal(userId, "alice", "hashed-pw");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }

    @Test
    void memberCreatesColumnReturns201WithLocationAndDto() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID columnId = UUID.randomUUID();
        Instant now = Instant.now();

        ColumnResponse response = new ColumnResponse(columnId, projectId, "Review", 3, now, now);
        when(boardColumnService.createColumn(eq(projectId), eq(userId), eq("Review")))
                .thenReturn(response);

        String body = objectMapper.writeValueAsString(Map.of("name", "Review"));

        mockMvc.perform(post("/api/projects/{projectId}/columns", projectId)
                        .with(authentication(authFor(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.containsString("/api/projects/" + projectId + "/columns/" + columnId)))
                .andExpect(jsonPath("$.id").value(columnId.toString()))
                .andExpect(jsonPath("$.projectId").value(projectId.toString()))
                .andExpect(jsonPath("$.name").value("Review"))
                .andExpect(jsonPath("$.position").value(3));
    }

    @Test
    void missingProjectReturns404() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();

        when(boardColumnService.createColumn(eq(projectId), eq(userId), any()))
                .thenThrow(new NotFoundException("Project not found"));

        String body = objectMapper.writeValueAsString(Map.of("name", "Backlog"));

        mockMvc.perform(post("/api/projects/{projectId}/columns", projectId)
                        .with(authentication(authFor(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void nonMemberReceivesForbidden() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();

        when(boardColumnService.createColumn(eq(projectId), eq(userId), any()))
                .thenThrow(new ForbiddenException("Project not accessible"));

        String body = objectMapper.writeValueAsString(Map.of("name", "Backlog"));

        mockMvc.perform(post("/api/projects/{projectId}/columns", projectId)
                        .with(authentication(authFor(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void blankNameFailsValidationWith400() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();

        String body = objectMapper.writeValueAsString(Map.of("name", ""));

        mockMvc.perform(post("/api/projects/{projectId}/columns", projectId)
                        .with(authentication(authFor(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        verifyNoInteractions(boardColumnService);
    }

    @Test
    void unauthenticatedRequestIsRejectedWith401() throws Exception {
        UUID projectId = UUID.randomUUID();
        String body = objectMapper.writeValueAsString(Map.of("name", "New Column"));

        mockMvc.perform(post("/api/projects/{projectId}/columns", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(boardColumnService);
    }

    // --- TASK-099: PATCH /{columnId} rename tests ---

    @Test
    void memberRenamesColumnReturns200WithUpdatedDto() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID columnId = UUID.randomUUID();
        Instant now = Instant.now();

        ColumnResponse response = new ColumnResponse(columnId, projectId, "Staging", 1, now, now);
        when(boardColumnService.renameColumn(eq(projectId), eq(columnId), eq(userId), eq("Staging")))
                .thenReturn(response);

        String body = objectMapper.writeValueAsString(Map.of("name", "Staging"));

        mockMvc.perform(patch("/api/projects/{projectId}/columns/{columnId}", projectId, columnId)
                        .with(authentication(authFor(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(columnId.toString()))
                .andExpect(jsonPath("$.name").value("Staging"))
                .andExpect(jsonPath("$.projectId").value(projectId.toString()));
    }

    @Test
    void renameColumnMissingOrCrossProjectReturns404() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID columnId = UUID.randomUUID();

        when(boardColumnService.renameColumn(eq(projectId), eq(columnId), eq(userId), any()))
                .thenThrow(new NotFoundException("Column not found"));

        String body = objectMapper.writeValueAsString(Map.of("name", "Renamed"));

        mockMvc.perform(patch("/api/projects/{projectId}/columns/{columnId}", projectId, columnId)
                        .with(authentication(authFor(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void renameColumnNonMemberReceivesForbidden() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID columnId = UUID.randomUUID();

        when(boardColumnService.renameColumn(eq(projectId), eq(columnId), eq(userId), any()))
                .thenThrow(new ForbiddenException("Project not accessible"));

        String body = objectMapper.writeValueAsString(Map.of("name", "Renamed"));

        mockMvc.perform(patch("/api/projects/{projectId}/columns/{columnId}", projectId, columnId)
                        .with(authentication(authFor(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void renameColumnBlankNameFailsValidationWith400() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID columnId = UUID.randomUUID();

        String body = objectMapper.writeValueAsString(Map.of("name", ""));

        mockMvc.perform(patch("/api/projects/{projectId}/columns/{columnId}", projectId, columnId)
                        .with(authentication(authFor(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        verifyNoInteractions(boardColumnService);
    }

    @Test
    void renameColumnUnauthenticatedIsRejectedWith401() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID columnId = UUID.randomUUID();
        String body = objectMapper.writeValueAsString(Map.of("name", "Renamed"));

        mockMvc.perform(patch("/api/projects/{projectId}/columns/{columnId}", projectId, columnId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(boardColumnService);
    }

    // --- TASK-100: DELETE /{columnId} tests ---

    @Test
    void memberDeletesEmptyColumnReturns204() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID columnId = UUID.randomUUID();

        doNothing().when(boardColumnService).deleteColumn(projectId, columnId, userId);

        mockMvc.perform(delete("/api/projects/{projectId}/columns/{columnId}", projectId, columnId)
                        .with(authentication(authFor(userId))))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteNonEmptyColumnReturns409WithApiError() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID columnId = UUID.randomUUID();

        doThrow(new ConflictException("Column must be emptied before it can be deleted"))
                .when(boardColumnService).deleteColumn(projectId, columnId, userId);

        mockMvc.perform(delete("/api/projects/{projectId}/columns/{columnId}", projectId, columnId)
                        .with(authentication(authFor(userId))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    void deleteMissingOrCrossProjectColumnReturns404() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID columnId = UUID.randomUUID();

        doThrow(new NotFoundException("Column not found"))
                .when(boardColumnService).deleteColumn(projectId, columnId, userId);

        mockMvc.perform(delete("/api/projects/{projectId}/columns/{columnId}", projectId, columnId)
                        .with(authentication(authFor(userId))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deleteColumnNonMemberReceivesForbidden() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID columnId = UUID.randomUUID();

        doThrow(new ForbiddenException("Project not accessible"))
                .when(boardColumnService).deleteColumn(projectId, columnId, userId);

        mockMvc.perform(delete("/api/projects/{projectId}/columns/{columnId}", projectId, columnId)
                        .with(authentication(authFor(userId))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void deleteColumnUnauthenticatedIsRejectedWith401() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID columnId = UUID.randomUUID();

        mockMvc.perform(delete("/api/projects/{projectId}/columns/{columnId}", projectId, columnId))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(boardColumnService);
    }

    // --- TASK-101: PATCH /order reorder tests ---

    @Test
    void memberReordersColumnsReturns200WithOrderedList() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID col1 = UUID.randomUUID();
        UUID col2 = UUID.randomUUID();
        UUID col3 = UUID.randomUUID();
        Instant now = Instant.now();

        List<ColumnResponse> reordered = List.of(
                new ColumnResponse(col3, projectId, "Done", 0, now, now),
                new ColumnResponse(col1, projectId, "Todo", 1, now, now),
                new ColumnResponse(col2, projectId, "In Progress", 2, now, now)
        );

        when(boardColumnService.reorderColumns(eq(projectId), eq(userId), eq(List.of(col3, col1, col2))))
                .thenReturn(reordered);

        String body = objectMapper.writeValueAsString(Map.of("columnIds", List.of(col3, col1, col2)));

        mockMvc.perform(patch("/api/projects/{projectId}/columns/order", projectId)
                        .with(authentication(authFor(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id").value(col3.toString()))
                .andExpect(jsonPath("$[1].id").value(col1.toString()))
                .andExpect(jsonPath("$[2].id").value(col2.toString()));
    }

    @Test
    void reorderWithDuplicateOrUnknownIdReturns400() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID col1 = UUID.randomUUID();

        when(boardColumnService.reorderColumns(eq(projectId), eq(userId), any()))
                .thenThrow(new IllegalArgumentException("Duplicate column id: " + col1));

        String body = objectMapper.writeValueAsString(Map.of("columnIds", List.of(col1, col1)));

        mockMvc.perform(patch("/api/projects/{projectId}/columns/order", projectId)
                        .with(authentication(authFor(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void reorderEmptyListFailsValidationWith400() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();

        String body = objectMapper.writeValueAsString(Map.of("columnIds", List.of()));

        mockMvc.perform(patch("/api/projects/{projectId}/columns/order", projectId)
                        .with(authentication(authFor(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        verifyNoInteractions(boardColumnService);
    }

    @Test
    void reorderNonMemberReceivesForbidden() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID col1 = UUID.randomUUID();

        when(boardColumnService.reorderColumns(eq(projectId), eq(userId), any()))
                .thenThrow(new ForbiddenException("Project not accessible"));

        String body = objectMapper.writeValueAsString(Map.of("columnIds", List.of(col1)));

        mockMvc.perform(patch("/api/projects/{projectId}/columns/order", projectId)
                        .with(authentication(authFor(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void reorderUnauthenticatedIsRejectedWith401() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID col1 = UUID.randomUUID();
        String body = objectMapper.writeValueAsString(Map.of("columnIds", List.of(col1)));

        mockMvc.perform(patch("/api/projects/{projectId}/columns/order", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(boardColumnService);
    }
}
