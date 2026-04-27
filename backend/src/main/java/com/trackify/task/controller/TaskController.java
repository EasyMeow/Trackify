package com.trackify.task.controller;

import com.trackify.auth.application.LocalUserPrincipal;
import com.trackify.task.application.TaskCommandService;
import com.trackify.task.dto.CreateTaskRequest;
import com.trackify.task.dto.TaskResponse;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Task write endpoints (TASK-050).
 *
 * <p>Thin controller: validates the request via Bean Validation, resolves the
 * principal, and delegates to {@link TaskCommandService}. No business logic here.
 *
 * <p>Authorization and column/sort-order resolution happen in the service layer.
 */
@RestController
@RequestMapping("/api/projects")
public class TaskController {

    private final TaskCommandService taskCommandService;

    public TaskController(TaskCommandService taskCommandService) {
        this.taskCommandService = taskCommandService;
    }

    /**
     * Creates a task in the default (first) board column of the given project.
     *
     * @param projectId target project UUID (path variable)
     * @param request   validated request body
     * @param principal authenticated user
     * @return HTTP 201 with the created task body
     */
    @PostMapping("/{projectId}/tasks")
    public ResponseEntity<TaskResponse> createTask(
            @PathVariable UUID projectId,
            @RequestBody @Valid CreateTaskRequest request,
            @AuthenticationPrincipal LocalUserPrincipal principal) {
        TaskResponse response = taskCommandService.createTask(projectId, principal.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
