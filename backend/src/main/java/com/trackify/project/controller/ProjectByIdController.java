package com.trackify.project.controller;

import com.trackify.auth.application.LocalUserPrincipal;
import com.trackify.project.application.ProjectDeleteService;
import com.trackify.project.application.ProjectQueryService;
import com.trackify.project.application.ProjectUpdateService;
import com.trackify.project.dto.ProjectPatchRequest;
import com.trackify.project.dto.ProjectResponse;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Project-by-id read endpoint (TASK-040).
 *
 * <p>{@code GET /api/projects/{projectId}} returns the single project
 * the authenticated caller is allowed to see. The lookup is project-keyed
 * so missing and forbidden cases surface as distinct 404/403 statuses —
 * see {@link ProjectQueryService#getById(UUID, UUID)} for the semantics.
 *
 * <p>Kept as a separate class from {@link ProjectController} because that
 * controller is rooted at {@code /api/workspaces/{workspaceId}/projects} and
 * the two path prefixes cannot be unified under one {@code @RequestMapping}.
 */
@RestController
@RequestMapping("/api/projects")
public class ProjectByIdController {

    private final ProjectQueryService projectQueryService;
    private final ProjectUpdateService projectUpdateService;
    private final ProjectDeleteService projectDeleteService;

    public ProjectByIdController(ProjectQueryService projectQueryService,
                                 ProjectUpdateService projectUpdateService,
                                 ProjectDeleteService projectDeleteService) {
        this.projectQueryService = projectQueryService;
        this.projectUpdateService = projectUpdateService;
        this.projectDeleteService = projectDeleteService;
    }

    @GetMapping("/{projectId}")
    public ProjectResponse getProject(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal LocalUserPrincipal principal) {
        return projectQueryService.getById(projectId, principal.userId());
    }

    @PatchMapping("/{projectId}")
    public ProjectResponse patchProject(
            @PathVariable UUID projectId,
            @Valid @RequestBody ProjectPatchRequest request,
            @AuthenticationPrincipal LocalUserPrincipal principal) {
        return projectUpdateService.patch(projectId, principal.userId(), request);
    }

    @DeleteMapping("/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProject(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal LocalUserPrincipal principal) {
        projectDeleteService.delete(projectId, principal.userId());
    }
}
