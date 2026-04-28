import { useMutation, useQueryClient } from '@tanstack/react-query';
import { boardApi } from '../api/boardApi';
import { addErrorToast } from '../../../shared/state/toastStore';

interface RenameColumnVars {
  columnId: string;
  name: string;
}

export function useRenameColumn(projectId: string | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ columnId, name }: RenameColumnVars) => {
      if (!projectId) return Promise.reject(new Error('No project selected'));
      return boardApi.renameColumn(projectId, columnId, name);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['projects', projectId, 'board'] });
    },
    onError: (error: Error) => {
      addErrorToast(error.message || 'Failed to rename column.');
    },
  });
}
