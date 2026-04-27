package com.trackify.task.controller;

import com.trackify.auth.application.LocalUserPrincipal;
import com.trackify.task.application.TaskQueryService;
import com.trackify.task.dto.TaskResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Task read endpoint: GET /api/tasks/{taskId} (TASK-052).
 *
 * <p>Kept separate from {@link TaskController} to avoid changing its class-level
 * {@code @RequestMapping("/api/projects")} and breaking the existing POST route.
 * The two controllers are logically part of the same task module; the split is
 * structural only and mirrors how the architecture routes task-level reads
 * ({@code /api/tasks/{id}}) vs project-scoped writes ({@code /api/projects/{id}/tasks}).
 *
 * <p>Controller is thin — authorization and loading are delegated to
 * {@link TaskQueryService}.
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskDetailController {

    private final TaskQueryService taskQueryService;

    public TaskDetailController(TaskQueryService taskQueryService) {
        this.taskQueryService = taskQueryService;
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
}
