import type { Task as GanttTask } from 'gantt-task-react';
import type { TimelineTask, Dependency } from '../types/timeline';

export interface MappedTimeline {
  ganttTasks: GanttTask[];
  /** Tasks excluded because startDate or dueDate is null — gantt-task-react requires both. */
  datelessCount: number;
}

export function mapTimelineToGantt(
  tasks: TimelineTask[],
  dependencies: Dependency[] = [],
): MappedTimeline {
  const ganttTasks: GanttTask[] = [];
  let datelessCount = 0;

  // Track which task IDs made it into the rendered list (have valid dates).
  const renderedIds = new Set<string>();

  // First pass: collect rendered IDs so we can filter dangling dependency edges.
  for (const t of tasks) {
    if (t.startDate && t.dueDate) {
      renderedIds.add(t.id);
    }
  }

  // Build successor → predecessorIds map, dropping edges whose predecessor has
  // no bar to point from (dateless tasks were filtered out above).
  const depsMap = new Map<string, string[]>();
  for (const dep of dependencies) {
    if (!renderedIds.has(dep.predecessorTaskId)) {
      continue; // predecessor has no bar — drop silently
    }
    const existing = depsMap.get(dep.successorTaskId);
    if (existing) {
      existing.push(dep.predecessorTaskId);
    } else {
      depsMap.set(dep.successorTaskId, [dep.predecessorTaskId]);
    }
  }

  for (const t of tasks) {
    if (!t.startDate || !t.dueDate) {
      datelessCount++;
      continue;
    }

    const start = new Date(t.startDate);
    const end = new Date(t.dueDate);

    // gantt-task-react crashes when end <= start; nudge end forward one day.
    if (end <= start) {
      end.setDate(end.getDate() + 1);
    }

    const predecessors = depsMap.get(t.id);

    ganttTasks.push({
      id: t.id,
      type: 'task',
      name: t.title,
      start,
      end,
      progress: 0,
      ...(predecessors ? { dependencies: predecessors } : {}),
    });
  }

  return { ganttTasks, datelessCount };
}
