import { httpClient } from '../../../shared/api/httpClient';
import type { BoardResponse } from '../types/board';

export const boardApi = {
  getBoard(projectId: string): Promise<BoardResponse> {
    return httpClient.get<BoardResponse>(`/projects/${projectId}/board`);
  },
};
