package com.trackify.workspace.application;

import com.trackify.workspace.domain.Workspace;
import com.trackify.workspace.domain.WorkspaceMember;
import com.trackify.workspace.dto.WorkspaceResponse;
import com.trackify.workspace.infrastructure.WorkspaceMemberRepository;
import com.trackify.workspace.infrastructure.WorkspaceRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Application service for workspace read queries (TASK-035).
 *
 * <p>Uses a two-step lookup: fetch the user's membership rows, then bulk-load
 * the corresponding workspace rows by ID. This avoids the need for a JPQL join
 * query (which would require either a {@code @ManyToOne} or native SQL) while
 * remaining correct for the expected small cardinality of workspaces per user.
 */
@Service
public class WorkspaceQueryService {

    private final WorkspaceMemberRepository memberRepository;
    private final WorkspaceRepository workspaceRepository;

    public WorkspaceQueryService(WorkspaceMemberRepository memberRepository,
                                 WorkspaceRepository workspaceRepository) {
        this.memberRepository = memberRepository;
        this.workspaceRepository = workspaceRepository;
    }

    /**
     * Returns all workspaces the given user is a member of, ordered by name.
     *
     * @param userId the authenticated user's UUID
     * @return list of workspace response DTOs, may be empty
     */
    @Transactional(readOnly = true)
    public List<WorkspaceResponse> listForUser(UUID userId) {
        List<UUID> workspaceIds = memberRepository.findAllByUserId(userId)
                .stream()
                .map(WorkspaceMember::getWorkspaceId)
                .toList();

        if (workspaceIds.isEmpty()) {
            return List.of();
        }

        return workspaceRepository.findAllById(workspaceIds)
                .stream()
                .sorted(java.util.Comparator.comparing(Workspace::getName))
                .map(ws -> new WorkspaceResponse(ws.getId(), ws.getName(), ws.getSlug()))
                .toList();
    }
}
