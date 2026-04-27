import type { Task as GanttTask } from 'gantt-task-react';
import type { TimelineTask } from '../types/timeline';

export interface MappedTimeline {
  ganttTasks: GanttTask[];
  /** Tasks excluded because startDate or dueDate is null — gantt-task-react requires both. */
  datelessCount: number;
}

export function mapTimelineToGantt(tasks: TimelineTask[]): MappedTimeline {
  const ganttTasks: GanttTask[] = [];
  let datelessCount = 0;

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

    ganttTasks.push({
      id: t.id,
      type: 'task',
      name: t.title,
      start,
      end,
      progress: 0,
      isDisabled: true,
    });
  }

  return { ganttTasks, datelessCount };
}
