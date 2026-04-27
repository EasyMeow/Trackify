package com.trackify.task.infrastructure;

import com.trackify.task.domain.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
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
}
