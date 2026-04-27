import { useQuery } from '@tanstack/react-query';
import { taskDetailApi } from '../api/taskDetailApi';
import type { TaskResponse } from '../../kanban/types/task';

export function useTaskDetail(taskId: string | null) {
  return useQuery<TaskResponse>({
    queryKey: ['tasks', taskId],
    queryFn: () => taskDetailApi.getTask(taskId!),
    enabled: !!taskId,
  });
}
