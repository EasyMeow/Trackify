import { httpClient } from '../../../shared/api/httpClient';
import type { TaskResponse } from '../../kanban/types/task';

export const taskDetailApi = {
  getTask(taskId: string): Promise<TaskResponse> {
    return httpClient.get<TaskResponse>(`/tasks/${taskId}`);
  },
};
