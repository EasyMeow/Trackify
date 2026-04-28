import { useMutation, useQueryClient } from '@tanstack/react-query';
import { taskDetailApi } from '../api/taskDetailApi';
import { addErrorToast } from '../../../shared/state/toastStore';

export function useDeleteTask(taskId: string, projectId: string) {
  const queryClient = useQueryClient();

  return useMutation<void, Error, void>({
    mutationFn: () => taskDetailApi.deleteTask(taskId),
    onSuccess: () => {
      queryClient.removeQueries({ queryKey: ['tasks', taskId] });
      queryClient.invalidateQueries({ queryKey: ['projects', projectId, 'board'] });
      queryClient.invalidateQueries({ queryKey: ['projects', projectId, 'timeline'] });
    },
    onError: (error) => {
      addErrorToast(error.message || 'Failed to delete task. Please try again.');
    },
  });
}
