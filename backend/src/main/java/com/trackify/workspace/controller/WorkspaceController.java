package com.trackify.workspace.controller;

import com.trackify.auth.application.LocalUserPrincipal;
import com.trackify.workspace.application.WorkspaceCreateService;
import com.trackify.workspace.application.WorkspaceQueryService;
import com.trackify.workspace.application.WorkspaceRenameService;
import com.trackify.workspace.dto.CreateWorkspaceRequest;
import com.trackify.workspace.dto.RenameWorkspaceRequest;
import com.trackify.workspace.dto.WorkspaceResponse;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
 * Workspace collection endpoints (TASK-035, TASK-108).
 *
 * <p>{@code GET /api/workspaces} — returns only the workspaces the caller is a
 * member of (TASK-035).
 *
 * <p>{@code POST /api/workspaces} — creates a new workspace owned by the caller
 * (TASK-108). Returns 201 with a {@code Location} header and the new workspace
 * payload.
 *
 * <p>{@code PATCH /api/workspaces/{id}} — renames a workspace for owners only
 * (TASK-109). Returns 200 with the updated workspace payload.
 *
 * <p>Authorization is enforced by Spring Security's catch-all
 * {@code .requestMatchers("/api/**").authenticated()} rule in
 * {@link com.trackify.config.SecurityConfig}; no extra annotation is needed here.
 */
@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceController {

    private final WorkspaceQueryService workspaceQueryService;
    private final WorkspaceCreateService workspaceCreateService;
    private final WorkspaceRenameService workspaceRenameService;

    public WorkspaceController(WorkspaceQueryService workspaceQueryService,
                               WorkspaceCreateService workspaceCreateService,
                               WorkspaceRenameService workspaceRenameService) {
        this.workspaceQueryService = workspaceQueryService;
        this.workspaceCreateService = workspaceCreateService;
        this.workspaceRenameService = workspaceRenameService;
    }

    @GetMapping
    public List<WorkspaceResponse> listWorkspaces(
            @AuthenticationPrincipal LocalUserPrincipal principal) {
        return workspaceQueryService.listForUser(principal.userId());
    }

    @PostMapping
    public ResponseEntity<WorkspaceResponse> createWorkspace(
            @Valid @RequestBody CreateWorkspaceRequest request,
            @AuthenticationPrincipal LocalUserPrincipal principal) {
        WorkspaceResponse response = workspaceCreateService.create(principal.userId(), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PatchMapping("/{id}")
    public WorkspaceResponse renameWorkspace(
            @PathVariable UUID id,
            @Valid @RequestBody RenameWorkspaceRequest request,
            @AuthenticationPrincipal LocalUserPrincipal principal) {
        return workspaceRenameService.rename(id, principal.userId(), request);
    }
}
