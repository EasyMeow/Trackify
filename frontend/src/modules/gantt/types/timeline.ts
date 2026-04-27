import type { TaskStatus, TaskPriority } from '../../kanban/types/board';

export type { TaskStatus, TaskPriority };

export interface TimelineTask {
  id: string;
  title: string;
  status: TaskStatus;
  priority: TaskPriority;
  startDate: string | null;
  dueDate: string | null;
}

export interface TimelineResponse {
  projectId: string;
  tasks: TimelineTask[];
}
