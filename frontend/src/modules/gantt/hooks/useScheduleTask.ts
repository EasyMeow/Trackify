import { useMutation, useQueryClient } from '@tanstack/react-query';
import { scheduleApi } from '../api/scheduleApi';
import type { ScheduleTaskRequest } from '../api/scheduleApi';
import type { TaskResponse } from '../../kanban/types/task';

export interface ScheduleTaskVariables extends ScheduleTaskRequest {
  taskId: string;
}

export function useScheduleTask(projectId: string | undefined) {
  const queryClient = useQueryClient();

  return useMutation<TaskResponse, Error, ScheduleTaskVariables>({
    mutationFn: ({ taskId, startDate, dueDate }) =>
      scheduleApi.scheduleTask(taskId, { startDate, dueDate }),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['projects', projectId, 'timeline'],
      });
      queryClient.invalidateQueries({
        queryKey: ['projects', projectId, 'board'],
      });
    },
  });
}
