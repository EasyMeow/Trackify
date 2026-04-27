package com.trackify.task.controller;

import com.trackify.auth.application.LocalUserPrincipal;
import com.trackify.task.application.TaskCommandService;
import com.trackify.task.application.TaskQueryService;
import com.trackify.task.dto.MoveTaskRequest;
import com.trackify.task.dto.ScheduleTaskRequest;
import com.trackify.task.dto.TaskResponse;
import com.trackify.task.dto.UpdateTaskRequest;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Task-level read and content-update endpoints: GET and PATCH /api/tasks/{taskId}.
 *
 * <p>GET added in TASK-052; PATCH added in TASK-055.
 *
 * <p>Kept separate from {@link TaskController} to avoid changing its class-level
 * {@code @RequestMapping("/api/projects")} and breaking the existing POST route.
 * The two controllers are logically part of the same task module; the split is
 * structural only and mirrors how the architecture routes task-level operations
 * ({@code /api/tasks/{id}}) vs project-scoped writes ({@code /api/projects/{id}/tasks}).
 *
 * <p>Controllers are thin — authorization and business logic are delegated to
 * {@link TaskQueryService} and {@link TaskCommandService}.
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskDetailController {

    private final TaskQueryService taskQueryService;
    private final TaskCommandService taskCommandService;

    public TaskDetailController(TaskQueryService taskQueryService, TaskCommandService taskCommandService) {
        this.taskQueryService = taskQueryService;
        this.taskCommandService = taskCommandService;
    }

    /**
     * Returns the full detail for a single task the caller can access.
     *
     * @param taskId    UUID of the task
     * @param principal authenticated caller
     * @return HTTP 200 with a {@link TaskResponse} body
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponse> getTask(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal LocalUserPrincipal principal) {
        TaskResponse response = taskQueryService.getById(taskId, principal.userId());
        return ResponseEntity.ok(response);
    }

    /**
     * Partially updates content fields of a task (TASK-055).
     *
     * <p>Only non-null fields in the request body are applied; omitted fields
     * are left unchanged. Column and position are NOT mutable here — use
     * {@code PATCH /api/tasks/{taskId}/move} (TASK-058/059) for that.
     *
     * @param taskId    UUID of the task to update
     * @param request   partial update payload; any absent/null field means "keep existing"
     * @param principal authenticated caller
     * @return HTTP 200 with the updated {@link TaskResponse} body
     */
    @PatchMapping("/{taskId}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable UUID taskId,
            @RequestBody @Valid UpdateTaskRequest request,
            @AuthenticationPrincipal LocalUserPrincipal principal) {
        TaskResponse response = taskCommandService.updateTask(taskId, principal.userId(), request);
        return ResponseEntity.ok(response);
    }

    /**
     * Moves a task to a new column and/or sort position (TASK-059).
     *
     * <p>Backs Kanban drag-and-drop. Both within-column reorder and cross-column
     * moves go through this single endpoint. The body must include both
     * {@code columnId} and {@code sortOrder}; the client is expected to compute
     * the fractional sort position (e.g. midpoint between neighbors).
     *
     * @param taskId    UUID of the task being moved
     * @param request   validated move payload
     * @param principal authenticated caller
     * @return HTTP 200 with the updated {@link TaskResponse} body
     */
    @PatchMapping("/{taskId}/move")
    public ResponseEntity<TaskResponse> moveTask(
            @PathVariable UUID taskId,
            @RequestBody @Valid MoveTaskRequest request,
            @AuthenticationPrincipal LocalUserPrincipal principal) {
        TaskResponse response = taskCommandService.moveTask(taskId, principal.userId(), request);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates only the schedule fields ({@code startDate}, {@code dueDate}) of a
     * task (TASK-070).
     *
     * <p>Backs the Gantt timeline drag/resize flow. Both fields in the body are
     * optional; null means "keep existing". Kept distinct from the generic
     * {@code PATCH /api/tasks/{taskId}} so the timeline UI's mutation has a
     * scope it cannot accidentally overshoot (no title/description/priority
     * changes can leak through).
     *
     * @param taskId    UUID of the task being rescheduled
     * @param request   validated schedule payload
     * @param principal authenticated caller
     * @return HTTP 200 with the updated {@link TaskResponse} body
     */
    @PatchMapping("/{taskId}/schedule")
    public ResponseEntity<TaskResponse> scheduleTask(
            @PathVariable UUID taskId,
            @RequestBody @Valid ScheduleTaskRequest request,
            @AuthenticationPrincipal LocalUserPrincipal principal) {
        TaskResponse response = taskCommandService.scheduleTask(taskId, principal.userId(), request);
        return ResponseEntity.ok(response);
    }
}
