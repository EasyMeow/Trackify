package com.trackify.board.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record ReorderColumnsRequest(
        @NotEmpty(message = "Column id list must not be empty")
        List<UUID> columnIds
) {
}
