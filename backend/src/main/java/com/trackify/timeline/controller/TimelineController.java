package com.trackify.timeline.controller;

import com.trackify.auth.application.LocalUserPrincipal;
import com.trackify.timeline.application.TimelineService;
import com.trackify.timeline.dto.TimelineResponse;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Timeline read endpoint (TASK-067).
 *
 * <p>{@code GET /api/projects/{projectId}/timeline} returns the Gantt-shaped
 * task payload for the authenticated caller. Authorization is enforced inside
 * {@link TimelineService} by delegating to the project access check — the same
 * pattern used by {@link com.trackify.board.controller.BoardController}.
 *
 * <p>Controller is thin: maps the HTTP path, resolves the principal, delegates
 * to the application service, and returns the DTO. No business logic here.
 */
@RestController
@RequestMapping("/api/projects")
public class TimelineController {

    private final TimelineService timelineService;

    public TimelineController(TimelineService timelineService) {
        this.timelineService = timelineService;
    }

    @GetMapping("/{projectId}/timeline")
    public TimelineResponse getTimeline(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal LocalUserPrincipal principal) {
        return timelineService.getTimeline(projectId, principal.userId());
    }
}
