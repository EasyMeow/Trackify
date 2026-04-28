import { useMutation, useQueryClient } from '@tanstack/react-query';
import { authApi } from '../api/authApi';
import { queryKeys } from '../../../shared/api/queryKeys';
import type { ApiError } from '../../../shared/api/httpClient';
import type { AuthUser, RegisterRequest } from '../types/auth';

export function useRegister() {
  const queryClient = useQueryClient();

  return useMutation<AuthUser, ApiError, RegisterRequest>({
    mutationFn: (payload) => authApi.register(payload),
    onSuccess: (user) => {
      queryClient.setQueryData(queryKeys.auth.me, user);
    },
  });
}
