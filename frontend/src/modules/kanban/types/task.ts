import type { TaskStatus, TaskPriority } from './board';

export type { TaskStatus, TaskPriority };

export interface TaskResponse {
  id: string;
  projectId: string;
  columnId: string;
  createdBy: string;
  title: string;
  description: string | null;
  status: TaskStatus;
  priority: TaskPriority;
  sortOrder: number;
  startDate: string | null;
  dueDate: string | null;
  estimatedHours: number | null;
  createdAt: string;
  updatedAt: string;
}
