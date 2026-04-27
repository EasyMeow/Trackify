import { useQuery } from '@tanstack/react-query';
import { commentApi } from '../api/commentApi';
import type { CommentResponse } from '../types/comment';

export function useComments(taskId: string | null) {
  return useQuery<CommentResponse[]>({
    queryKey: ['tasks', taskId, 'comments'],
    queryFn: () => commentApi.listComments(taskId!),
    enabled: !!taskId,
  });
}
