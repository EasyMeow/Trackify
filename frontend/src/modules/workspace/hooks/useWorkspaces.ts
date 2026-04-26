import { useQuery } from '@tanstack/react-query';
import { workspaceApi } from '../api/workspaceApi';
import type { Workspace } from '../types/workspace';

export function useWorkspaces() {
  return useQuery<Workspace[]>({
    queryKey: ['workspaces'],
    queryFn: () => workspaceApi.list(),
  });
}
