package com.trackify.comment.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a single comment (TASK-063).
 *
 * <p>{@code authorId} and {@code authorName} are nullable: the comment entity
 * stores author_id as nullable (ON DELETE SET NULL), so a comment whose author
 * account was deleted still returns a response — it just has null author fields.
 */
public record CommentResponse(
        UUID id,
        UUID taskId,
        UUID authorId,
        String authorName,
        String body,
        Instant createdAt,
        Instant updatedAt
) {
}
