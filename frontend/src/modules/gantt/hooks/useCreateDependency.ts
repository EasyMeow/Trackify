import { useMutation, useQueryClient } from '@tanstack/react-query';
import { dependencyApi } from '../api/dependencyApi';
import type { Dependency } from '../types/timeline';

export interface CreateDependencyVariables {
  taskId: string;
  predecessorTaskId: string;
}

export function useCreateDependency(projectId: string | undefined) {
  const queryClient = useQueryClient();

  return useMutation<Dependency, Error, CreateDependencyVariables>({
    mutationFn: ({ taskId, predecessorTaskId }) =>
      dependencyApi.createDependency(taskId, predecessorTaskId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['projects', projectId, 'timeline'],
      });
    },
  });
}
