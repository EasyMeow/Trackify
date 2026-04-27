import { useMutation, useQueryClient } from '@tanstack/react-query';
import { commentApi } from '../api/commentApi';
import type { CreateCommentRequest, CommentResponse } from '../types/comment';

export function useCreateComment(taskId: string) {
  const queryClient = useQueryClient();

  return useMutation<CommentResponse, Error, CreateCommentRequest>({
    mutationFn: (request) => commentApi.createComment(taskId, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks', taskId, 'comments'] });
    },
  });
}
