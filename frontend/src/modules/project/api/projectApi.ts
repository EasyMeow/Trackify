import { httpClient } from '../../../shared/api/httpClient';
import type { Project } from '../types/project';

export interface ProjectCreateRequest {
  name: string;
  slug?: string;
  description?: string;
}

export const projectApi = {
  listForWorkspace(workspaceId: string): Promise<Project[]> {
    return httpClient.get<Project[]>(`/workspaces/${workspaceId}/projects`);
  },

  create(workspaceId: string, request: ProjectCreateRequest): Promise<Project> {
    const body: ProjectCreateRequest = { name: request.name };
    if (request.slug && request.slug.trim()) body.slug = request.slug.trim();
    if (request.description && request.description.trim()) body.description = request.description.trim();
    return httpClient.post<Project>(`/workspaces/${workspaceId}/projects`, body);
  },
};
