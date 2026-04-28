package com.trackify.board.controller;

import com.trackify.auth.application.LocalUserPrincipal;
import com.trackify.board.application.BoardColumnService;
import com.trackify.board.dto.ColumnResponse;
import com.trackify.board.dto.CreateColumnRequest;
import com.trackify.board.dto.RenameColumnRequest;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * Board column management endpoints (TASK-098+).
 *
 * <p>{@code POST /api/projects/{projectId}/columns} — append a new column to the board.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/columns")
public class ColumnController {

    private final BoardColumnService boardColumnService;

    public ColumnController(BoardColumnService boardColumnService) {
        this.boardColumnService = boardColumnService;
    }

    @PostMapping
    public ResponseEntity<ColumnResponse> createColumn(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateColumnRequest request,
            @AuthenticationPrincipal LocalUserPrincipal principal) {
        ColumnResponse response = boardColumnService.createColumn(projectId, principal.userId(), request.name());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @DeleteMapping("/{columnId}")
    public ResponseEntity<Void> deleteColumn(
            @PathVariable UUID projectId,
            @PathVariable UUID columnId,
            @AuthenticationPrincipal LocalUserPrincipal principal) {
        boardColumnService.deleteColumn(projectId, columnId, principal.userId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{columnId}")
    public ResponseEntity<ColumnResponse> renameColumn(
            @PathVariable UUID projectId,
            @PathVariable UUID columnId,
            @Valid @RequestBody RenameColumnRequest request,
            @AuthenticationPrincipal LocalUserPrincipal principal) {
        ColumnResponse response = boardColumnService.renameColumn(
                projectId, columnId, principal.userId(), request.name());
        return ResponseEntity.ok(response);
    }
}
