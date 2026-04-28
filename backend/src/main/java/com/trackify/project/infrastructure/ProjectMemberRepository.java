package com.trackify.project.infrastructure;

import com.trackify.project.domain.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data repository for {@link ProjectMember}.
 * Exposes the lookup methods needed by TASK-041 (authz checks) and project membership queries.
 */
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {

    List<ProjectMember> findAllByProjectId(UUID projectId);

    List<ProjectMember> findAllByUserId(UUID userId);

    Optional<ProjectMember> findByProjectIdAndUserId(UUID projectId, UUID userId);

    boolean existsByProjectIdAndUserId(UUID projectId, UUID userId);

    void deleteByProjectId(UUID projectId);
}
