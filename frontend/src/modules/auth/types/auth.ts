export interface AuthUser {
  id: string;
  login: string;
  email: string;
  displayName: string;
}

export interface LoginRequest {
  login: string;
  password: string;
}

export interface LoginResponse {
  userId: string;
  login: string;
}

export interface LogoutResponse {
  success: boolean;
}

export interface RegisterRequest {
  login: string;
  email: string;
  displayName: string;
  password: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface ChangePasswordResponse {
  message: string;
}

export type AuthStatus = 'loading' | 'authenticated' | 'unauthenticated';
