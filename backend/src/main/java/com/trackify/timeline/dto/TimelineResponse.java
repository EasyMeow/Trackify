package com.trackify.timeline.dto;

import com.trackify.task.dto.DependencyResponse;

import java.util.List;
import java.util.UUID;

/**
 * Timeline payload returned by {@code GET /api/projects/{projectId}/timeline} (TASK-067, TASK-073).
 *
 * <p>Top-level envelope containing the project identity, the flat list of task
 * rows the Gantt chart will render, and the flat list of dependency edges
 * within the project.
 *
 * <p>Dependencies are exposed as a top-level list rather than per-task arrays
 * so the frontend has access to each edge's {@code id} (needed to call
 * {@code DELETE /api/tasks/{taskId}/dependencies/{dependencyId}} from TASK-075)
 * without an extra round-trip.
 */
public record TimelineResponse(
        UUID projectId,
        List<TimelineTaskResponse> tasks,
        List<DependencyResponse> dependencies
) {
}
