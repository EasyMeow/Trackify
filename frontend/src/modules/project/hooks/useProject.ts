import { useQuery } from '@tanstack/react-query';
import { projectApi } from '../api/projectApi';
import type { Project } from '../types/project';

export function useProject(projectId: string | undefined) {
  return useQuery<Project>({
    queryKey: ['projects', projectId],
    queryFn: () => projectApi.getById(projectId as string),
    enabled: Boolean(projectId),
  });
}
