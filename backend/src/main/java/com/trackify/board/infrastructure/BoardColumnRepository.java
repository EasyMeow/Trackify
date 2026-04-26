package com.trackify.board.infrastructure;

import com.trackify.board.domain.BoardColumn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data repository for {@link BoardColumn}.
 * Only exposes finders needed by TASK-044 scope; additional finders will be added
 * in TASK-045 (default-column seeding) and later board write/read tasks.
 */
public interface BoardColumnRepository extends JpaRepository<BoardColumn, UUID> {

    List<BoardColumn> findByProjectIdOrderByPositionAsc(UUID projectId);
}
