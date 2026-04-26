import { httpClient } from '../../../shared/api/httpClient';
import type { Project } from '../types/project';

export const projectApi = {
  listForWorkspace(workspaceId: string): Promise<Project[]> {
    return httpClient.get<Project[]>(`/workspaces/${workspaceId}/projects`);
  },
};
