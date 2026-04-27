package com.trackify.timeline.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Single task row in the Gantt timeline payload (TASK-067).
 *
 * <p>Contains only the fields the Gantt view needs: identity, display name,
 * status, priority, and the date range that positions the bar on the chart.
 * Description, comments, and sort-order are intentionally omitted — the
 * timeline is a read-model shaped for the UI, not a full task projection.
 *
 * <p>Dependencies are deferred to TASK-073.
 */
public record TimelineTaskResponse(
        UUID id,
        String title,
        String status,
        String priority,
        LocalDate startDate,
        LocalDate dueDate
) {
}
