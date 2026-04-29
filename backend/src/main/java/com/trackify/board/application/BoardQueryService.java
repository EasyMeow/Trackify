package com.trackify.board.application;

import com.trackify.board.domain.BoardColumn;
import com.trackify.board.dto.BoardColumnResponse;
import com.trackify.board.dto.BoardResponse;
import com.trackify.board.dto.BoardTaskCardResponse;
import com.trackify.board.infrastructure.BoardColumnRepository;
import com.trackify.common.exception.ForbiddenException;
import com.trackify.common.exception.NotFoundException;
import com.trackify.project.application.ProjectQueryService;
import com.trackify.task.domain.Task;
import com.trackify.task.infrastructure.TaskRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Application service for the board read model (TASK-047).
 *
 * <p>Reads columns ordered by position and tasks ordered by (columnId, sortOrder),
 * then groups tasks under their column for the Kanban UI shape. Authorization
 * delegates to {@link ProjectQueryService#getById(UUID, UUID)} — the same check
 * that backs {@code GET /api/projects/{projectId}}.
 *
 * <p>This is a read-only service: no mutations, no calls into the task application
 * service. Board and timeline modules may only mutate tasks via the task module
 * (architecture.md §12).
 */
@Service
public class BoardQueryService {

    private final ProjectQueryService projectQueryService;
    private final BoardColumnRepository boardColumnRepository;
    private final TaskRepository taskRepository;

    public BoardQueryService(ProjectQueryService projectQueryService,
                             BoardColumnRepository boardColumnRepository,
                             TaskRepository taskRepository) {
        this.projectQueryService = projectQueryService;
        this.boardColumnRepository = boardColumnRepository;
        this.taskRepository = taskRepository;
    }

    /**
     * Returns the board for the given project, asserting that {@code userId} is
     * a workspace member with read access.
     *
     * @param projectId target project UUID
     * @param userId    authenticated user's UUID
     * @return board DTO with ordered columns and task cards
     * @throws NotFoundException  if the project does not exist
     * @throws ForbiddenException if the caller is not a member of the project's workspace
     */
    @Transactional(readOnly = true)
    public BoardResponse getBoard(UUID projectId, UUID userId) {
        // Authorization: reuse the existing project access check.
        // Throws NotFoundException or ForbiddenException as appropriate.
        projectQueryService.getById(projectId, userId);

        // Columns ordered by position (index in the DB).
        List<BoardColumn> columns = boardColumnRepository.findByProjectIdOrderByPositionAsc(projectId);

        // All project tasks, ordered by (column_id asc, sort_order asc).
        List<Task> tasks = taskRepository.findByProjectIdOrderByColumnIdAscSortOrderAsc(projectId);

        // Group tasks by columnId — preserves the sort_order order from the query.
        Map<UUID, List<Task>> tasksByColumn = tasks.stream()
                .collect(Collectors.groupingBy(Task::getColumnId,
                        Collectors.toCollection(ArrayList::new)));

        List<BoardColumnResponse> columnResponses = columns.stream()
                .map(col -> {
                    List<Task> colTasks = tasksByColumn.getOrDefault(col.getId(), List.of());
                    List<BoardTaskCardResponse> cards = colTasks.stream()
                            .map(BoardQueryService::toCard)
                            .toList();
                    return new BoardColumnResponse(col.getId(), col.getName(), col.getPosition(), cards);
                })
                .toList();

        return new BoardResponse(projectId, columnResponses);
    }

    private static BoardTaskCardResponse toCard(Task task) {
        return new BoardTaskCardResponse(
                task.getId(),
                task.getTitle(),
                task.getStatus(),
                task.getPriority(),
                task.getSortOrder(),
                task.getStartDate(),
                task.getDueDate(),
                task.getCreatedAt()
        );
    }
}
