package com.trackify.task.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trackify.board.domain.BoardColumn;
import com.trackify.board.infrastructure.BoardColumnRepository;
import com.trackify.common.exception.NotFoundException;
import com.trackify.project.application.ProjectQueryService;
import com.trackify.task.domain.Task;
import com.trackify.task.dto.MoveTaskRequest;
import com.trackify.task.dto.TaskResponse;
import com.trackify.task.infrastructure.TaskRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

/**
 * Service-level tests for {@link TaskCommandService#moveTask} (TASK-059).
 *
 * <p>Complements {@code TaskMoveControllerTest} (which only verifies the
 * controller pass-through) by exercising the actual move logic with mocked
 * collaborators:
 *
 * <ul>
 *   <li>Move within the same column updates only {@code sortOrder}.</li>
 *   <li>Move across columns updates both {@code columnId} and {@code sortOrder}.</li>
 *   <li>Destination column in a different project is rejected with HTTP 400.</li>
 *   <li>Missing task → 404.</li>
 *   <li>Missing destination column → 404.</li>
 * </ul>
 *
 * <p>Uses Mockito directly (no Spring context) to stay consistent with
 * {@code BoardColumnServiceTest} and {@code WorkspaceBootstrapServiceTest}.
 */
@ExtendWith(MockitoExtension.class)
class TaskCommandServiceMoveTest {

    @Mock
    private ProjectQueryService projectQueryService;

    @Mock
    private BoardColumnRepository boardColumnRepository;

    @Mock
    private TaskRepository taskRepository;

    private TaskCommandService service;

    private UUID projectId;
    private UUID userId;
    private UUID taskId;
    private UUID originColumnId;
    private UUID destinationColumnId;

    @BeforeEach
    void setUp() {
        service = new TaskCommandService(projectQueryService, boardColumnRepository, taskRepository);

        projectId = UUID.randomUUID();
        userId = UUID.randomUUID();
        taskId = UUID.randomUUID();
        originColumnId = UUID.randomUUID();
        destinationColumnId = UUID.randomUUID();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Task buildTask(UUID columnId, double sortOrder) {
        return new Task(
                projectId,
                columnId,
                userId,
                "Move me",
                null,
                "TODO",
                "MEDIUM",
                sortOrder,
                null,
                null,
                null
        );
    }

    private BoardColumn buildColumn(UUID columnProjectId) {
        return new BoardColumn(columnProjectId, "Anywhere", 0);
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    void moveWithinSameColumnUpdatesOnlySortOrder() {
        Task existing = buildTask(originColumnId, 1.0);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(existing));
        when(boardColumnRepository.findById(originColumnId))
                .thenReturn(Optional.of(buildColumn(projectId)));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse response = service.moveTask(
                taskId, userId, new MoveTaskRequest(originColumnId, 5.5));

        assertThat(response.columnId()).isEqualTo(originColumnId);
        assertThat(response.sortOrder()).isEqualTo(5.5);

        // Authorization is delegated to ProjectQueryService.
        verify(projectQueryService).getById(projectId, userId);

        // Confirm the saved entity carries the new sortOrder and unchanged columnId.
        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        assertThat(captor.getValue().getColumnId()).isEqualTo(originColumnId);
        assertThat(captor.getValue().getSortOrder()).isEqualTo(5.5);
    }

    @Test
    void moveAcrossColumnsUpdatesColumnIdAndSortOrder() {
        Task existing = buildTask(originColumnId, 1.0);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(existing));
        when(boardColumnRepository.findById(destinationColumnId))
                .thenReturn(Optional.of(buildColumn(projectId)));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse response = service.moveTask(
                taskId, userId, new MoveTaskRequest(destinationColumnId, 0.0));

        assertThat(response.columnId()).isEqualTo(destinationColumnId);
        assertThat(response.sortOrder()).isEqualTo(0.0);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        assertThat(captor.getValue().getColumnId()).isEqualTo(destinationColumnId);
        assertThat(captor.getValue().getSortOrder()).isEqualTo(0.0);
    }

    @Test
    void destinationColumnInDifferentProjectIsRejected() {
        UUID otherProjectId = UUID.randomUUID();
        Task existing = buildTask(originColumnId, 1.0);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(existing));
        when(boardColumnRepository.findById(destinationColumnId))
                .thenReturn(Optional.of(buildColumn(otherProjectId)));

        assertThatThrownBy(() -> service.moveTask(
                taskId, userId, new MoveTaskRequest(destinationColumnId, 0.0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong");

        verify(taskRepository, never()).save(any());
    }

    @Test
    void missingTaskThrowsNotFound() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.moveTask(
                taskId, userId, new MoveTaskRequest(destinationColumnId, 0.0)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Task not found");

        verify(taskRepository, never()).save(any());
    }

    @Test
    void missingDestinationColumnThrowsNotFound() {
        Task existing = buildTask(originColumnId, 1.0);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(existing));
        when(boardColumnRepository.findById(destinationColumnId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.moveTask(
                taskId, userId, new MoveTaskRequest(destinationColumnId, 0.0)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Board column not found");

        verify(taskRepository, never()).save(any());
    }
}
