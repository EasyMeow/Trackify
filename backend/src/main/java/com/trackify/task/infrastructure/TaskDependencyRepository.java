package com.trackify.task.infrastructure;

import com.trackify.task.domain.TaskDependency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Persistence port for task dependency edges.
 *
 * Two directional finders cover the primary read paths used by the timeline
 * payload (TASK-067) and by the dependency create/delete endpoints (TASK-072).
 * Cycle-detection helpers belong in the application service, not here.
 *
 * Both finders are backed by indexes defined in V9:
 *   - outgoing (predecessorTaskId): covered by the UNIQUE constraint index on
 *     (predecessor_task_id, successor_task_id).
 *   - incoming (successorTaskId): covered by task_dependencies_successor_task_id_idx.
 */
public interface TaskDependencyRepository extends JpaRepository<TaskDependency, UUID> {

    /**
     * Returns all edges where the given task is the predecessor (outgoing edges).
     *
     * <p>Answers "which tasks depend on {@code predecessorTaskId}?"
     *
     * @param predecessorTaskId the upstream task
     * @return dependency edges in insertion order; empty list if none
     */
    List<TaskDependency> findByPredecessorTaskId(UUID predecessorTaskId);

    /**
     * Returns all edges where the given task is the successor (incoming edges).
     *
     * <p>Answers "which tasks must complete before {@code successorTaskId} can start?"
     *
     * @param successorTaskId the downstream task
     * @return dependency edges in insertion order; empty list if none
     */
    List<TaskDependency> findBySuccessorTaskId(UUID successorTaskId);
}
