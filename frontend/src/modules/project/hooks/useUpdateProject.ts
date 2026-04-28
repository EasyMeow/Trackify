import { useMutation, useQueryClient } from '@tanstack/react-query';
import { projectApi } from '../api/projectApi';
import type { ProjectUpdateRequest } from '../api/projectApi';
import type { Project } from '../types/project';
import { addErrorToast } from '../../../shared/state/toastStore';

export function useUpdateProject(projectId: string) {
  const queryClient = useQueryClient();

  return useMutation<Project, Error, ProjectUpdateRequest>({
    mutationFn: (request) => projectApi.update(projectId, request),
    onSuccess: (updatedProject) => {
      queryClient.setQueryData(['projects', projectId], updatedProject);
      queryClient.invalidateQueries({
        queryKey: ['workspaces', updatedProject.workspaceId, 'projects'],
      });
    },
    onError: (error) => {
      addErrorToast(error.message || 'Failed to save project changes. Please try again.');
    },
  });
}
