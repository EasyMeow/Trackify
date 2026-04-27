package com.trackify.board.controller;

import com.trackify.auth.application.LocalUserPrincipal;
import com.trackify.board.application.BoardQueryService;
import com.trackify.board.dto.BoardResponse;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Board read endpoint (TASK-047).
 *
 * <p>{@code GET /api/projects/{projectId}/board} returns columns and their
 * task cards for the authenticated caller. Authorization is enforced inside
 * {@link BoardQueryService} by delegating to the project access check;
 * missing/forbidden projects surface as 404/403 via {@link com.trackify.common.exception.GlobalExceptionHandler}.
 *
 * <p>Controller is thin: maps the HTTP path, resolves the principal, delegates
 * to the application service, and returns the DTO. No business logic here.
 */
@RestController
@RequestMapping("/api/projects")
public class BoardController {

    private final BoardQueryService boardQueryService;

    public BoardController(BoardQueryService boardQueryService) {
        this.boardQueryService = boardQueryService;
    }

    @GetMapping("/{projectId}/board")
    public BoardResponse getBoard(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal LocalUserPrincipal principal) {
        return boardQueryService.getBoard(projectId, principal.userId());
    }
}
