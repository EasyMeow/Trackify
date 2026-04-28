package com.trackify.board.application;

import com.trackify.board.domain.BoardColumn;
import com.trackify.board.dto.ColumnResponse;
import com.trackify.board.infrastructure.BoardColumnRepository;
import com.trackify.project.application.ProjectQueryService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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

    public BoardColumnService(BoardColumnRepository boardColumnRepository,
                              ProjectQueryService projectQueryService) {
        this.boardColumnRepository = boardColumnRepository;
        this.projectQueryService = projectQueryService;
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
