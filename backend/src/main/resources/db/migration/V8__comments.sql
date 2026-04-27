-- comments stores user-authored notes attached to a task.
-- A comment belongs to one task and (optionally) one author.
--
-- Schema choices:
--   task_id:   ON DELETE CASCADE — removing a task removes all its comments; a
--              comment is subordinate to the task and has no value without it.
--   author_id: nullable + ON DELETE SET NULL — comment survives account removal,
--              keeping the discussion history intact while clearing the attribution
--              pointer. Mirrors the created_by pattern used in V7 for tasks.
--   body:      TEXT with no length cap — no arbitrary character limit imposed at
--              the DB layer; validation can be done at the application layer if
--              needed, and long technical comments (code snippets, stack traces)
--              should not be silently truncated.
--   created_at / updated_at: TIMESTAMPTZ for timezone-safe ordering and display.

CREATE TABLE comments (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id     UUID        NOT NULL REFERENCES tasks(id)  ON DELETE CASCADE,
    author_id   UUID                 REFERENCES users(id)  ON DELETE SET NULL,
    body        TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Supports the primary listing query: all comments for a task in chronological order.
-- Composite on (task_id, created_at) so the planner can satisfy the WHERE + ORDER BY
-- from the index alone without a separate sort step.
CREATE INDEX comments_task_id_created_at_idx ON comments (task_id, created_at);
