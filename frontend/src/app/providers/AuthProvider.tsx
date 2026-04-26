import { useCallback, useMemo } from 'react';
import type { ReactNode } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { authApi } from '../../modules/auth/api/authApi';
import type { AuthStatus, AuthUser, LoginRequest } from '../../modules/auth/types/auth';
import { ApiError } from '../../shared/api/httpClient';
import { queryKeys } from '../../shared/api/queryKeys';
import { AuthContext } from './AuthContext';
import type { AuthContextValue } from './AuthContext';

async function fetchMeOrNull(): Promise<AuthUser | null> {
  try {
    return await authApi.me();
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      return null;
    }
    throw error;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();

  const meQuery = useQuery<AuthUser | null>({
    queryKey: queryKeys.auth.me,
    queryFn: fetchMeOrNull,
    staleTime: Infinity,
    retry: false,
    refetchOnWindowFocus: false,
  });

  const refresh = useCallback(async () => {
    const result = await queryClient.fetchQuery<AuthUser | null>({
      queryKey: queryKeys.auth.me,
      queryFn: fetchMeOrNull,
      staleTime: 0,
    });
    return result;
  }, [queryClient]);

  const signIn = useCallback(
    async (credentials: LoginRequest) => {
      await authApi.login(credentials);
      const user = await refresh();
      if (!user) {
        throw new ApiError(
          401,
          'SESSION_NOT_ESTABLISHED',
          'Login succeeded but no session was established.',
        );
      }
      return user;
    },
    [refresh],
  );

  const signOut = useCallback(async () => {
    try {
      await authApi.logout();
    } finally {
      queryClient.clear();
      queryClient.setQueryData<AuthUser | null>(queryKeys.auth.me, null);
    }
  }, [queryClient]);

  const value = useMemo<AuthContextValue>(() => {
    const user = meQuery.data ?? null;
    let status: AuthStatus;
    if (meQuery.isPending) {
      status = 'loading';
    } else if (user) {
      status = 'authenticated';
    } else {
      status = 'unauthenticated';
    }
    return { status, user, signIn, signOut, refresh };
  }, [meQuery.data, meQuery.isPending, signIn, signOut, refresh]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
