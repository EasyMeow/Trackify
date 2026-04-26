package com.trackify.workspace.application;

import com.trackify.workspace.domain.Workspace;
import com.trackify.workspace.domain.WorkspaceMember;
import com.trackify.workspace.domain.WorkspaceRole;
import com.trackify.workspace.infrastructure.WorkspaceMemberRepository;
import com.trackify.workspace.infrastructure.WorkspaceRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Ensures that a user who has just signed in owns at least one workspace
 * (TASK-034). Idempotent: if the user already has any workspace membership
 * the method returns immediately without writing any rows. This covers the
 * "second login does not create a duplicate" requirement.
 *
 * <p>Slug strategy: kebab-case of the login (e.g. {@code alice-smith} for
 * login {@code alice_smith}). On collision (another workspace already owns
 * that slug) a short UUID fragment is appended: {@code alice-7f3a}. The
 * workspace name is {@code "{login}'s workspace"}.
 */
@Service
public class WorkspaceBootstrapService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public WorkspaceBootstrapService(WorkspaceRepository workspaceRepository,
                                     WorkspaceMemberRepository workspaceMemberRepository) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    /**
     * Creates a personal workspace and an OWNER membership for {@code userId}
     * if no membership rows exist for that user yet.
     *
     * @param userId the UUID of the authenticated user
     * @param login  the username, used to derive a slug and workspace name
     */
    @Transactional
    public void ensurePersonalWorkspace(UUID userId, String login) {
        List<WorkspaceMember> existing = workspaceMemberRepository.findAllByUserId(userId);
        if (!existing.isEmpty()) {
            return;
        }

        String slug = uniqueSlug(login);
        String name = login + "'s workspace";

        Workspace workspace = new Workspace(name, slug, userId);
        workspace = workspaceRepository.save(workspace);

        WorkspaceMember membership = new WorkspaceMember(workspace.getId(), userId, WorkspaceRole.OWNER);
        workspaceMemberRepository.save(membership);
    }

    // --- private helpers ---

    private String uniqueSlug(String login) {
        String base = toKebabCase(login);
        if (!workspaceRepository.existsBySlug(base)) {
            return base;
        }
        // Append a 4-character UUID fragment to resolve collision.
        String fragment = UUID.randomUUID().toString().replace("-", "").substring(0, 4);
        return base + "-" + fragment;
    }

    /**
     * Converts a login string to a URL-safe kebab-case slug.
     * Non-alphanumeric characters are replaced with hyphens; consecutive
     * hyphens are collapsed; leading/trailing hyphens are removed.
     */
    static String toKebabCase(String login) {
        return login
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }
}
