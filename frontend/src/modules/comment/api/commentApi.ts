import { httpClient } from '../../../shared/api/httpClient';
import type { CommentResponse, CreateCommentRequest } from '../types/comment';

export const commentApi = {
  listComments(taskId: string): Promise<CommentResponse[]> {
    return httpClient.get<CommentResponse[]>(`/tasks/${taskId}/comments`);
  },

  createComment(taskId: string, request: CreateCommentRequest): Promise<CommentResponse> {
    return httpClient.post<CommentResponse>(`/tasks/${taskId}/comments`, request);
  },
};
