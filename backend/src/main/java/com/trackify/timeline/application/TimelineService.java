package com.trackify.timeline.application;

import com.trackify.common.exception.ForbiddenException;
import com.trackify.common.exception.NotFoundException;
import com.trackify.project.application.ProjectQueryService;
import com.trackify.task.domain.Task;
import com.trackify.task.infrastructure.TaskRepository;
import com.trackify.timeline.dto.TimelineResponse;
import com.trackify.timeline.dto.TimelineTaskResponse;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Application service for the timeline read model (TASK-067).
 *
 * <p>Loads all tasks belonging to the project and maps them to the Gantt-shaped
 * payload. Authorization is delegated to {@link ProjectQueryService#getById},
 * the same check used by the board read endpoint — a missing project surfaces
 * as 404, a non-member as 403.
 *
 * <p>This service is read-only. It never mutates tasks. Dependency edges are
 * deferred to TASK-073.
 */
@Service
public class TimelineService {

    private final ProjectQueryService projectQueryService;
    private final TaskRepository taskRepository;

    public TimelineService(ProjectQueryService projectQueryService,
                           TaskRepository taskRepository) {
        this.projectQueryService = projectQueryService;
        this.taskRepository = taskRepository;
    }

    /**
     * Returns the Gantt timeline payload for the given project.
     *
     * @param projectId target project UUID
     * @param userId    authenticated caller's UUID
     * @return timeline DTO with ordered task rows
     * @throws NotFoundException  if the project does not exist
     * @throws ForbiddenException if the caller is not a member of the project's workspace
     */
    @Transactional(readOnly = true)
    public TimelineResponse getTimeline(UUID projectId, UUID userId) {
        // Authorization: reuses the existing project access check.
        // Throws NotFoundException (404) or ForbiddenException (403) as appropriate.
        projectQueryService.getById(projectId, userId);

        List<Task> tasks = taskRepository.findByProjectIdOrderByColumnIdAscSortOrderAsc(projectId);

        List<TimelineTaskResponse> taskResponses = tasks.stream()
                .map(TimelineService::toTaskResponse)
                .toList();

        return new TimelineResponse(projectId, taskResponses);
    }

    private static TimelineTaskResponse toTaskResponse(Task task) {
        return new TimelineTaskResponse(
                task.getId(),
                task.getTitle(),
                task.getStatus(),
                task.getPriority(),
                task.getStartDate(),
                task.getDueDate()
        );
    }
}
