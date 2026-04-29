import { useMutation, useQueryClient } from '@tanstack/react-query';
import { workspaceApi } from '../api/workspaceApi';
import type { Workspace } from '../types/workspace';
import { addErrorToast } from '../../../shared/state/toastStore';

export function useUpdateWorkspace() {
  const queryClient = useQueryClient();

  return useMutation<Workspace, Error, { id: string; name: string }>({
    mutationFn: ({ id, name }) => workspaceApi.update(id, name),
    onSuccess: (updated) => {
      queryClient.setQueryData<Workspace[]>(['workspaces'], (prev = []) =>
        prev.map((w) => (w.id === updated.id ? updated : w))
      );
    },
    onError: (error) => {
      addErrorToast(error.message || 'Failed to rename workspace. Please try again.');
    },
  });
}
