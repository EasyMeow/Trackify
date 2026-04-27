package com.trackify.timeline.dto;

import java.util.List;
import java.util.UUID;

/**
 * Timeline payload returned by {@code GET /api/projects/{projectId}/timeline} (TASK-067).
 *
 * <p>Top-level envelope containing the project identity and the flat list of task
 * rows the Gantt chart will render. Dependencies are deferred to TASK-073.
 */
public record TimelineResponse(
        UUID projectId,
        List<TimelineTaskResponse> tasks
) {
}
