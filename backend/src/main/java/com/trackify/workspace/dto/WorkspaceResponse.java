package com.trackify.workspace.dto;

import java.util.UUID;

/**
 * Response payload for {@code GET /api/workspaces} (TASK-035). Exposes the
 * fields the frontend needs to display the workspace selector. Entities never
 * cross this boundary.
 */
public record WorkspaceResponse(
        UUID id,
        String name,
        String slug
) {
}
