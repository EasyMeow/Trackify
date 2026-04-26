package com.trackify.workspace.domain;

/**
 * Role a member holds within a workspace.
 * Values must match the CHECK constraint in V4__workspaces.sql.
 */
public enum WorkspaceRole {
    OWNER,
    ADMIN,
    MEMBER,
    VIEWER
}
