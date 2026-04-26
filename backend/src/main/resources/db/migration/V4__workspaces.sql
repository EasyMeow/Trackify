-- Workspaces and membership. A workspace is the top-level container for projects.
-- Slug is the URL-safe handle; must be unique across all workspaces.
-- owner_id references users(id) — ON DELETE RESTRICT because owner removal
-- requires explicit workspace transfer or deletion first.

CREATE TABLE workspaces (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name        TEXT        NOT NULL,
    slug        TEXT        NOT NULL,
    owner_id    UUID        NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT workspaces_slug_unique UNIQUE (slug)
);

-- workspace_members maps users into workspaces with a role.
-- One row per (workspace, user) pair — enforced by the unique constraint.
-- Index on user_id powers the "list workspaces for user" query (TASK-035).

CREATE TABLE workspace_members (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id    UUID        NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    user_id         UUID        NOT NULL REFERENCES users(id)      ON DELETE CASCADE,
    role            TEXT        NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT workspace_members_role_check CHECK (role IN ('OWNER','ADMIN','MEMBER','VIEWER')),
    CONSTRAINT workspace_members_workspace_user_unique UNIQUE (workspace_id, user_id)
);

CREATE INDEX workspace_members_user_id_idx ON workspace_members (user_id);
