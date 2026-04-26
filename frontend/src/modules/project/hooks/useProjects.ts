import { useQuery } from '@tanstack/react-query';
import { projectApi } from '../api/projectApi';
import type { Project } from '../types/project';

export function useProjects(workspaceId: string | undefined) {
  return useQuery<Project[]>({
    queryKey: ['workspaces', workspaceId, 'projects'],
    queryFn: () => projectApi.listForWorkspace(workspaceId as string),
    enabled: Boolean(workspaceId),
  });
}
