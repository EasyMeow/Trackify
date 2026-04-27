package com.trackify.board.application;

import com.trackify.board.domain.BoardColumn;
import com.trackify.board.infrastructure.BoardColumnRepository;

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

    public BoardColumnService(BoardColumnRepository boardColumnRepository) {
        this.boardColumnRepository = boardColumnRepository;
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
}
