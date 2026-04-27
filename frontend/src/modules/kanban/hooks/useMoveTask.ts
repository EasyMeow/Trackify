import { useMutation, useQueryClient } from '@tanstack/react-query';
import { taskApi } from '../api/taskApi';
import type { MoveTaskRequest } from '../api/taskApi';
import type { TaskResponse } from '../types/task';

export interface MoveTaskVariables extends MoveTaskRequest {
  taskId: string;
}

export function useMoveTask(projectId: string | undefined) {
  const queryClient = useQueryClient();

  return useMutation<TaskResponse, Error, MoveTaskVariables>({
    mutationFn: ({ taskId, columnId, sortOrder }) =>
      taskApi.moveTask(taskId, { columnId, sortOrder }),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['projects', projectId, 'board'],
      });
    },
  });
}
