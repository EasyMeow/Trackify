package com.trackify.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameWorkspaceRequest(
        @NotBlank @Size(max = 255) String name
) {
}
