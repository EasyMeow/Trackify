package com.trackify.workspace.controller;

import com.trackify.auth.application.LocalUserPrincipal;
import com.trackify.workspace.application.WorkspaceQueryService;
import com.trackify.workspace.dto.WorkspaceResponse;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Exposes workspace listing for the authenticated user (TASK-035).
 *
 * <p>{@code GET /api/workspaces} returns only the workspaces the caller is a
 * member of — no admin/list-all path exists. Authorization is enforced by
 * Spring Security's catch-all {@code .requestMatchers("/api/**").authenticated()}
 * rule in {@link com.trackify.config.SecurityConfig}; no extra annotation is
 * needed here.
 */
@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceController {

    private final WorkspaceQueryService workspaceQueryService;

    public WorkspaceController(WorkspaceQueryService workspaceQueryService) {
        this.workspaceQueryService = workspaceQueryService;
    }

    @GetMapping
    public List<WorkspaceResponse> listWorkspaces(
            @AuthenticationPrincipal LocalUserPrincipal principal) {
        return workspaceQueryService.listForUser(principal.userId());
    }
}
