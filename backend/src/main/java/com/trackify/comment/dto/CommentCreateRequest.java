package com.trackify.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /api/tasks/{taskId}/comments} (TASK-063).
 *
 * <p>{@code body} is required, non-blank, and capped at 10 000 characters —
 * enough for a substantial comment while still fitting comfortably in the DB
 * column (TEXT). The cap mirrors the pattern in {@link com.trackify.task.dto.CreateTaskRequest}
 * but uses a higher limit appropriate for comment text.
 */
public record CommentCreateRequest(
        @NotBlank(message = "Comment body must not be blank")
        @Size(max = 10000, message = "Comment body must be at most 10000 characters")
        String body
) {
}
