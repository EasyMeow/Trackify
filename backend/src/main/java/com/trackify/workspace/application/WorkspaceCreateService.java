package com.trackify.workspace.application;

import com.trackify.workspace.domain.Workspace;
import com.trackify.workspace.domain.WorkspaceMember;
import com.trackify.workspace.domain.WorkspaceRole;
import com.trackify.workspace.dto.CreateWorkspaceRequest;
import com.trackify.workspace.dto.WorkspaceResponse;
import com.trackify.workspace.infrastructure.WorkspaceMemberRepository;
import com.trackify.workspace.infrastructure.WorkspaceRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Application service for creating additional workspaces on behalf of an
 * authenticated user (TASK-108).
 *
 * <p>Each call creates exactly one {@link Workspace} and one
 * {@link WorkspaceMember} row with role {@link WorkspaceRole#OWNER} in a single
 * transaction. There is no cap on the number of workspaces a user may own.
 *
 * <p>Slug strategy: kebab-case of the name. On collision a 4-character UUID
 * fragment is appended (same approach used by
 * {@link WorkspaceBootstrapService#uniqueSlug(String)}).
 */
@Service
public class WorkspaceCreateService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public WorkspaceCreateService(WorkspaceRepository workspaceRepository,
                                  WorkspaceMemberRepository workspaceMemberRepository) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    /**
     * Creates a new workspace owned by {@code userId}.
     *
     * @param userId  the authenticated caller's UUID
     * @param request validated request body
     * @return the created workspace as a response DTO
     */
    @Transactional
    public WorkspaceResponse create(UUID userId, CreateWorkspaceRequest request) {
        String slug = uniqueSlug(request.name());
        Workspace workspace = new Workspace(request.name(), slug, userId);
        workspace = workspaceRepository.save(workspace);

        WorkspaceMember membership = new WorkspaceMember(workspace.getId(), userId, WorkspaceRole.OWNER);
        workspaceMemberRepository.save(membership);

        return new WorkspaceResponse(workspace.getId(), workspace.getName(), workspace.getSlug());
    }

    // --- private helpers ---

    private String uniqueSlug(String name) {
        String base = WorkspaceBootstrapService.toKebabCase(name);
        if (!workspaceRepository.existsBySlug(base)) {
            return base;
        }
        String fragment = UUID.randomUUID().toString().replace("-", "").substring(0, 4);
        return base + "-" + fragment;
    }
}
