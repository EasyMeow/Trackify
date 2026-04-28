package com.trackify.task.controller;

import com.trackify.auth.application.LocalUserPrincipal;
import com.trackify.task.application.TaskDependencyService;
import com.trackify.task.dto.CreateDependencyRequest;
import com.trackify.task.dto.DependencyResponse;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Task-dependency write endpoints (TASK-072 / TASK-089):
 * {@code POST /api/tasks/{taskId}/dependencies} and
 * {@code DELETE /api/tasks/{taskId}/dependencies/{dependencyId}}.
 *
 * <p>Thin controller — authorization, cycle detection, and persistence live
 * in {@link TaskDependencyService}.
 */
@RestController
@RequestMapping("/api/tasks/{taskId}/dependencies")
public class DependencyController {

    private final TaskDependencyService taskDependencyService;

    public DependencyController(TaskDependencyService taskDependencyService) {
        this.taskDependencyService = taskDependencyService;
    }

    @PostMapping
    public ResponseEntity<DependencyResponse> create(
            @PathVariable UUID taskId,
            @RequestBody @Valid CreateDependencyRequest request,
            @AuthenticationPrincipal LocalUserPrincipal principal) {
        DependencyResponse response = taskDependencyService.create(taskId, principal.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{dependencyId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID taskId,
            @PathVariable UUID dependencyId,
            @AuthenticationPrincipal LocalUserPrincipal principal) {
        taskDependencyService.delete(taskId, dependencyId, principal.userId());
        return ResponseEntity.noContent().build();
    }
}
