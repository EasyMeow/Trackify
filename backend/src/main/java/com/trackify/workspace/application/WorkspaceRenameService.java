package com.trackify.workspace.application;

import com.trackify.common.exception.ForbiddenException;
import com.trackify.common.exception.NotFoundException;
import com.trackify.workspace.domain.Workspace;
import com.trackify.workspace.domain.WorkspaceRole;
import com.trackify.workspace.dto.RenameWorkspaceRequest;
import com.trackify.workspace.dto.WorkspaceResponse;
import com.trackify.workspace.infrastructure.WorkspaceMemberRepository;
import com.trackify.workspace.infrastructure.WorkspaceRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Application service for renaming a workspace (TASK-109).
 *
 * <p>Only the workspace owner may rename it. Non-members receive 403; missing
 * workspaces receive 404.
 */
@Service
public class WorkspaceRenameService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;

    public WorkspaceRenameService(WorkspaceRepository workspaceRepository,
                                  WorkspaceMemberRepository memberRepository) {
        this.workspaceRepository = workspaceRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional
    public WorkspaceResponse rename(UUID workspaceId, UUID userId, RenameWorkspaceRequest request) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new NotFoundException("Workspace not found: " + workspaceId));

        if (!memberRepository.existsByWorkspaceIdAndUserIdAndRole(workspaceId, userId, WorkspaceRole.OWNER)) {
            throw new ForbiddenException("Only workspace owners may rename the workspace.");
        }

        workspace.setName(request.name());
        workspace = workspaceRepository.save(workspace);
        return new WorkspaceResponse(workspace.getId(), workspace.getName(), workspace.getSlug());
    }
}
