package com.trackify.comment.infrastructure;

import com.trackify.comment.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Persistence port for the comment domain.
 *
 * The single finder here supports the primary read path: list all comments for
 * a task in chronological order. The query is backed by the composite index
 * {@code comments_task_id_created_at_idx} defined in V8.
 *
 * Additional finders (e.g., paged listing, author lookup) will be added in
 * later tasks when the application layer requires them.
 */
public interface CommentRepository extends JpaRepository<Comment, UUID> {

    /**
     * Returns all comments for the given task ordered oldest-first.
     *
     * <p>Used by the future {@code GET /api/tasks/{taskId}/comments} endpoint
     * (TASK-063) to present the thread in chronological reading order.
     * Spring Data derives {@code WHERE task_id = ? ORDER BY created_at ASC}
     * from the method name, matching the composite index in V8.
     *
     * @param taskId the task whose comments to fetch
     * @return comments in ascending {@code created_at} order; empty list if none
     */
    List<Comment> findByTaskIdOrderByCreatedAtAsc(UUID taskId);

    void deleteByTaskIdIn(Collection<UUID> taskIds);
}
