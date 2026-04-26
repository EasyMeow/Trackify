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

export type AuthStatus = 'loading' | 'authenticated' | 'unauthenticated';
