import { httpClient } from '../../../shared/api/httpClient';
import type { TaskResponse } from '../../kanban/types/task';
import type { UpdateTaskRequest } from '../types/updateTask';

export const taskDetailApi = {
  getTask(taskId: string): Promise<TaskResponse> {
    return httpClient.get<TaskResponse>(`/tasks/${taskId}`);
  },
  updateTask(taskId: string, request: UpdateTaskRequest): Promise<TaskResponse> {
    return httpClient.patch<TaskResponse>(`/tasks/${taskId}`, request);
  },
  deleteTask(taskId: string): Promise<void> {
    return httpClient.del<void>(`/tasks/${taskId}`);
  },
};
