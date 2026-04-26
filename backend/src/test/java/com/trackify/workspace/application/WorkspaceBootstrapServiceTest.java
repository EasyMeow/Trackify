package com.trackify.workspace.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trackify.workspace.domain.Workspace;
import com.trackify.workspace.domain.WorkspaceMember;
import com.trackify.workspace.domain.WorkspaceRole;
import com.trackify.workspace.infrastructure.WorkspaceMemberRepository;
import com.trackify.workspace.infrastructure.WorkspaceRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

/**
 * Proves the TASK-034 workspace-bootstrap contract:
 * <ol>
 *   <li>First login (user has zero memberships) creates exactly one workspace and
 *       one OWNER membership row.</li>
 *   <li>Second login (user already has a membership) is a no-op — no new rows.</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class WorkspaceBootstrapServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    private WorkspaceBootstrapService service;

    @BeforeEach
    void setUp() {
        service = new WorkspaceBootstrapService(workspaceRepository, workspaceMemberRepository);
    }

    @Test
    void firstLoginCreatesExactlyOneWorkspaceAndOneOwnerMembership() {
        UUID userId = UUID.randomUUID();
        String login = "alice";

        // No memberships exist yet (first login).
        when(workspaceMemberRepository.findAllByUserId(userId)).thenReturn(List.of());
        when(workspaceRepository.existsBySlug("alice")).thenReturn(false);

        // Stub save(Workspace) so getId() is non-null when we capture the member.
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(invocation -> {
            Workspace ws = invocation.getArgument(0);
            ReflectionTestUtils.setField(ws, "id", UUID.randomUUID());
            return ws;
        });
        when(workspaceMemberRepository.save(any(WorkspaceMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.ensurePersonalWorkspace(userId, login);

        // Exactly one workspace persisted.
        ArgumentCaptor<Workspace> wsCaptor = ArgumentCaptor.forClass(Workspace.class);
        verify(workspaceRepository).save(wsCaptor.capture());
        Workspace savedWs = wsCaptor.getValue();
        assertThat(savedWs.getName()).isEqualTo("alice's workspace");
        assertThat(savedWs.getSlug()).isEqualTo("alice");
        assertThat(savedWs.getOwnerId()).isEqualTo(userId);

        // Exactly one membership persisted with OWNER role.
        ArgumentCaptor<WorkspaceMember> memberCaptor = ArgumentCaptor.forClass(WorkspaceMember.class);
        verify(workspaceMemberRepository).save(memberCaptor.capture());
        WorkspaceMember savedMember = memberCaptor.getValue();
        assertThat(savedMember.getUserId()).isEqualTo(userId);
        assertThat(savedMember.getRole()).isEqualTo(WorkspaceRole.OWNER);
        assertThat(savedMember.getWorkspaceId()).isEqualTo(savedWs.getId());
    }

    @Test
    void secondLoginDoesNotCreateDuplicateWorkspaceOrMembership() {
        UUID userId = UUID.randomUUID();
        UUID existingWorkspaceId = UUID.randomUUID();

        // User already has a membership from the first login.
        WorkspaceMember existingMember = new WorkspaceMember(existingWorkspaceId, userId, WorkspaceRole.OWNER);
        when(workspaceMemberRepository.findAllByUserId(userId)).thenReturn(List.of(existingMember));

        service.ensurePersonalWorkspace(userId, "alice");

        // No writes at all.
        verify(workspaceRepository, never()).save(any());
        verify(workspaceMemberRepository, never()).save(any());
        verify(workspaceRepository, never()).existsBySlug(any());
    }

    @Test
    void slugCollisionAppendsShortFragment() {
        UUID userId = UUID.randomUUID();
        String login = "bob";

        when(workspaceMemberRepository.findAllByUserId(userId)).thenReturn(List.of());
        // First slug candidate "bob" is already taken.
        when(workspaceRepository.existsBySlug("bob")).thenReturn(true);

        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(invocation -> {
            Workspace ws = invocation.getArgument(0);
            ReflectionTestUtils.setField(ws, "id", UUID.randomUUID());
            return ws;
        });
        when(workspaceMemberRepository.save(any(WorkspaceMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.ensurePersonalWorkspace(userId, login);

        ArgumentCaptor<Workspace> wsCaptor = ArgumentCaptor.forClass(Workspace.class);
        verify(workspaceRepository).save(wsCaptor.capture());
        String slug = wsCaptor.getValue().getSlug();

        // Slug must start with "bob-" and have at least one extra character.
        assertThat(slug).startsWith("bob-");
        assertThat(slug.length()).isGreaterThan("bob".length());
    }

    @Test
    void toKebabCaseConvertsLoginCorrectly() {
        assertThat(WorkspaceBootstrapService.toKebabCase("Alice Smith")).isEqualTo("alice-smith");
        assertThat(WorkspaceBootstrapService.toKebabCase("john.doe")).isEqualTo("john-doe");
        assertThat(WorkspaceBootstrapService.toKebabCase("UPPER_CASE")).isEqualTo("upper-case");
        assertThat(WorkspaceBootstrapService.toKebabCase("  trim  ")).isEqualTo("trim");
        assertThat(WorkspaceBootstrapService.toKebabCase("alice")).isEqualTo("alice");
    }
}
