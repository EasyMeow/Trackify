import { useMutation, useQueryClient } from '@tanstack/react-query';
import { scheduleApi } from '../api/scheduleApi';
import type { ScheduleTaskRequest } from '../api/scheduleApi';
import type { TaskResponse } from '../../kanban/types/task';
import { addErrorToast } from '../../../shared/state/toastStore';

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
    onError: (error) => {
      addErrorToast(error.message || 'Failed to update task schedule. Please try again.');
    },
  });
}
