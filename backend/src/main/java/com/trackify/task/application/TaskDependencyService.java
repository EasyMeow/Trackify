package com.trackify.task.application;

import com.trackify.common.exception.ConflictException;
import com.trackify.common.exception.NotFoundException;
import com.trackify.project.application.ProjectQueryService;
import com.trackify.task.domain.Task;
import com.trackify.task.domain.TaskDependency;
import com.trackify.task.dto.CreateDependencyRequest;
import com.trackify.task.dto.DependencyResponse;
import com.trackify.task.infrastructure.TaskDependencyRepository;
import com.trackify.task.infrastructure.TaskRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Application service for dependency edges between tasks (TASK-072 /
 * TASK-089).
 *
 * <p>Authorization mirrors the rest of the task module: load the task whose
 * id appears in the URL, resolve its project, and delegate to
 * {@link ProjectQueryService#getById} which throws {@code NotFoundException}
 * (404) when the project does not exist or {@code ForbiddenException} (403)
 * when the caller is not a workspace member.
 */
@Service
public class TaskDependencyService {

    private final TaskRepository taskRepository;
    private final TaskDependencyRepository taskDependencyRepository;
    private final ProjectQueryService projectQueryService;

    public TaskDependencyService(TaskRepository taskRepository,
                                 TaskDependencyRepository taskDependencyRepository,
                                 ProjectQueryService projectQueryService) {
        this.taskRepository = taskRepository;
        this.taskDependencyRepository = taskDependencyRepository;
        this.projectQueryService = projectQueryService;
    }

    /**
     * Persists a new dependency edge so the task identified by {@code taskId}
     * (the successor) cannot start until {@code request.predecessorTaskId}
     * finishes.
     *
     * <p>Validation order:
     * <ol>
     *   <li>Load the successor task — 404 if missing.</li>
     *   <li>Verify caller can access the successor's project (404/403).</li>
     *   <li>Load the predecessor task — 404 if missing.</li>
     *   <li>Reject self-loops with {@link IllegalArgumentException} (400).</li>
     *   <li>Reject cross-project edges with {@link IllegalArgumentException}
     *       (400) — the task module's invariant relied on by the timeline
     *       payload.</li>
     *   <li>Reject duplicate edges with {@link ConflictException} (409).</li>
     *   <li>Reject edges that would close a cycle with
     *       {@link ConflictException} (409).</li>
     * </ol>
     */
    @Transactional
    public DependencyResponse create(UUID taskId, UUID userId, CreateDependencyRequest request) {
        UUID predecessorTaskId = request.predecessorTaskId();

        Task successor = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        projectQueryService.getById(successor.getProjectId(), userId);

        if (predecessorTaskId.equals(taskId)) {
            throw new IllegalArgumentException("A task cannot depend on itself");
        }

        Task predecessor = taskRepository.findById(predecessorTaskId)
                .orElseThrow(() -> new NotFoundException("Predecessor task not found"));

        if (!predecessor.getProjectId().equals(successor.getProjectId())) {
            throw new IllegalArgumentException(
                    "Predecessor task must belong to the same project as the successor");
        }

        if (taskDependencyRepository.existsByPredecessorTaskIdAndSuccessorTaskId(
                predecessorTaskId, taskId)) {
            throw new ConflictException("Dependency already exists");
        }

        if (wouldCreateCycle(predecessorTaskId, taskId)) {
            throw new ConflictException("Dependency would create a cycle");
        }

        TaskDependency saved = taskDependencyRepository.save(
                new TaskDependency(predecessorTaskId, taskId));

        return new DependencyResponse(
                saved.getId(),
                saved.getPredecessorTaskId(),
                saved.getSuccessorTaskId(),
                saved.getCreatedAt()
        );
    }

    /**
     * Deletes a dependency edge that hangs off the given successor task.
     *
     * <p>The dependency is required to belong to the task in the URL (i.e.
     * {@code edge.successorTaskId == taskId}); a mismatch surfaces as 404 so
     * the URL keeps the "edge under task X" mental model.
     */
    @Transactional
    public void delete(UUID taskId, UUID dependencyId, UUID userId) {
        TaskDependency edge = taskDependencyRepository.findById(dependencyId)
                .orElseThrow(() -> new NotFoundException("Dependency not found"));

        if (!edge.getSuccessorTaskId().equals(taskId)) {
            throw new NotFoundException("Dependency not found");
        }

        Task successor = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        projectQueryService.getById(successor.getProjectId(), userId);

        taskDependencyRepository.delete(edge);
    }

    /**
     * Returns true when adding the edge {@code predecessor -> successor} would
     * close a directed cycle. A cycle exists when {@code predecessor} is
     * already a transitive successor of {@code successor} (i.e. there is a
     * path {@code successor -> ... -> predecessor}).
     */
    private boolean wouldCreateCycle(UUID predecessorTaskId, UUID successorTaskId) {
        Set<UUID> visited = new HashSet<>();
        Deque<UUID> frontier = new ArrayDeque<>();
        frontier.add(successorTaskId);

        while (!frontier.isEmpty()) {
            UUID current = frontier.poll();
            if (!visited.add(current)) {
                continue;
            }
            List<TaskDependency> outgoing =
                    taskDependencyRepository.findByPredecessorTaskId(current);
            for (TaskDependency edge : outgoing) {
                UUID next = edge.getSuccessorTaskId();
                if (next.equals(predecessorTaskId)) {
                    return true;
                }
                if (!visited.contains(next)) {
                    frontier.add(next);
                }
            }
        }
        return false;
    }
}
