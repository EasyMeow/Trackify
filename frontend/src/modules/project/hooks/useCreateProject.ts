import { useMutation, useQueryClient } from '@tanstack/react-query';
import { projectApi } from '../api/projectApi';
import type { ProjectCreateRequest } from '../api/projectApi';

export function useCreateProject(workspaceId: string | undefined) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: ProjectCreateRequest) => {
      if (!workspaceId) {
        return Promise.reject(new Error('No workspace selected'));
      }
      return projectApi.create(workspaceId, request);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['workspaces', workspaceId, 'projects'],
      });
    },
  });
}
