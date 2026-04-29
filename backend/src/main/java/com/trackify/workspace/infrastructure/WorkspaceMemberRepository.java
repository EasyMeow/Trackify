package com.trackify.workspace.infrastructure;

import com.trackify.workspace.domain.WorkspaceMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data repository for {@link WorkspaceMember}.
 * Exposes the lookup methods needed by TASK-035 (list workspaces) and TASK-076 (authz checks).
 */
public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {

    List<WorkspaceMember> findAllByUserId(UUID userId);

    boolean existsByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);

    boolean existsByWorkspaceIdAndUserIdAndRole(UUID workspaceId, UUID userId,
            com.trackify.workspace.domain.WorkspaceRole role);
}
