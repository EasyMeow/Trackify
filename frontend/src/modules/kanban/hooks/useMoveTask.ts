import { useMutation, useQueryClient } from '@tanstack/react-query';
import { taskApi } from '../api/taskApi';
import type { MoveTaskRequest } from '../api/taskApi';
import type { TaskResponse } from '../types/task';
import type { BoardResponse } from '../types/board';
import { addErrorToast } from '../../../shared/state/toastStore';

export interface MoveTaskVariables extends MoveTaskRequest {
  taskId: string;
}

export function useMoveTask(projectId: string | undefined) {
  const queryClient = useQueryClient();

  return useMutation<TaskResponse, Error, MoveTaskVariables>({
    mutationFn: ({ taskId, columnId, sortOrder }) =>
      taskApi.moveTask(taskId, { columnId, sortOrder }),
    onSuccess: (updatedTask, { taskId, columnId }) => {
      queryClient.setQueryData(['tasks', taskId], updatedTask);
      // Immediately update the board cache so the card moves without waiting for a refetch.
      queryClient.setQueryData<BoardResponse>(
        ['projects', projectId, 'board'],
        (old) => {
          if (!old) return old;
          let movedCard = old.columns.flatMap((c) => c.tasks).find((t) => t.id === taskId);
          if (!movedCard) return old;
          movedCard = { ...movedCard, status: updatedTask.status, sortOrder: updatedTask.sortOrder };
          return {
            ...old,
            columns: old.columns.map((col) => {
              const withoutTask = col.tasks.filter((t) => t.id !== taskId);
              if (col.id !== columnId) return { ...col, tasks: withoutTask };
              return { ...col, tasks: [...withoutTask, movedCard!] };
            }),
          };
        }
      );
      // Background refetch to ensure eventual consistency.
      queryClient.invalidateQueries({ queryKey: ['projects', projectId, 'board'] });
    },
    onError: (error) => {
      addErrorToast(error.message || 'Failed to move task. Please try again.');
    },
  });
}
