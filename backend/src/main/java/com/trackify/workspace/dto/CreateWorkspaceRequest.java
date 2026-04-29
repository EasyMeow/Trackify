package com.trackify.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /api/workspaces} (TASK-108).
 *
 * <p>{@code name} is required and non-blank. Slug is auto-derived from the name
 * by {@link com.trackify.workspace.application.WorkspaceCreateService}; callers
 * cannot supply a custom slug (unlike project creation) to keep the surface small.
 */
public record CreateWorkspaceRequest(
        @NotBlank(message = "name must not be blank")
        @Size(max = 255, message = "name must be at most 255 characters")
        String name
) {
}
