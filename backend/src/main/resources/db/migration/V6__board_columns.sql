-- board_columns defines the Kanban columns for a project.
-- Each column belongs to exactly one project (FK with ON DELETE CASCADE so removing a project
-- removes its columns automatically).  position drives the left-to-right display order.
-- The UNIQUE constraint on (project_id, position) prevents duplicate ordering within a project
-- and is the natural key used when re-ordering columns.

CREATE TABLE board_columns (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id  UUID        NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name        TEXT        NOT NULL,
    position    INTEGER     NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT board_columns_project_position_unique UNIQUE (project_id, position)
);

CREATE INDEX board_columns_project_id_idx ON board_columns (project_id);
