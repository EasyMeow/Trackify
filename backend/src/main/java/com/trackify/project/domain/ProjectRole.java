package com.trackify.project.domain;

/**
 * Role a member holds within a project.
 * Values must match the CHECK constraint in V5__projects.sql.
 */
public enum ProjectRole {
    OWNER,
    ADMIN,
    MEMBER,
    VIEWER
}
