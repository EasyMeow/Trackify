package com.trackify.task.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A directed dependency edge: {@code predecessorTaskId} must finish (or at least
 * exist) before {@code successorTaskId} can start.
 *
 * FK columns are stored as opaque UUIDs to avoid cross-entity JPA coupling within
 * the task domain. A surrogate UUID PK is used for consistency with the rest of
 * the codebase (workspace_members, project_members, comments all use this style).
 *
 * The DB-level CHECK prevents self-loops; the UNIQUE constraint prevents duplicate
 * edges. Cycle detection is an application-layer concern (TASK-072).
 */
@Entity
@Table(name = "task_dependencies")
public class TaskDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** FK to tasks(id). ON DELETE CASCADE — removing a task removes edges it originates. */
    @Column(name = "predecessor_task_id", nullable = false, updatable = false)
    private UUID predecessorTaskId;

    /** FK to tasks(id). ON DELETE CASCADE — removing a task removes edges that depend on it. */
    @Column(name = "successor_task_id", nullable = false, updatable = false)
    private UUID successorTaskId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** No-arg constructor for JPA; protected to discourage direct use. */
    protected TaskDependency() {
    }

    /**
     * Creation constructor. Both task IDs are immutable once the edge is persisted;
     * to change a dependency the caller must delete the old edge and insert a new one.
     *
     * @param predecessorTaskId the task that must come first (required)
     * @param successorTaskId   the task that depends on the predecessor (required)
     */
    public TaskDependency(UUID predecessorTaskId, UUID successorTaskId) {
        this.predecessorTaskId = predecessorTaskId;
        this.successorTaskId = successorTaskId;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    // -------------------------------------------------------------------------
    // Getters — all fields are immutable; no setters
    // -------------------------------------------------------------------------

    public UUID getId() {
        return id;
    }

    public UUID getPredecessorTaskId() {
        return predecessorTaskId;
    }

    public UUID getSuccessorTaskId() {
        return successorTaskId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
