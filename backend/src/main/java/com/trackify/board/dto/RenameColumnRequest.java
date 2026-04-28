package com.trackify.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameColumnRequest(
        @NotBlank @Size(max = 255) String name
) {
}
