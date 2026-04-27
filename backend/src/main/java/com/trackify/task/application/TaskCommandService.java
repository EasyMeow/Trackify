package com.trackify.task.application;

import com.trackify.board.domain.BoardColumn;
import com.trackify.board.infrastructure.BoardColumnRepository;
import com.trackify.project.application.ProjectQueryService;
import com.trackify.task.domain.Task;
import com.trackify.common.exception.NotFoundException;
import com.trackify.task.dto.CreateTaskRequest;
import com.trackify.task.dto.MoveTaskRequest;
import com.trackify.task.dto.ScheduleTaskRequest;
import com.trackify.task.dto.TaskResponse;
import com.trackify.task.dto.UpdateTaskRequest;
import com.trackify.task.infrastructure.TaskRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Application service for task write operations (TASK-050).
 *
 * <p>All mutations go through this service; board and timeline modules must
 * never bypass it (architecture.md §12).
 */
@Service
@Transactional
public class TaskCommandService {

    private static final String DEFAULT_STATUS = "TODO";
    private static final String DEFAULT_PRIORITY = "MEDIUM";
    private static final Set<String> VALID_PRIORITIES = Set.of("LOW", "MEDIUM", "HIGH", "URGENT");

    private final ProjectQueryService projectQueryService;
    private final BoardColumnRepository boardColumnRepository;
    private final TaskRepository taskRepository;

    public TaskCommandService(ProjectQueryService projectQueryService,
                              BoardColumnRepository boardColumnRepository,
                              TaskRepository taskRepository) {
        this.projectQueryService = projectQueryService;
        this.boardColumnRepository = boardColumnRepository;
        this.taskRepository = taskRepository;
    }

    /**
     * Creates a new task in the first (default) board column of the project.
     *
     * <p>Authorization: delegates to {@link ProjectQueryService#getById} which
     * throws {@link com.trackify.common.exception.NotFoundException} (404) or
     * {@link com.trackify.common.exception.ForbiddenException} (403) if the
     * caller cannot access the project.
     *
     * <p>Column resolution: loads columns ordered by position and picks index 0
     * (the seeded "Todo" column). Throws {@link IllegalStateException} if no
     * columns exist — this should never happen because TASK-045 seeds defaults
     * inside the same transaction as project creation.
     *
     * <p>Sort order: {@code max(sort_order) + 1.0} for non-empty columns, or
     * {@code 0.0} for an empty column.
     *
     * <p>Priority: defaults to "MEDIUM" when null; validates against
     * LOW|MEDIUM|HIGH|URGENT, throwing {@link IllegalArgumentException} (mapped
     * to HTTP 400 by {@link com.trackify.common.exception.GlobalExceptionHandler})
     * for any other value.
     *
     * @param projectId target project UUID
     * @param userId    authenticated user's UUID (becomes {@code created_by})
     * @param request   validated request body (title required; others optional)
     * @return the persisted task as a {@link TaskResponse}
     */
    public TaskResponse createTask(UUID projectId, UUID userId, CreateTaskRequest request) {
        // Authorization — throws 404 or 403 if not accessible
        projectQueryService.getById(projectId, userId);

        // Resolve default column (first by position)
        List<BoardColumn> columns = boardColumnRepository.findByProjectIdOrderByPositionAsc(projectId);
        if (columns.isEmpty()) {
            throw new IllegalStateException("Project " + projectId + " has no board columns");
        }
        BoardColumn defaultColumn = columns.get(0);
        UUID columnId = defaultColumn.getId();

        // Compute next sort order
        double sortOrder = taskRepository.findTopByColumnIdOrderBySortOrderDesc(columnId)
                .map(t -> t.getSortOrder() + 1.0)
                .orElse(0.0);

        // Validate / default priority
        String priority = resolvePriority(request.priority());

        Task task = new Task(
                projectId,
                columnId,
                userId,
                request.title(),
                request.description(),
                DEFAULT_STATUS,
                priority,
                sortOrder,
                request.startDate(),
                request.dueDate(),
                null // estimatedHours out of scope for this task
        );

        Task saved = taskRepository.save(task);
        return toResponse(saved);
    }

    /**
     * Partially updates an existing task's content fields.
     *
     * <p>Authorization: loads the task, then delegates to
     * {@link ProjectQueryService#getById} which throws 404 if the project does not
     * exist and 403 if the caller is not a workspace member.
     *
     * <p>Only non-null fields in {@code request} are applied; null fields are left
     * unchanged on the persisted entity.
     *
     * <p>Priority: when non-null, validated against LOW|MEDIUM|HIGH|URGENT; throws
     * {@link IllegalArgumentException} (mapped to HTTP 400) for invalid values.
     *
     * @param taskId  UUID of the task to update
     * @param userId  authenticated caller's UUID
     * @param request partial update payload; any null field means "keep existing"
     * @return the updated task as a {@link TaskResponse}
     * @throws NotFoundException if no task exists with {@code taskId}
     * @throws com.trackify.common.exception.ForbiddenException if the caller cannot
     *         access the task's project workspace
     */
    public TaskResponse updateTask(UUID taskId, UUID userId, UpdateTaskRequest request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));

        // Authorization — throws 404/403 if not accessible
        projectQueryService.getById(task.getProjectId(), userId);

        // Apply only non-null fields
        if (request.title() != null) {
            task.setTitle(request.title());
        }
        if (request.description() != null) {
            task.setDescription(request.description());
        }
        if (request.priority() != null) {
            task.setPriority(resolvePriority(request.priority()));
        }
        if (request.startDate() != null) {
            task.setStartDate(request.startDate());
        }
        if (request.dueDate() != null) {
            task.setDueDate(request.dueDate());
        }

        Task saved = taskRepository.save(task);
        return toResponse(saved);
    }

    /**
     * Moves a task to a new column and sort position (TASK-059).
     *
     * <p>Used by the Kanban drag-and-drop flow. The same endpoint handles both
     * within-column reordering ({@code columnId} unchanged, new {@code sortOrder})
     * and cross-column moves (different {@code columnId}).
     *
     * <p>Authorization: loads the task, then delegates to
     * {@link ProjectQueryService#getById} which throws 404/403 if the caller is
     * not a workspace member.
     *
     * <p>Cross-project safety: the destination column must belong to the same
     * project as the task. Rejects with {@link IllegalArgumentException} (HTTP 400)
     * otherwise — preventing a task from being smuggled into another project's
     * board via a forged {@code columnId}.
     *
     * <p>Status field is intentionally not modified here. Column-to-status mapping
     * is not part of the MVP scope; the lifecycle status remains independent of
     * board position. Tasks dragged to a "Done" column keep their stored status
     * until edited explicitly via {@code PATCH /api/tasks/{id}}.
     *
     * @param taskId  UUID of the task to move
     * @param userId  authenticated caller's UUID
     * @param request validated move payload (columnId and sortOrder both required)
     * @return the updated task as a {@link TaskResponse}
     * @throws NotFoundException if the task or destination column does not exist
     * @throws IllegalArgumentException if the destination column is in a different project
     * @throws com.trackify.common.exception.ForbiddenException if the caller cannot
     *         access the task's project workspace
     */
    public TaskResponse moveTask(UUID taskId, UUID userId, MoveTaskRequest request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));

        // Authorization — throws 404/403 if not accessible
        projectQueryService.getById(task.getProjectId(), userId);

        BoardColumn targetColumn = boardColumnRepository.findById(request.columnId())
                .orElseThrow(() -> new NotFoundException("Board column not found"));

        if (!targetColumn.getProjectId().equals(task.getProjectId())) {
            throw new IllegalArgumentException(
                    "Destination column does not belong to the task's project");
        }

        task.setColumnId(request.columnId());
        task.setSortOrder(request.sortOrder());

        Task saved = taskRepository.save(task);
        return toResponse(saved);
    }

    /**
     * Updates only the schedule fields ({@code startDate}, {@code dueDate}) of a
     * task (TASK-070). Backs the Gantt timeline drag-and-resize flow.
     *
     * <p>Authorization: loads the task, then delegates to
     * {@link ProjectQueryService#getById} which throws 404/403 if the caller is
     * not a workspace member.
     *
     * <p>Only non-null fields in {@code request} are applied; null fields are left
     * unchanged on the persisted entity. Cross-field validation (start before
     * due) is not enforced — the Gantt frontend guards against inverted ranges
     * before sending.
     *
     * <p>Why a dedicated endpoint instead of reusing {@code PATCH /api/tasks/{id}}:
     * the timeline drag UI is a focused interaction whose only side effect should
     * be moving the schedule. A separate route makes that intent explicit and
     * keeps content updates (title/description/priority) out of the Gantt code path.
     *
     * @param taskId  UUID of the task to reschedule
     * @param userId  authenticated caller's UUID
     * @param request partial schedule payload; any null field means "keep existing"
     * @return the updated task as a {@link TaskResponse}
     * @throws NotFoundException if no task exists with {@code taskId}
     * @throws com.trackify.common.exception.ForbiddenException if the caller cannot
     *         access the task's project workspace
     */
    public TaskResponse scheduleTask(UUID taskId, UUID userId, ScheduleTaskRequest request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));

        // Authorization — throws 404/403 if not accessible
        projectQueryService.getById(task.getProjectId(), userId);

        if (request.startDate() != null) {
            task.setStartDate(request.startDate());
        }
        if (request.dueDate() != null) {
            task.setDueDate(request.dueDate());
        }

        Task saved = taskRepository.save(task);
        return toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the priority string to use: defaults to "MEDIUM" when {@code raw}
     * is null; validates against the DB CHECK constraint values otherwise.
     *
     * @param raw the caller-supplied priority string, may be null
     * @return a validated, non-null priority string
     * @throws IllegalArgumentException if {@code raw} is non-null and not in the valid set
     */
    private static String resolvePriority(String raw) {
        if (raw == null) {
            return DEFAULT_PRIORITY;
        }
        String upper = raw.toUpperCase();
        if (!VALID_PRIORITIES.contains(upper)) {
            throw new IllegalArgumentException(
                    "Invalid priority '" + raw + "'. Must be one of: LOW, MEDIUM, HIGH, URGENT");
        }
        return upper;
    }

    private static TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getProjectId(),
                task.getColumnId(),
                task.getCreatedBy(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getSortOrder(),
                task.getStartDate(),
                task.getDueDate(),
                task.getEstimatedHours(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
