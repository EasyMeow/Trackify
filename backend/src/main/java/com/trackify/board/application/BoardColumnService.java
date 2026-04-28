package com.trackify.board.application;

import com.trackify.board.domain.BoardColumn;
import com.trackify.board.dto.ColumnResponse;
import com.trackify.board.infrastructure.BoardColumnRepository;
import com.trackify.common.exception.ConflictException;
import com.trackify.common.exception.NotFoundException;
import com.trackify.project.application.ProjectQueryService;
import com.trackify.task.application.TaskQueryService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Application service for board-column mutations.
 *
 * <p>TASK-045: Exposes {@link #createDefaultColumns(UUID)} so that
 * {@code ProjectCreateService} can seed the three standard columns
 * (Todo, In Progress, Done) inside the same transaction as project creation.
 *
 * <p>Propagation is {@code MANDATORY} — the caller (ProjectCreateService)
 * must already hold a transaction so the seeding and the project row are
 * committed or rolled back together.
 */
@Service
public class BoardColumnService {

    static final List<String> DEFAULT_COLUMN_NAMES = List.of("Todo", "In Progress", "Done");

    private final BoardColumnRepository boardColumnRepository;
    private final ProjectQueryService projectQueryService;
    private final TaskQueryService taskQueryService;

    public BoardColumnService(BoardColumnRepository boardColumnRepository,
                              ProjectQueryService projectQueryService,
                              TaskQueryService taskQueryService) {
        this.boardColumnRepository = boardColumnRepository;
        this.projectQueryService = projectQueryService;
        this.taskQueryService = taskQueryService;
    }

    /**
     * Persists the three default columns for {@code projectId} at positions 0, 1, 2.
     *
     * <p>Must be called from within an active transaction (propagation MANDATORY).
     *
     * @param projectId the newly-created project's UUID
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void createDefaultColumns(UUID projectId) {
        for (int i = 0; i < DEFAULT_COLUMN_NAMES.size(); i++) {
            boardColumnRepository.save(new BoardColumn(projectId, DEFAULT_COLUMN_NAMES.get(i), i));
        }
    }

    /**
     * Creates a new column appended to the end of the project's column order.
     *
     * <p>Authorization is enforced via {@link ProjectQueryService#getById}: the
     * caller must be a member of the project's workspace, otherwise 403/404 is thrown.
     *
     * @param projectId target project UUID
     * @param userId    authenticated caller's UUID
     * @param name      column name (non-blank, max 255 chars)
     * @return the created column as a response DTO
     */
    @Transactional
    public ColumnResponse createColumn(UUID projectId, UUID userId, String name) {
        projectQueryService.getById(projectId, userId);
        long count = boardColumnRepository.countByProjectId(projectId);
        BoardColumn column = boardColumnRepository.save(new BoardColumn(projectId, name, (int) count));
        return toResponse(column);
    }

    /**
     * Renames an existing column that belongs to {@code projectId}.
     *
     * <p>Authorization is enforced first (project membership), then existence of the
     * column within that project. A column that exists but belongs to a different
     * project yields 404 rather than leaking information.
     *
     * @param projectId target project UUID
     * @param columnId  column to rename
     * @param userId    authenticated caller's UUID
     * @param name      new name (non-blank, max 255 chars)
     * @return updated column as a response DTO
     */
    @Transactional
    public ColumnResponse renameColumn(UUID projectId, UUID columnId, UUID userId, String name) {
        projectQueryService.getById(projectId, userId);
        BoardColumn column = boardColumnRepository.findById(columnId)
                .filter(c -> c.getProjectId().equals(projectId))
                .orElseThrow(() -> new NotFoundException("Column not found"));
        column.setName(name);
        boardColumnRepository.save(column);
        return toResponse(column);
    }

    /**
     * Deletes a column that belongs to {@code projectId}, provided it contains no tasks.
     *
     * <p>Authorization is enforced first (project membership), then existence of the
     * column within that project. A non-empty column yields HTTP 409.
     *
     * @param projectId target project UUID
     * @param columnId  column to delete
     * @param userId    authenticated caller's UUID
     * @throws NotFoundException  when the column does not exist under this project
     * @throws ConflictException  when the column still contains tasks
     */
    @Transactional
    public void deleteColumn(UUID projectId, UUID columnId, UUID userId) {
        projectQueryService.getById(projectId, userId);
        BoardColumn column = boardColumnRepository.findById(columnId)
                .filter(c -> c.getProjectId().equals(projectId))
                .orElseThrow(() -> new NotFoundException("Column not found"));
        if (taskQueryService.hasTasksInColumn(column.getId())) {
            throw new ConflictException("Column must be emptied before it can be deleted");
        }
        boardColumnRepository.delete(column);
    }

    /**
     * Reorders all columns of {@code projectId} to match the supplied id list.
     *
     * <p>The request must include exactly the same set of column ids that belong to
     * the project — no extras, no omissions, no duplicates. Any violation throws
     * {@link IllegalArgumentException} which the global handler maps to HTTP 400.
     *
     * @param projectId target project UUID
     * @param userId    authenticated caller's UUID
     * @param columnIds complete ordered list of column ids
     * @return columns in the new order
     */
    @Transactional
    public List<ColumnResponse> reorderColumns(UUID projectId, UUID userId, List<UUID> columnIds) {
        projectQueryService.getById(projectId, userId);

        // Validate: no duplicates
        Set<UUID> seen = new HashSet<>();
        for (UUID id : columnIds) {
            if (!seen.add(id)) {
                throw new IllegalArgumentException("Duplicate column id: " + id);
            }
        }

        List<BoardColumn> existing = boardColumnRepository.findByProjectIdOrderByPositionAsc(projectId);
        Set<UUID> existingIds = existing.stream().map(BoardColumn::getId).collect(Collectors.toSet());

        // Validate: no unknown ids
        for (UUID id : columnIds) {
            if (!existingIds.contains(id)) {
                throw new IllegalArgumentException("Unknown column id: " + id);
            }
        }

        // Validate: no missing ids
        if (columnIds.size() != existingIds.size()) {
            throw new IllegalArgumentException("All " + existingIds.size() + " column ids must be provided");
        }

        Map<UUID, BoardColumn> byId = existing.stream()
                .collect(Collectors.toMap(BoardColumn::getId, Function.identity()));

        for (int i = 0; i < columnIds.size(); i++) {
            BoardColumn col = byId.get(columnIds.get(i));
            col.setPosition(i);
            boardColumnRepository.save(col);
        }

        return columnIds.stream().map(id -> toResponse(byId.get(id))).toList();
    }

    private static ColumnResponse toResponse(BoardColumn column) {
        return new ColumnResponse(
                column.getId(),
                column.getProjectId(),
                column.getName(),
                column.getPosition(),
                column.getCreatedAt(),
                column.getUpdatedAt()
        );
    }
}
