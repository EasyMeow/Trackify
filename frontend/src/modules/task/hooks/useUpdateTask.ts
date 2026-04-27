import { useMutation, useQueryClient } from '@tanstack/react-query';
import { taskDetailApi } from '../api/taskDetailApi';
import type { UpdateTaskRequest } from '../types/updateTask';
import type { TaskResponse } from '../../kanban/types/task';
import { addErrorToast } from '../../../shared/state/toastStore';

export function useUpdateTask(taskId: string, projectId: string) {
  const queryClient = useQueryClient();

  return useMutation<TaskResponse, Error, UpdateTaskRequest>({
    mutationFn: (request) => taskDetailApi.updateTask(taskId, request),
    onSuccess: (updatedTask) => {
      queryClient.setQueryData(['tasks', taskId], updatedTask);
      queryClient.invalidateQueries({ queryKey: ['projects', projectId, 'board'] });
    },
    onError: (error) => {
      addErrorToast(error.message || 'Failed to save task changes. Please try again.');
    },
  });
}
