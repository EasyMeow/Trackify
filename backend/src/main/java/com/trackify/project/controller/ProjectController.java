package com.trackify.project.controller;

import com.trackify.auth.application.LocalUserPrincipal;
import com.trackify.project.application.ProjectCreateService;
import com.trackify.project.application.ProjectQueryService;
import com.trackify.project.dto.ProjectCreateRequest;
import com.trackify.project.dto.ProjectResponse;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Project collection endpoints (TASK-039, TASK-041).
 *
 * <p>{@code GET /api/workspaces/{workspaceId}/projects} — list projects in a workspace.
 * <p>{@code POST /api/workspaces/{workspaceId}/projects} — create a new project.
 *
 * <p>Membership checks and business logic live in the application services.
 * The catch-all {@code .requestMatchers("/api/**").authenticated()} rule in
 * {@link com.trackify.config.SecurityConfig} guarantees the caller is signed in.
 */
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/projects")
public class ProjectController {

    private final ProjectQueryService projectQueryService;
    private final ProjectCreateService projectCreateService;

    public ProjectController(ProjectQueryService projectQueryService,
                             ProjectCreateService projectCreateService) {
        this.projectQueryService = projectQueryService;
        this.projectCreateService = projectCreateService;
    }

    @GetMapping
    public List<ProjectResponse> listProjects(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal LocalUserPrincipal principal) {
        return projectQueryService.listForWorkspace(workspaceId, principal.userId());
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody ProjectCreateRequest request,
            @AuthenticationPrincipal LocalUserPrincipal principal) {
        ProjectResponse response = projectCreateService.create(workspaceId, principal.userId(), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }
}
