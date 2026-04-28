package com.trackify.project.application;

import com.trackify.common.exception.ForbiddenException;
import com.trackify.common.exception.NotFoundException;
import com.trackify.project.domain.Project;
import com.trackify.project.dto.ProjectPatchRequest;
import com.trackify.project.dto.ProjectResponse;
import com.trackify.project.infrastructure.ProjectRepository;
import com.trackify.workspace.infrastructure.WorkspaceMemberRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Application service for partial project updates (TASK-094).
 */
@Service
public class ProjectUpdateService {

    private final ProjectRepository projectRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public ProjectUpdateService(ProjectRepository projectRepository,
                                WorkspaceMemberRepository workspaceMemberRepository) {
        this.projectRepository = projectRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    /**
     * Applies a partial update to a project.
     *
     * @param projectId target project UUID
     * @param userId    authenticated caller's UUID
     * @param request   fields to update; null fields are left unchanged
     * @return the updated project as a response DTO
     * @throws NotFoundException  if no project row with that id exists
     * @throws ForbiddenException if the caller is not a member of the project's workspace
     */
    @Transactional
    public ProjectResponse patch(UUID projectId, UUID userId, ProjectPatchRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));

        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(project.getWorkspaceId(), userId)) {
            throw new ForbiddenException("Project not accessible");
        }

        if (request.name() != null) {
            project.setName(request.name());
        }
        if (request.description() != null) {
            project.setDescription(request.description());
        }

        Project saved = projectRepository.save(project);
        return toResponse(saved);
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
