package com.trackify.project.dto;

import jakarta.validation.constraints.Size;

/**
 * Partial-update payload for PATCH /api/projects/{projectId}.
 * Both fields are optional; null means "do not change".
 */
public record ProjectPatchRequest(

        @Size(min = 1, max = 255, message = "name must be between 1 and 255 characters")
        String name,

        @Size(max = 1000, message = "description must not exceed 1000 characters")
        String description
) {
}
