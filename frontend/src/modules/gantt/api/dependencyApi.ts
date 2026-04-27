import { httpClient } from '../../../shared/api/httpClient';
import type { Dependency } from '../types/timeline';

export const dependencyApi = {
  createDependency(taskId: string, predecessorTaskId: string): Promise<Dependency> {
    return httpClient.post<Dependency>(`/tasks/${taskId}/dependencies`, {
      predecessorTaskId,
    });
  },
  deleteDependency(taskId: string, dependencyId: string): Promise<void> {
    return httpClient.del<void>(`/tasks/${taskId}/dependencies/${dependencyId}`);
  },
};
