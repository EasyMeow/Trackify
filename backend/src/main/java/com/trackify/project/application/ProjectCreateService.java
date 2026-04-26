package com.trackify.project.application;

import com.trackify.common.exception.ConflictException;
import com.trackify.common.exception.ForbiddenException;
import com.trackify.project.domain.Project;
import com.trackify.project.dto.ProjectCreateRequest;
import com.trackify.project.dto.ProjectResponse;
import com.trackify.project.infrastructure.ProjectRepository;
import com.trackify.workspace.infrastructure.WorkspaceMemberRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Application service for creating projects (TASK-041).
 *
 * <p>Split from {@link ProjectQueryService} to follow the read/write separation
 * established by {@code UserRegistrationService} in the auth module. A class
 * named "QueryService" that also mutates state is misleading and harder to test.
 *
 * <p>Authorization: the caller must be a workspace member (same gate as the list
 * endpoint). Non-members receive 403 via {@link ForbiddenException}.
 *
 * <p>Slug: derived from the project name when not supplied (lowercase, whitespace
 * collapsed and replaced with hyphens, non-alphanumeric/hyphen chars stripped).
 * Uniqueness is pre-checked within the workspace — a collision surfaces as 409
 * rather than a raw DB constraint violation (which would surface as 500).
 */
@Service
public class ProjectCreateService {

    private final ProjectRepository projectRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public ProjectCreateService(ProjectRepository projectRepository,
                                WorkspaceMemberRepository workspaceMemberRepository) {
        this.projectRepository = projectRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    /**
     * Creates a new project inside the given workspace.
     *
     * @param workspaceId target workspace UUID (from URL path variable)
     * @param userId      authenticated caller's UUID
     * @param request     validated request body
     * @return the created project as a response DTO
     * @throws ForbiddenException if the caller is not a member of the workspace
     * @throws ConflictException  if a project with the same slug already exists in the workspace
     */
    @Transactional
    public ProjectResponse create(UUID workspaceId, UUID userId, ProjectCreateRequest request) {
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new ForbiddenException("Workspace not accessible");
        }

        String slug = deriveSlug(request.slug(), request.name());

        if (projectRepository.existsByWorkspaceIdAndSlug(workspaceId, slug)) {
            throw new ConflictException("A project with slug '" + slug + "' already exists in this workspace");
        }

        Project project = new Project(workspaceId, userId, request.name(), slug, request.description());
        Project saved = projectRepository.save(project);
        return toResponse(saved);
    }

    /**
     * Returns the supplied slug if non-blank, otherwise derives one from the name.
     * Derivation: lowercase, collapse whitespace, replace with hyphens, strip
     * characters that are neither alphanumeric nor hyphens.
     */
    static String deriveSlug(String suppliedSlug, String name) {
        if (suppliedSlug != null && !suppliedSlug.isBlank()) {
            return suppliedSlug.trim();
        }
        return name.trim()
                .toLowerCase()
                .replaceAll("\\s+", "-")
                .replaceAll("[^a-z0-9-]", "");
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
