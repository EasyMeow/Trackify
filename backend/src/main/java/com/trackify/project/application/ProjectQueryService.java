package com.trackify.project.application;

import com.trackify.common.exception.ForbiddenException;
import com.trackify.project.domain.Project;
import com.trackify.project.dto.ProjectResponse;
import com.trackify.project.infrastructure.ProjectRepository;
import com.trackify.workspace.infrastructure.WorkspaceMemberRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Application service for project read queries (TASK-039).
 *
 * <p>Authorization is enforced by checking workspace membership before any
 * project rows are returned. A non-member receives 403 (mapped from
 * {@link ForbiddenException}) rather than 404 — workspaces are first-class
 * resources, and surfacing a distinct forbidden status here gives TASK-077 a
 * clean place to standardize the envelope.
 */
@Service
public class ProjectQueryService {

    private final ProjectRepository projectRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public ProjectQueryService(ProjectRepository projectRepository,
                               WorkspaceMemberRepository workspaceMemberRepository) {
        this.projectRepository = projectRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    /**
     * Returns the projects in a workspace the caller belongs to, ordered by name.
     *
     * @param workspaceId target workspace UUID
     * @param userId      authenticated user's UUID
     * @return projects in the workspace, possibly empty
     * @throws ForbiddenException if the caller is not a member of the workspace
     */
    @Transactional(readOnly = true)
    public List<ProjectResponse> listForWorkspace(UUID workspaceId, UUID userId) {
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new ForbiddenException("Workspace not accessible");
        }

        return projectRepository.findAllByWorkspaceId(workspaceId)
                .stream()
                .sorted(Comparator.comparing(Project::getName))
                .map(ProjectQueryService::toResponse)
                .toList();
    }

    private static ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getWorkspaceId(),
                project.getName(),
                project.getSlug(),
                project.getDescription(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
