import { httpClient } from '../../../shared/api/httpClient';
import type { TaskResponse } from '../types/task';

export interface CreateTaskRequest {
  title: string;
  description?: string;
  priority?: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
  startDate?: string;
  dueDate?: string;
}

export const taskApi = {
  createTask(projectId: string, request: CreateTaskRequest): Promise<TaskResponse> {
    const body: CreateTaskRequest = { title: request.title.trim() };
    if (request.description && request.description.trim()) {
      body.description = request.description.trim();
    }
    if (request.priority) body.priority = request.priority;
    if (request.startDate) body.startDate = request.startDate;
    if (request.dueDate) body.dueDate = request.dueDate;
    return httpClient.post<TaskResponse>(`/projects/${projectId}/tasks`, body);
  },
};
