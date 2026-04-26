import { createContext } from 'react';
import type { AuthStatus, AuthUser, LoginRequest } from '../../modules/auth/types/auth';

export interface AuthContextValue {
  status: AuthStatus;
  user: AuthUser | null;
  signIn: (credentials: LoginRequest) => Promise<AuthUser>;
  signOut: () => Promise<void>;
  refresh: () => Promise<AuthUser | null>;
}

export const AuthContext = createContext<AuthContextValue | null>(null);
