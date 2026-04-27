package com.trackify.project.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trackify.board.application.BoardColumnService;
import com.trackify.common.exception.ConflictException;
import com.trackify.common.exception.ForbiddenException;
import com.trackify.project.domain.Project;
import com.trackify.project.dto.ProjectCreateRequest;
import com.trackify.project.infrastructure.ProjectRepository;
import com.trackify.workspace.infrastructure.WorkspaceMemberRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

/**
 * Proves the TASK-045 wire-up contract: project creation also seeds default board columns.
 *
 * <ul>
 *   <li>Happy path: {@link BoardColumnService#createDefaultColumns} is called exactly once
 *       with the id that {@code projectRepository.save} returned.</li>
 *   <li>Forbidden path: neither {@code projectRepository.save} nor
 *       {@code boardColumnService.createDefaultColumns} is ever invoked.</li>
 *   <li>Slug-conflict path: {@code boardColumnService.createDefaultColumns} is never invoked.</li>
 * </ul>
 *
 * <p>Note: the column ordering guarantee (Todo → In Progress → Done, positions 0/1/2)
 * is verified by {@code BoardColumnServiceTest}. This test only needs to prove the
 * <em>invocation</em> of {@code createDefaultColumns} with the correct project id.
 *
 * <p>Uses Mockito (no Spring context) for speed, consistent with
 * {@code WorkspaceBootstrapServiceTest} and {@code BoardColumnServiceTest}.
 */
@ExtendWith(MockitoExtension.class)
class ProjectCreateServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Mock
    private BoardColumnService boardColumnService;

    private ProjectCreateService service;

    @BeforeEach
    void setUp() {
        service = new ProjectCreateService(projectRepository, workspaceMemberRepository, boardColumnService);
    }

    /**
     * Happy path: a successful project creation must trigger default-column seeding
     * with the id of the saved project record.
     */
    @Test
    void creatingProjectAlsoCreatesDefaultColumnsInOrder() {
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID savedProjectId = UUID.randomUUID();

        ProjectCreateRequest request = new ProjectCreateRequest("My Project", null, "desc");

        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId))
                .thenReturn(true);
        when(projectRepository.existsByWorkspaceIdAndSlug(workspaceId, "my-project"))
                .thenReturn(false);

        // Simulate the DB assigning an id on save.
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project p = invocation.getArgument(0);
            ReflectionTestUtils.setField(p, "id", savedProjectId);
            // @PrePersist is a JPA lifecycle callback; set timestamps manually in test.
            ReflectionTestUtils.setField(p, "createdAt", java.time.Instant.now());
            ReflectionTestUtils.setField(p, "updatedAt", java.time.Instant.now());
            return p;
        });

        service.create(workspaceId, userId, request);

        // The wire-up line in ProjectCreateService must call createDefaultColumns
        // with exactly the id that was assigned by the repository.
        verify(boardColumnService).createDefaultColumns(savedProjectId);
    }

    /**
     * When the caller is not a workspace member, both save and column seeding must
     * be skipped entirely.
     */
    @Test
    void forbiddenWorkspaceDoesNotCreateColumns() {
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        ProjectCreateRequest request = new ProjectCreateRequest("My Project", null, null);

        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId))
                .thenReturn(false);

        assertThatThrownBy(() -> service.create(workspaceId, userId, request))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(projectRepository);
        verifyNoInteractions(boardColumnService);
    }

    /**
     * When the derived slug collides with an existing project, column seeding must
     * not be triggered.
     */
    @Test
    void slugConflictDoesNotCreateColumns() {
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        ProjectCreateRequest request = new ProjectCreateRequest("My Project", null, null);

        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId))
                .thenReturn(true);
        when(projectRepository.existsByWorkspaceIdAndSlug(workspaceId, "my-project"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(workspaceId, userId, request))
                .isInstanceOf(ConflictException.class);

        verify(projectRepository).existsByWorkspaceIdAndSlug(workspaceId, "my-project");
        verifyNoInteractions(boardColumnService);
    }
}
