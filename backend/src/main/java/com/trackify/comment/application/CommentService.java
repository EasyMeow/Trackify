package com.trackify.comment.application;

import com.trackify.comment.domain.Comment;
import com.trackify.comment.dto.CommentCreateRequest;
import com.trackify.comment.dto.CommentResponse;
import com.trackify.comment.infrastructure.CommentRepository;
import com.trackify.common.exception.NotFoundException;
import com.trackify.project.application.ProjectQueryService;
import com.trackify.task.infrastructure.TaskRepository;
import com.trackify.user.domain.User;
import com.trackify.user.infrastructure.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Application service for task comment operations (TASK-063).
 *
 * <p>Authorization follows the same pattern as {@link com.trackify.task.application.TaskQueryService}:
 * load the task, resolve its project, then delegate to {@link ProjectQueryService#getById}
 * which throws {@link NotFoundException} (404) when the project does not exist
 * and {@link com.trackify.common.exception.ForbiddenException} (403) when the
 * caller is not a workspace member.
 *
 * <p>Transactions live here, not in the controller.
 */
@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final ProjectQueryService projectQueryService;
    private final UserRepository userRepository;

    public CommentService(CommentRepository commentRepository,
                          TaskRepository taskRepository,
                          ProjectQueryService projectQueryService,
                          UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.taskRepository = taskRepository;
        this.projectQueryService = projectQueryService;
        this.userRepository = userRepository;
    }

    /**
     * Returns all comments for a task the caller can access, in chronological order.
     *
     * <p>Authorization: loads the task to resolve its project, then delegates to
     * {@link ProjectQueryService#getById} which enforces workspace membership.
     *
     * <p>Author names are resolved in a single batch lookup so there is no N+1 query.
     *
     * @param currentUserId authenticated caller's UUID
     * @param taskId        UUID of the task whose comments to list
     * @return comments ordered oldest-first; empty list when no comments exist
     * @throws NotFoundException  if no task with {@code taskId} exists
     * @throws com.trackify.common.exception.ForbiddenException if caller cannot access the task
     */
    @Transactional(readOnly = true)
    public List<CommentResponse> listComments(UUID currentUserId, UUID taskId) {
        verifyAccess(currentUserId, taskId);

        List<Comment> comments = commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId);

        // Batch-resolve author display names — one query for all distinct author ids
        Set<UUID> authorIds = comments.stream()
                .map(Comment::getAuthorId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        Map<UUID, String> nameById = authorIds.isEmpty()
                ? Map.of()
                : userRepository.findAllById(authorIds).stream()
                        .collect(Collectors.toMap(User::getId, User::getDisplayName));

        return comments.stream()
                .map(c -> toResponse(c, nameById))
                .toList();
    }

    /**
     * Creates a comment on a task the caller can access.
     *
     * <p>Authorization: same as {@link #listComments} — verifies workspace membership
     * before any write occurs.
     *
     * @param currentUserId authenticated caller's UUID (becomes {@code author_id})
     * @param taskId        UUID of the task to comment on
     * @param request       validated request body containing the comment text
     * @return the persisted comment as a {@link CommentResponse}
     * @throws NotFoundException  if no task with {@code taskId} exists
     * @throws com.trackify.common.exception.ForbiddenException if caller cannot access the task
     */
    @Transactional
    public CommentResponse createComment(UUID currentUserId, UUID taskId, CommentCreateRequest request) {
        verifyAccess(currentUserId, taskId);

        Comment comment = new Comment(taskId, currentUserId, request.body());
        Comment saved = commentRepository.save(comment);

        // Resolve the author name for the response
        String authorName = userRepository.findById(currentUserId)
                .map(User::getDisplayName)
                .orElse(null);

        return toResponse(saved, currentUserId, authorName);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Verifies the caller can access the task. Throws 404 if the task does not
     * exist, or delegates to {@link ProjectQueryService#getById} which throws 404
     * (project missing) or 403 (caller not a workspace member).
     */
    private void verifyAccess(UUID currentUserId, UUID taskId) {
        var task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        // Delegates authorization; throws NotFoundException or ForbiddenException
        projectQueryService.getById(task.getProjectId(), currentUserId);
    }

    private static CommentResponse toResponse(Comment comment, Map<UUID, String> nameById) {
        UUID authorId = comment.getAuthorId();
        String authorName = (authorId != null) ? nameById.get(authorId) : null;
        return toResponse(comment, authorId, authorName);
    }

    private static CommentResponse toResponse(Comment comment, UUID authorId, String authorName) {
        return new CommentResponse(
                comment.getId(),
                comment.getTaskId(),
                authorId,
                authorName,
                comment.getBody(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
