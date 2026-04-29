import { useMutation } from '@tanstack/react-query';
import { authApi } from '../api/authApi';
import type { ApiError } from '../../../shared/api/httpClient';
import type { ChangePasswordRequest, ChangePasswordResponse } from '../types/auth';

export function useChangePassword() {
  return useMutation<ChangePasswordResponse, ApiError, ChangePasswordRequest>({
    mutationFn: (payload) => authApi.changePassword(payload),
  });
}
