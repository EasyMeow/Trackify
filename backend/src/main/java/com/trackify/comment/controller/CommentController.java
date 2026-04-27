package com.trackify.comment.controller;

import com.trackify.auth.application.LocalUserPrincipal;
import com.trackify.comment.application.CommentService;
import com.trackify.comment.dto.CommentCreateRequest;
import com.trackify.comment.dto.CommentResponse;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Comment endpoints for a task (TASK-063).
 *
 * <p>Thin controller — authorization and business logic are in {@link CommentService}.
 * No {@code @Transactional} here.
 */
@RestController
@RequestMapping("/api/tasks/{taskId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * Lists all comments for a task in chronological order.
     *
     * @param taskId    UUID of the task
     * @param principal authenticated caller
     * @return HTTP 200 with the list of {@link CommentResponse} objects
     */
    @GetMapping
    public ResponseEntity<List<CommentResponse>> listComments(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal LocalUserPrincipal principal) {
        List<CommentResponse> comments = commentService.listComments(principal.userId(), taskId);
        return ResponseEntity.ok(comments);
    }

    /**
     * Creates a comment on a task.
     *
     * @param taskId    UUID of the task to comment on
     * @param request   validated request body with the comment text
     * @param principal authenticated caller (becomes the comment author)
     * @return HTTP 201 with the created {@link CommentResponse} body
     */
    @PostMapping
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable UUID taskId,
            @RequestBody @Valid CommentCreateRequest request,
            @AuthenticationPrincipal LocalUserPrincipal principal) {
        CommentResponse response = commentService.createComment(principal.userId(), taskId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
