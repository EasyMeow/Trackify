package com.trackify.project.infrastructure;

import com.trackify.project.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data repository for {@link Project}.
 * Exposes the lookup methods needed by TASK-039 (list projects) and slug uniqueness checks.
 */
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    List<Project> findAllByWorkspaceId(UUID workspaceId);

    Optional<Project> findByWorkspaceIdAndSlug(UUID workspaceId, String slug);

    boolean existsByWorkspaceIdAndSlug(UUID workspaceId, String slug);
}
