import { useMutation, useQueryClient } from '@tanstack/react-query';
import { taskApi } from '../api/taskApi';
import type { CreateTaskRequest } from '../api/taskApi';
import { addErrorToast } from '../../../shared/state/toastStore';

export function useCreateTask(projectId: string | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: CreateTaskRequest) => {
      if (!projectId) {
        return Promise.reject(new Error('No project selected'));
      }
      return taskApi.createTask(projectId, request);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['projects', projectId, 'board'],
      });
    },
    onError: (error: Error) => {
      addErrorToast(error.message || 'Failed to create task. Please try again.');
    },
  });
}
