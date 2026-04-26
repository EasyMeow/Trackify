package com.trackify.workspace.infrastructure;

import com.trackify.workspace.domain.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data repository for {@link Workspace}.
 * Exposes the lookup methods needed by TASK-034 (auto-creation) and slug uniqueness checks.
 */
public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {

    Optional<Workspace> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
