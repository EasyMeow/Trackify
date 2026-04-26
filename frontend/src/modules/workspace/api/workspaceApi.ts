import { httpClient } from '../../../shared/api/httpClient';
import type { Workspace } from '../types/workspace';

export const workspaceApi = {
  list(): Promise<Workspace[]> {
    return httpClient.get<Workspace[]>('/workspaces');
  },
};
