import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../modules/auth/hooks/useAuth';

function AuthLoadingShell() {
  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: 'var(--color-bg)',
        color: 'var(--color-text-subtle)',
        fontSize: 'var(--font-size-sm)',
      }}
    >
      Loading…
    </div>
  );
}

export function RequireAuth({ children }: { children: ReactNode }) {
  const { status } = useAuth();
  const location = useLocation();

  if (status === 'loading') {
    return <AuthLoadingShell />;
  }
  if (status === 'unauthenticated') {
    return <Navigate to="/sign-in" replace state={{ from: location.pathname }} />;
  }
  return <>{children}</>;
}

export function RequireAnonymous({ children }: { children: ReactNode }) {
  const { status } = useAuth();

  if (status === 'loading') {
    return <AuthLoadingShell />;
  }
  if (status === 'authenticated') {
    return <Navigate to="/" replace />;
  }
  return <>{children}</>;
}
