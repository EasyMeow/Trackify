package com.trackify.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /api/workspaces/{workspaceId}/projects} (TASK-041).
 *
 * <p>{@code slug} is optional: when absent or blank the application service derives
 * it from {@code name} (lowercase, spaces replaced with hyphens). If supplied it
 * must satisfy the same length bound as the DB column allows.
 */
public record ProjectCreateRequest(
        @NotBlank(message = "name must not be blank")
        @Size(max = 255, message = "name must be at most 255 characters")
        String name,

        @Size(max = 255, message = "slug must be at most 255 characters")
        String slug,

        @Size(max = 2000, message = "description must be at most 2000 characters")
        String description
) {
}
