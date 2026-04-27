import { httpClient } from '../../../shared/api/httpClient';
import type { CommentResponse } from '../types/comment';

export const commentApi = {
  listComments(taskId: string): Promise<CommentResponse[]> {
    return httpClient.get<CommentResponse[]>(`/tasks/${taskId}/comments`);
  },
};
