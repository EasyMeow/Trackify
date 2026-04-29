import { useMutation, useQueryClient } from '@tanstack/react-query';
import { workspaceApi } from '../api/workspaceApi';
import type { Workspace } from '../types/workspace';
import { addErrorToast } from '../../../shared/state/toastStore';

export function useDeleteWorkspace() {
  const queryClient = useQueryClient();

  return useMutation<void, Error, string>({
    mutationFn: (id) => workspaceApi.delete(id),
    onSuccess: (_, deletedId) => {
      queryClient.setQueryData<Workspace[]>(['workspaces'], (prev = []) =>
        prev.filter((w) => w.id !== deletedId)
      );
    },
    onError: (error) => {
      addErrorToast(error.message || 'Failed to delete workspace. Please try again.');
    },
  });
}
