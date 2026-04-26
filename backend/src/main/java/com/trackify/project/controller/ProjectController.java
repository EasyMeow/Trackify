package com.trackify.project.controller;

import com.trackify.auth.application.LocalUserPrincipal;
import com.trackify.project.application.ProjectQueryService;
import com.trackify.project.dto.ProjectResponse;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Project read endpoints (TASK-039).
 *
 * <p>{@code GET /api/workspaces/{workspaceId}/projects} returns the projects in
 * a workspace the authenticated caller belongs to. Membership is checked by
 * {@link ProjectQueryService}; the catch-all
 * {@code .requestMatchers("/api/**").authenticated()} rule in
 * {@link com.trackify.config.SecurityConfig} guarantees the caller is signed in.
 */
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/projects")
public class ProjectController {

    private final ProjectQueryService projectQueryService;

    public ProjectController(ProjectQueryService projectQueryService) {
        this.projectQueryService = projectQueryService;
    }

    @GetMapping
    public List<ProjectResponse> listProjects(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal LocalUserPrincipal principal) {
        return projectQueryService.listForWorkspace(workspaceId, principal.userId());
    }
}
