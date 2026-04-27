package com.trackify.task.application;

import com.trackify.common.exception.NotFoundException;
import com.trackify.project.application.ProjectQueryService;
import com.trackify.task.domain.Task;
import com.trackify.task.dto.TaskResponse;
import com.trackify.task.infrastructure.TaskRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Application service for task read queries (TASK-052).
 *
 * <p>Authorization follows the same pattern as {@link TaskCommandService}: load
 * the task, then delegate to {@link ProjectQueryService#getById} which throws
 * {@link com.trackify.common.exception.NotFoundException} (404) when the project
 * does not exist and {@link com.trackify.common.exception.ForbiddenException}
 * (403) when the caller is not a workspace member.
 */
@Service
@Transactional(readOnly = true)
public class TaskQueryService {

    private final TaskRepository taskRepository;
    private final ProjectQueryService projectQueryService;

    public TaskQueryService(TaskRepository taskRepository, ProjectQueryService projectQueryService) {
        this.taskRepository = taskRepository;
        this.projectQueryService = projectQueryService;
    }

    /**
     * Returns a single task the caller is authorized to read.
     *
     * <p>Authorization: loads the task's project via {@link ProjectQueryService#getById},
     * which enforces workspace membership. The task's {@code project_id} is used as the
     * authorization key, keeping the same semantics as task-write operations.
     *
     * @param taskId the UUID of the task to fetch
     * @param userId the authenticated caller's UUID
     * @return the task as a {@link TaskResponse}
     * @throws NotFoundException  if no task row exists with that id
     * @throws com.trackify.common.exception.ForbiddenException if the caller is not a
     *         member of the task's project workspace
     */
    public TaskResponse getById(UUID taskId, UUID userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));

        // Delegate authorization to project query; throws 404/403 as appropriate.
        projectQueryService.getById(task.getProjectId(), userId);

        return toResponse(task);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

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
