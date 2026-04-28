import { useMutation, useQueryClient } from '@tanstack/react-query';
import { boardApi } from '../api/boardApi';
import { addErrorToast } from '../../../shared/state/toastStore';

export function useReorderColumns(projectId: string | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (columnIds: string[]) => {
      if (!projectId) return Promise.reject(new Error('No project selected'));
      return boardApi.reorderColumns(projectId, columnIds);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['projects', projectId, 'board'] });
    },
    onError: (error: Error) => {
      addErrorToast(error.message || 'Failed to reorder columns.');
    },
  });
}
