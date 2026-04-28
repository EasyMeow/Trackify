import { httpClient } from '../../../shared/api/httpClient';
import type { BoardResponse } from '../types/board';

export interface ColumnResponse {
  id: string;
  projectId: string;
  name: string;
  position: number;
  createdAt: string;
  updatedAt: string;
}

export const boardApi = {
  getBoard(projectId: string): Promise<BoardResponse> {
    return httpClient.get<BoardResponse>(`/projects/${projectId}/board`);
  },

  createColumn(projectId: string, name: string): Promise<ColumnResponse> {
    return httpClient.post<ColumnResponse>(`/projects/${projectId}/columns`, { name });
  },

  renameColumn(projectId: string, columnId: string, name: string): Promise<ColumnResponse> {
    return httpClient.patch<ColumnResponse>(`/projects/${projectId}/columns/${columnId}`, { name });
  },

  deleteColumn(projectId: string, columnId: string): Promise<void> {
    return httpClient.del<void>(`/projects/${projectId}/columns/${columnId}`);
  },

  reorderColumns(projectId: string, columnIds: string[]): Promise<ColumnResponse[]> {
    return httpClient.patch<ColumnResponse[]>(`/projects/${projectId}/columns/order`, { columnIds });
  },
};
