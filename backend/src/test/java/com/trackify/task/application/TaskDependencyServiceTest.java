package com.trackify.task.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trackify.common.exception.ConflictException;
import com.trackify.common.exception.NotFoundException;
import com.trackify.project.application.ProjectQueryService;
import com.trackify.task.domain.Task;
import com.trackify.task.domain.TaskDependency;
import com.trackify.task.dto.CreateDependencyRequest;
import com.trackify.task.dto.DependencyResponse;
import com.trackify.task.infrastructure.TaskDependencyRepository;
import com.trackify.task.infrastructure.TaskRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service-level tests for {@link TaskDependencyService} (TASK-089).
 *
 * <p>Covers the validation order described in the service Javadoc and the
 * BFS-based cycle detection.
 */
@ExtendWith(MockitoExtension.class)
class TaskDependencyServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskDependencyRepository taskDependencyRepository;

    @Mock
    private ProjectQueryService projectQueryService;

    private TaskDependencyService service;

    private UUID userId;
    private UUID projectId;
    private UUID successorId;
    private UUID predecessorId;

    @BeforeEach
    void setUp() {
        service = new TaskDependencyService(taskRepository, taskDependencyRepository, projectQueryService);
        userId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        successorId = UUID.randomUUID();
        predecessorId = UUID.randomUUID();
    }

    private Task taskInProject(UUID id, UUID project) {
        Task t = new Task(project, UUID.randomUUID(), userId, "t", null, "TODO", "MEDIUM",
                0.0, null, null, null);
        try {
            var idField = Task.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(t, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return t;
    }

    @Test
    void createPersistsEdgeAndReturnsResponse() {
        Task successor = taskInProject(successorId, projectId);
        Task predecessor = taskInProject(predecessorId, projectId);
        when(taskRepository.findById(successorId)).thenReturn(Optional.of(successor));
        when(taskRepository.findById(predecessorId)).thenReturn(Optional.of(predecessor));
        when(taskDependencyRepository.existsByPredecessorTaskIdAndSuccessorTaskId(predecessorId, successorId))
                .thenReturn(false);
        when(taskDependencyRepository.findByPredecessorTaskId(any(UUID.class)))
                .thenReturn(List.of());
        when(taskDependencyRepository.save(any(TaskDependency.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DependencyResponse response = service.create(successorId, userId, new CreateDependencyRequest(predecessorId));

        ArgumentCaptor<TaskDependency> captor = ArgumentCaptor.forClass(TaskDependency.class);
        verify(taskDependencyRepository).save(captor.capture());
        assertThat(captor.getValue().getPredecessorTaskId()).isEqualTo(predecessorId);
        assertThat(captor.getValue().getSuccessorTaskId()).isEqualTo(successorId);
        assertThat(response.predecessorTaskId()).isEqualTo(predecessorId);
        assertThat(response.successorTaskId()).isEqualTo(successorId);
    }

    @Test
    void createRejectsSelfLoop() {
        Task successor = taskInProject(successorId, projectId);
        when(taskRepository.findById(successorId)).thenReturn(Optional.of(successor));

        assertThatThrownBy(() -> service.create(successorId, userId, new CreateDependencyRequest(successorId)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(taskDependencyRepository, never()).save(any());
    }

    @Test
    void createRejectsCrossProjectEdge() {
        Task successor = taskInProject(successorId, projectId);
        Task predecessor = taskInProject(predecessorId, UUID.randomUUID());
        when(taskRepository.findById(successorId)).thenReturn(Optional.of(successor));
        when(taskRepository.findById(predecessorId)).thenReturn(Optional.of(predecessor));

        assertThatThrownBy(() -> service.create(successorId, userId, new CreateDependencyRequest(predecessorId)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(taskDependencyRepository, never()).save(any());
    }

    @Test
    void createRejectsDuplicate() {
        Task successor = taskInProject(successorId, projectId);
        Task predecessor = taskInProject(predecessorId, projectId);
        when(taskRepository.findById(successorId)).thenReturn(Optional.of(successor));
        when(taskRepository.findById(predecessorId)).thenReturn(Optional.of(predecessor));
        when(taskDependencyRepository.existsByPredecessorTaskIdAndSuccessorTaskId(predecessorId, successorId))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(successorId, userId, new CreateDependencyRequest(predecessorId)))
                .isInstanceOf(ConflictException.class);
        verify(taskDependencyRepository, never()).save(any());
    }

    @Test
    void createRejectsCycle() {
        // Existing chain: successor -> predecessor (so adding predecessor -> successor closes a cycle)
        Task successor = taskInProject(successorId, projectId);
        Task predecessor = taskInProject(predecessorId, projectId);
        when(taskRepository.findById(successorId)).thenReturn(Optional.of(successor));
        when(taskRepository.findById(predecessorId)).thenReturn(Optional.of(predecessor));
        when(taskDependencyRepository.existsByPredecessorTaskIdAndSuccessorTaskId(predecessorId, successorId))
                .thenReturn(false);
        when(taskDependencyRepository.findByPredecessorTaskId(successorId))
                .thenReturn(List.of(new TaskDependency(successorId, predecessorId)));

        assertThatThrownBy(() -> service.create(successorId, userId, new CreateDependencyRequest(predecessorId)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("cycle");
        verify(taskDependencyRepository, never()).save(any());
    }

    @Test
    void createNotFoundWhenSuccessorMissing() {
        when(taskRepository.findById(successorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(successorId, userId, new CreateDependencyRequest(predecessorId)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createNotFoundWhenPredecessorMissing() {
        Task successor = taskInProject(successorId, projectId);
        when(taskRepository.findById(successorId)).thenReturn(Optional.of(successor));
        when(taskRepository.findById(predecessorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(successorId, userId, new CreateDependencyRequest(predecessorId)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteRemovesEdge() {
        UUID dependencyId = UUID.randomUUID();
        TaskDependency edge = new TaskDependency(predecessorId, successorId);
        try {
            var idField = TaskDependency.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(edge, dependencyId);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        Task successor = taskInProject(successorId, projectId);

        when(taskDependencyRepository.findById(dependencyId)).thenReturn(Optional.of(edge));
        when(taskRepository.findById(successorId)).thenReturn(Optional.of(successor));

        service.delete(successorId, dependencyId, userId);

        verify(taskDependencyRepository).delete(edge);
    }

    @Test
    void deleteNotFoundWhenEdgeBelongsToDifferentTask() {
        UUID dependencyId = UUID.randomUUID();
        UUID otherTaskId = UUID.randomUUID();
        TaskDependency edge = new TaskDependency(predecessorId, otherTaskId);

        when(taskDependencyRepository.findById(dependencyId)).thenReturn(Optional.of(edge));

        assertThatThrownBy(() -> service.delete(successorId, dependencyId, userId))
                .isInstanceOf(NotFoundException.class);
        verify(taskDependencyRepository, never()).delete(any(TaskDependency.class));
    }
}
