import { httpClient } from '../../../shared/api/httpClient';
import type { Workspace } from '../types/workspace';

export const workspaceApi = {
  list(): Promise<Workspace[]> {
    return httpClient.get<Workspace[]>('/workspaces');
  },
  create(name: string): Promise<Workspace> {
    return httpClient.post<Workspace>('/workspaces', { name });
  },
  update(id: string, name: string): Promise<Workspace> {
    return httpClient.patch<Workspace>(`/workspaces/${id}`, { name });
  },
  delete(id: string): Promise<void> {
    return httpClient.del<void>(`/workspaces/${id}`);
  },
};
