import { httpClient } from '../../../shared/api/httpClient';
import type { TaskResponse } from '../../kanban/types/task';

export interface ScheduleTaskRequest {
  startDate: string | null;
  dueDate: string | null;
}

export const scheduleApi = {
  scheduleTask(taskId: string, request: ScheduleTaskRequest): Promise<TaskResponse> {
    return httpClient.patch<TaskResponse>(`/tasks/${taskId}/schedule`, request);
  },
};
