import { httpClient } from '../../../shared/api/httpClient';
import type {
  AuthUser,
  LoginRequest,
  LoginResponse,
  LogoutResponse,
} from '../types/auth';

export const authApi = {
  me(): Promise<AuthUser> {
    return httpClient.get<AuthUser>('/me');
  },
  login(payload: LoginRequest): Promise<LoginResponse> {
    return httpClient.post<LoginResponse>('/auth/login', payload);
  },
  logout(): Promise<LogoutResponse> {
    return httpClient.post<LogoutResponse>('/logout');
  },
};
