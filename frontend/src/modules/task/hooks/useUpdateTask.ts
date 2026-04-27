import { useMutation, useQueryClient } from '@tanstack/react-query';
import { taskDetailApi } from '../api/taskDetailApi';
import type { UpdateTaskRequest } from '../types/updateTask';
import type { TaskResponse } from '../../kanban/types/task';

export function useUpdateTask(taskId: string, projectId: string) {
  const queryClient = useQueryClient();

  return useMutation<TaskResponse, Error, UpdateTaskRequest>({
    mutationFn: (request) => taskDetailApi.updateTask(taskId, request),
    onSuccess: (updatedTask) => {
      queryClient.setQueryData(['tasks', taskId], updatedTask);
      queryClient.invalidateQueries({ queryKey: ['projects', projectId, 'board'] });
    },
  });
}
