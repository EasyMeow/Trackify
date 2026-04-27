package com.trackify.comment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A user-authored comment attached to a single task.
 *
 * FK columns (task_id, author_id) are stored as opaque UUIDs to avoid
 * cross-module JPA coupling (architecture.md §7 modular monolith).
 * No {@code @ManyToOne} — the {@code task} and {@code user} modules must not
 * leak their entities into the {@code comment} module.
 */
@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** FK to tasks(id). Opaque UUID — no @ManyToOne. ON DELETE CASCADE in DB. */
    @Column(name = "task_id", nullable = false, updatable = false)
    private UUID taskId;

    /** FK to users(id). Nullable — ON DELETE SET NULL; comment survives account removal. */
    @Column(name = "author_id", updatable = false)
    private UUID authorId;

    @Column(name = "body", nullable = false)
    private String body;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** No-arg constructor for JPA; protected to discourage direct use. */
    protected Comment() {
    }

    /**
     * Creation constructor. All immutable fields are set here; {@code createdAt}
     * and {@code updatedAt} are assigned by the {@link #onCreate()} lifecycle hook.
     *
     * @param taskId   the task this comment belongs to (required)
     * @param authorId the user who wrote the comment (nullable — preserved if null)
     * @param body     the comment text (required, non-empty enforced by application layer)
     */
    public Comment(UUID taskId, UUID authorId, String body) {
        this.taskId = taskId;
        this.authorId = authorId;
        this.body = body;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public UUID getId() {
        return id;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public String getBody() {
        return body;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // -------------------------------------------------------------------------
    // Setters (mutable fields only)
    // -------------------------------------------------------------------------

    /**
     * Updates the comment body. The only post-creation field that may change;
     * taskId, authorId, and timestamps are immutable from the domain side.
     */
    public void setBody(String body) {
        this.body = body;
    }
}
