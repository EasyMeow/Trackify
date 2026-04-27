import type { TaskPriority } from '../../kanban/types/task';

export interface UpdateTaskRequest {
  title?: string;
  description?: string | null;
  priority?: TaskPriority;
  startDate?: string | null;
  dueDate?: string | null;
}
