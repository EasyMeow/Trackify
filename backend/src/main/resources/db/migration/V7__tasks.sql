-- tasks is the single shared record powering both Kanban board and Gantt timeline.
-- Both views read from this table; mutations always go through the task application service.
--
-- Schema choices:
--   status/priority: TEXT + CHECK constraint, consistent with V5 role column style.
--   sort_order: DOUBLE PRECISION to allow fractional reordering between existing values
--               without a full gap re-numbering pass.
--   start_date / due_date: nullable DATE (calendar dates, not timestamps).
--   estimated_hours: nullable NUMERIC(6,2).
--   project_id:  ON DELETE CASCADE — removing a project removes its tasks.
--   column_id:   ON DELETE RESTRICT — deleting a column that owns tasks must be
--                an explicit business operation (move or delete tasks first).
--   created_by:  nullable + ON DELETE SET NULL — task record survives account removal,
--                keeping history intact while clearing the attribution pointer.

CREATE TABLE tasks (
    id               UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id       UUID           NOT NULL REFERENCES projects(id)      ON DELETE CASCADE,
    column_id        UUID           NOT NULL REFERENCES board_columns(id)  ON DELETE RESTRICT,
    created_by       UUID                    REFERENCES users(id)          ON DELETE SET NULL,
    title            TEXT           NOT NULL,
    description      TEXT,
    status           TEXT           NOT NULL DEFAULT 'TODO',
    priority         TEXT           NOT NULL DEFAULT 'MEDIUM',
    sort_order       DOUBLE PRECISION NOT NULL DEFAULT 0,
    start_date       DATE,
    due_date         DATE,
    estimated_hours  NUMERIC(6,2),
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT tasks_status_check   CHECK (status   IN ('TODO','IN_PROGRESS','IN_REVIEW','DONE','CANCELLED')),
    CONSTRAINT tasks_priority_check CHECK (priority IN ('LOW','MEDIUM','HIGH','URGENT'))
);

-- Primary board query: tasks for a project ordered by column then sort position.
CREATE INDEX tasks_project_column_sort_idx ON tasks (project_id, column_id, sort_order);

-- Supports "all tasks in a column" lookup (e.g., when validating ON DELETE RESTRICT).
CREATE INDEX tasks_column_id_idx ON tasks (column_id);

-- Supports "tasks created by user" lookups (e.g., after ON DELETE SET NULL migration).
CREATE INDEX tasks_created_by_idx ON tasks (created_by);
