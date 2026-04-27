-- task_dependencies models directed predecessor → successor edges between tasks.
-- A predecessor must finish (or at least exist) before a successor can start.
-- Both views (Kanban board and Gantt timeline) read these edges; mutations always
-- go through the task application service (architecture.md §12).
--
-- Schema choices:
--   id:                   Synthetic UUID PK — consistent with workspace_members,
--                         project_members, and comments, which all use a surrogate
--                         key rather than a composite PK. Lets the application layer
--                         address an edge by a single stable identifier.
--   predecessor_task_id:  ON DELETE CASCADE — removing a task removes edges where
--                         it was the prerequisite; dependents become unblocked.
--   successor_task_id:    ON DELETE CASCADE — removing a task removes edges where
--                         it was the dependent; predecessors are unaffected.
--   UNIQUE (predecessor_task_id, successor_task_id): prevents duplicate edges while
--                         using the simpler surrogate-PK approach.
--   CHECK predecessor <> successor: self-dependency is meaningless and would trivially
--                         satisfy a cycle-detection query; reject it at the DB layer.
--   created_at:           TIMESTAMPTZ for timezone-safe audit; not updatable.
--   No updated_at:        An edge is either present or absent; there is nothing to
--                         update on the row itself.

CREATE TABLE task_dependencies (
    id                   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    predecessor_task_id  UUID        NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    successor_task_id    UUID        NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT task_dependencies_no_self_loop
        CHECK (predecessor_task_id <> successor_task_id),
    CONSTRAINT task_dependencies_unique_edge
        UNIQUE (predecessor_task_id, successor_task_id)
);

-- Supports "what tasks must complete before this one?" lookups (incoming edges).
-- The UNIQUE constraint already provides an index on (predecessor_task_id, successor_task_id),
-- covering outgoing-edge lookups; this index covers the successor direction.
CREATE INDEX task_dependencies_successor_task_id_idx
    ON task_dependencies (successor_task_id);
