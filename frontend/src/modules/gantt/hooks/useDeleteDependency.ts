import { useMutation, useQueryClient } from '@tanstack/react-query';
import { dependencyApi } from '../api/dependencyApi';

export interface DeleteDependencyVariables {
  taskId: string;
  dependencyId: string;
}

export function useDeleteDependency(projectId: string | undefined) {
  const queryClient = useQueryClient();

  return useMutation<void, Error, DeleteDependencyVariables>({
    mutationFn: ({ taskId, dependencyId }) =>
      dependencyApi.deleteDependency(taskId, dependencyId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['projects', projectId, 'timeline'],
      });
    },
  });
}
