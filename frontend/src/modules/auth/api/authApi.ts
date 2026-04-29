import { httpClient } from '../../../shared/api/httpClient';
import type {
  AuthUser,
  ChangePasswordRequest,
  ChangePasswordResponse,
  LoginRequest,
  LoginResponse,
  LogoutResponse,
  RegisterRequest,
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
  register(payload: RegisterRequest): Promise<AuthUser> {
    return httpClient.post<AuthUser>('/auth/register', payload);
  },
  updateMe(formData: FormData): Promise<AuthUser> {
    return httpClient.patchFormData<AuthUser>('/me', formData);
  },
  changePassword(payload: ChangePasswordRequest): Promise<ChangePasswordResponse> {
    return httpClient.post<ChangePasswordResponse>('/me/password', payload);
  },
};
