package com.trackify.comment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackify.auth.application.LocalUserDetailsService;
import com.trackify.auth.application.LocalUserPrincipal;
import com.trackify.comment.application.CommentService;
import com.trackify.comment.dto.CommentCreateRequest;
import com.trackify.comment.dto.CommentResponse;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * TASK-063: verifies {@code GET /api/tasks/{taskId}/comments} and
 * {@code POST /api/tasks/{taskId}/comments} behaviour.
 *
 * <ul>
 *   <li>List happy path — authenticated member lists comments; expects HTTP 200 with body array.
 *   <li>Create happy path — authenticated member creates a comment; expects HTTP 201 with body.
 *   <li>List forbidden — service throws {@link ForbiddenException} → HTTP 403.
 *   <li>Create forbidden — service throws {@link ForbiddenException} → HTTP 403.
 *   <li>List not found — service throws {@link NotFoundException} → HTTP 404.
 *   <li>Create not found — service throws {@link NotFoundException} → HTTP 404.
 *   <li>Create blank body — Bean Validation rejects before service → HTTP 400.
 *   <li>Unauthenticated — Spring Security rejects before controller → HTTP 401.
 * </ul>
 */
@WebMvcTest(controllers = CommentController.class)
@Import({SecurityConfig.class, WebConfig.class, JacksonConfig.class})
@TestPropertySource(properties = {
        "trackify.cors.allowed-origin=http://localhost:5173",
        "trackify.cors.allowed-methods=GET,POST,PUT,PATCH,DELETE,OPTIONS",
        "trackify.cors.allowed-headers=*",
        "trackify.cors.allow-credentials=true"
})
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CommentService commentService;

    @MockitoBean
    private LocalUserDetailsService localUserDetailsService;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static RequestPostProcessor auth(UUID userId) {
        LocalUserPrincipal principal = new LocalUserPrincipal(userId, "alice", "hashed-pw");
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private static CommentResponse sampleComment(UUID taskId, UUID authorId) {
        Instant now = Instant.now();
        return new CommentResponse(
                UUID.randomUUID(),
                taskId,
                authorId,
                "Alice",
                "Great task!",
                now,
                now
        );
    }

    // -------------------------------------------------------------------------
    // GET /api/tasks/{taskId}/comments
    // -------------------------------------------------------------------------

    @Test
    void authenticatedMemberCanListCommentsAndReceives200() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        CommentResponse comment = sampleComment(taskId, userId);

        when(commentService.listComments(eq(userId), eq(taskId)))
                .thenReturn(List.of(comment));

        mockMvc.perform(get("/api/tasks/{taskId}/comments", taskId)
                        .with(auth(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(comment.id().toString()))
                .andExpect(jsonPath("$[0].taskId").value(taskId.toString()))
                .andExpect(jsonPath("$[0].authorId").value(userId.toString()))
                .andExpect(jsonPath("$[0].authorName").value("Alice"))
                .andExpect(jsonPath("$[0].body").value("Great task!"));

        verify(commentService).listComments(eq(userId), eq(taskId));
    }

    @Test
    void listCommentsReturnsEmptyArrayWhenNoComments() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        when(commentService.listComments(eq(userId), eq(taskId))).thenReturn(List.of());

        mockMvc.perform(get("/api/tasks/{taskId}/comments", taskId)
                        .with(auth(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listCommentsForbiddenWhenCallerLacksAccess() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        when(commentService.listComments(eq(userId), eq(taskId)))
                .thenThrow(new ForbiddenException("Project not accessible"));

        mockMvc.perform(get("/api/tasks/{taskId}/comments", taskId)
                        .with(auth(userId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Project not accessible"));
    }

    @Test
    void listCommentsNotFoundWhenTaskMissing() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        when(commentService.listComments(eq(userId), eq(taskId)))
                .thenThrow(new NotFoundException("Task not found"));

        mockMvc.perform(get("/api/tasks/{taskId}/comments", taskId)
                        .with(auth(userId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Task not found"));
    }

    @Test
    void listCommentsUnauthenticatedRequestRejectedWith401() throws Exception {
        UUID taskId = UUID.randomUUID();

        mockMvc.perform(get("/api/tasks/{taskId}/comments", taskId))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(commentService);
    }

    // -------------------------------------------------------------------------
    // POST /api/tasks/{taskId}/comments
    // -------------------------------------------------------------------------

    @Test
    void authenticatedMemberCanCreateCommentAndReceives201() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        CommentResponse stub = sampleComment(taskId, userId);

        when(commentService.createComment(eq(userId), eq(taskId), any(CommentCreateRequest.class)))
                .thenReturn(stub);

        String body = objectMapper.writeValueAsString(new CommentCreateRequest("Great task!"));

        mockMvc.perform(post("/api/tasks/{taskId}/comments", taskId)
                        .with(auth(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(stub.id().toString()))
                .andExpect(jsonPath("$.taskId").value(taskId.toString()))
                .andExpect(jsonPath("$.authorId").value(userId.toString()))
                .andExpect(jsonPath("$.authorName").value("Alice"))
                .andExpect(jsonPath("$.body").value("Great task!"));

        verify(commentService).createComment(eq(userId), eq(taskId), any(CommentCreateRequest.class));
    }

    @Test
    void createCommentBlankBodyReturnsValidationFailure400() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        String body = objectMapper.writeValueAsString(new CommentCreateRequest(""));

        mockMvc.perform(post("/api/tasks/{taskId}/comments", taskId)
                        .with(auth(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));

        verifyNoInteractions(commentService);
    }

    @Test
    void createCommentForbiddenWhenCallerLacksAccess() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        when(commentService.createComment(eq(userId), eq(taskId), any(CommentCreateRequest.class)))
                .thenThrow(new ForbiddenException("Project not accessible"));

        String body = objectMapper.writeValueAsString(new CommentCreateRequest("Some comment"));

        mockMvc.perform(post("/api/tasks/{taskId}/comments", taskId)
                        .with(auth(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Project not accessible"));
    }

    @Test
    void createCommentNotFoundWhenTaskMissing() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        when(commentService.createComment(eq(userId), eq(taskId), any(CommentCreateRequest.class)))
                .thenThrow(new NotFoundException("Task not found"));

        String body = objectMapper.writeValueAsString(new CommentCreateRequest("Some comment"));

        mockMvc.perform(post("/api/tasks/{taskId}/comments", taskId)
                        .with(auth(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Task not found"));
    }

    @Test
    void createCommentUnauthenticatedRequestRejectedWith401() throws Exception {
        UUID taskId = UUID.randomUUID();

        String body = objectMapper.writeValueAsString(new CommentCreateRequest("Some comment"));

        mockMvc.perform(post("/api/tasks/{taskId}/comments", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(commentService);
    }
}
