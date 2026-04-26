-- Projects belong to a workspace. Slug is unique within a workspace (not globally).
-- owner_id references users(id); ON DELETE RESTRICT requires explicit ownership transfer
-- before a user account can be removed.

CREATE TABLE projects (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id    UUID        NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    owner_id        UUID        NOT NULL REFERENCES users(id)      ON DELETE RESTRICT,
    name            TEXT        NOT NULL,
    slug            TEXT        NOT NULL,
    description     TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT projects_workspace_slug_unique UNIQUE (workspace_id, slug)
);

CREATE INDEX projects_workspace_id_idx ON projects (workspace_id);

-- project_members maps users into projects with a role.
-- One row per (project, user) pair — enforced by the unique constraint.
-- Index on user_id powers "list projects for user" queries.

CREATE TABLE project_members (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id  UUID        NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id     UUID        NOT NULL REFERENCES users(id)    ON DELETE CASCADE,
    role        TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT project_members_role_check CHECK (role IN ('OWNER','ADMIN','MEMBER','VIEWER')),
    CONSTRAINT project_members_project_user_unique UNIQUE (project_id, user_id)
);

CREATE INDEX project_members_user_id_idx ON project_members (user_id);
