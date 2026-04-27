package com.trackify.task.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Single task record shared by the Kanban board and Gantt timeline.
 * Both views are read-models over this entity; mutations always go through
 * the task application service (architecture.md §12).
 *
 * FK columns (project_id, column_id, created_by) are stored as opaque UUIDs
 * to avoid cross-module JPA coupling (architecture.md §7 modular monolith).
 */
@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** FK to projects(id). Opaque UUID — no @ManyToOne. */
    @Column(name = "project_id", nullable = false, updatable = false)
    private UUID projectId;

    /** FK to board_columns(id). Opaque UUID — no @ManyToOne. */
    @Column(name = "column_id", nullable = false)
    private UUID columnId;

    /** FK to users(id). Nullable — ON DELETE SET NULL; task survives account removal. */
    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    /**
     * Lifecycle status stored as TEXT in the DB.
     * Valid values match the tasks_status_check constraint:
     * TODO, IN_PROGRESS, IN_REVIEW, DONE, CANCELLED.
     */
    @Column(name = "status", nullable = false)
    private String status;

    /**
     * Priority stored as TEXT in the DB.
     * Valid values match the tasks_priority_check constraint:
     * LOW, MEDIUM, HIGH, URGENT.
     */
    @Column(name = "priority", nullable = false)
    private String priority;

    /**
     * Fractional position within a column. DOUBLE PRECISION allows inserting a task
     * between two existing sort_order values without re-numbering the whole column.
     */
    @Column(name = "sort_order", nullable = false)
    private double sortOrder;

    /** Calendar start date (nullable). Not a timestamp — date only. */
    @Column(name = "start_date")
    private LocalDate startDate;

    /** Calendar due date (nullable). Not a timestamp — date only. */
    @Column(name = "due_date")
    private LocalDate dueDate;

    /** Estimated effort in hours (nullable). */
    @Column(name = "estimated_hours", precision = 6, scale = 2)
    private BigDecimal estimatedHours;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** No-arg constructor for JPA; package-private to discourage direct use. */
    protected Task() {
    }

    /**
     * Creation constructor. status defaults to "TODO", priority to "MEDIUM",
     * sortOrder to 0 — callers should adjust sort_order when inserting into
     * a non-empty column.
     */
    public Task(UUID projectId, UUID columnId, UUID createdBy, String title,
                String description, String status, String priority, double sortOrder,
                LocalDate startDate, LocalDate dueDate, BigDecimal estimatedHours) {
        this.projectId = projectId;
        this.columnId = columnId;
        this.createdBy = createdBy;
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.sortOrder = sortOrder;
        this.startDate = startDate;
        this.dueDate = dueDate;
        this.estimatedHours = estimatedHours;
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

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getColumnId() {
        return columnId;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public String getPriority() {
        return priority;
    }

    public double getSortOrder() {
        return sortOrder;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public BigDecimal getEstimatedHours() {
        return estimatedHours;
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

    public void setColumnId(UUID columnId) {
        this.columnId = columnId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public void setSortOrder(double sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public void setEstimatedHours(BigDecimal estimatedHours) {
        this.estimatedHours = estimatedHours;
    }
}
