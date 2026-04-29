import { useMutation, useQueryClient } from '@tanstack/react-query';
import { authApi } from '../api/authApi';
import { queryKeys } from '../../../shared/api/queryKeys';
import { useAvatarStore } from '../../../shared/state/avatarStore';
import type { ApiError } from '../../../shared/api/httpClient';
import type { AuthUser } from '../types/auth';

export function useUpdateMe() {
  const queryClient = useQueryClient();
  const bumpVersion = useAvatarStore((s) => s.bumpVersion);

  return useMutation<AuthUser, ApiError, FormData>({
    mutationFn: (formData) => authApi.updateMe(formData),
    onSuccess: (user, formData) => {
      queryClient.setQueryData(queryKeys.auth.me, user);
      if (formData.has('avatar')) {
        bumpVersion();
      }
    },
  });
}
