export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'IN_REVIEW' | 'DONE' | 'CANCELLED';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';

export interface BoardTaskCard {
  id: string;
  title: string;
  status: TaskStatus;
  priority: TaskPriority;
  sortOrder: number;
  startDate: string | null;
  dueDate: string | null;
}

export interface BoardColumn {
  id: string;
  name: string;
  position: number;
  tasks: BoardTaskCard[];
}

export interface BoardResponse {
  projectId: string;
  columns: BoardColumn[];
}
