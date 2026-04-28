package com.trackify.board.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackify.auth.application.LocalUserPrincipal;
import com.trackify.board.application.BoardColumnService;
import com.trackify.board.dto.ColumnResponse;
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
 * TASK-098: verifies {@code POST /api/projects/{projectId}/columns} behaviour.
 *
 * <ul>
 *   <li>Happy path — member creates a column; receives 201 with Location header and column DTO.
 *   <li>Missing project — service throws {@link NotFoundException}, response is 404.
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
}
