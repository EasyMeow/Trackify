import { useMutation, useQueryClient } from '@tanstack/react-query';
import { projectApi } from '../api/projectApi';
import { addErrorToast } from '../../../shared/state/toastStore';

export function useDeleteProject(projectId: string, workspaceId: string) {
  const queryClient = useQueryClient();

  return useMutation<void, Error, void>({
    mutationFn: () => projectApi.delete(projectId),
    onSuccess: () => {
      queryClient.removeQueries({ queryKey: ['projects', projectId] });
      queryClient.invalidateQueries({
        queryKey: ['workspaces', workspaceId, 'projects'],
      });
    },
    onError: (error) => {
      addErrorToast(error.message || 'Failed to delete project. Please try again.');
    },
  });
}
