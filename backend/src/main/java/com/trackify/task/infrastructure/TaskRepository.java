package com.trackify.task.infrastructure;

import com.trackify.task.domain.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence port for the task domain.
 *
 * Finders kept to the bare minimum required by the board read path (TASK-047).
 * Additional finders (timeline query, task-detail lookup, etc.) will be added
 * in the tasks that need them.
 */
public interface TaskRepository extends JpaRepository<Task, UUID> {

    /**
     * Returns all tasks for a project, ordered by column then sort position.
     * Matches the composite index tasks_project_column_sort_idx in V7.
     * Used by the board read endpoint to assemble columns with their ordered cards.
     */
    List<Task> findByProjectIdOrderByColumnIdAscSortOrderAsc(UUID projectId);

    /**
     * Returns the task with the highest {@code sort_order} in the given column,
     * or empty if the column contains no tasks.
     *
     * <p>Used by {@code TaskCommandService} to compute the next {@code sortOrder}
     * when appending a task to a column: {@code max + 1.0}, or {@code 0.0} when empty.
     * A derived-query finder is preferred over a {@code @Query(MAX)} because Spring
     * Data generates an efficient {@code ORDER BY sort_order DESC LIMIT 1} query
     * without requiring a separate aggregation projection.
     */
    Optional<Task> findTopByColumnIdOrderBySortOrderDesc(UUID columnId);

    List<Task> findByProjectId(UUID projectId);

    void deleteByProjectId(UUID projectId);
}
