package com.trackify.board.dto;

import java.util.List;
import java.util.UUID;

/**
 * A board column with its ordered task cards (TASK-047).
 */
public record BoardColumnResponse(
        UUID id,
        String name,
        int position,
        List<BoardTaskCardResponse> tasks
) {
}
