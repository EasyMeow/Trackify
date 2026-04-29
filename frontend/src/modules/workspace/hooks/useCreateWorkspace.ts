import { useMutation, useQueryClient } from '@tanstack/react-query';
import { workspaceApi } from '../api/workspaceApi';
import type { Workspace } from '../types/workspace';
import { addErrorToast } from '../../../shared/state/toastStore';

export function useCreateWorkspace() {
  const queryClient = useQueryClient();

  return useMutation<Workspace, Error, string>({
    mutationFn: (name) => workspaceApi.create(name),
    onSuccess: (newWorkspace) => {
      queryClient.setQueryData<Workspace[]>(['workspaces'], (prev = []) => [
        ...prev,
        newWorkspace,
      ]);
    },
    onError: (error) => {
      addErrorToast(error.message || 'Failed to create workspace. Please try again.');
    },
  });
}
