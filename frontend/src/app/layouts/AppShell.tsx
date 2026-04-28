import { useState } from 'react';
import type { ReactNode } from 'react';
import { useAuth } from '../../modules/auth/hooks/useAuth';
import { WorkspaceSwitcher } from '../../modules/workspace/components/WorkspaceSwitcher';
import { ToastViewport } from '../../shared/components/ToastViewport';
import { addErrorToast } from '../../shared/state/toastStore';

function SignOutControl() {
  const { user, signOut } = useAuth();
  const [pending, setPending] = useState(false);

  const handleClick = async () => {
    if (pending) {
      return;
    }
    setPending(true);
    try {
      await signOut();
    } catch (error) {
      const message =
        error instanceof Error && error.message ? error.message : 'Failed to sign out.';
      addErrorToast(message);
      setPending(false);
    }
  };

  return (
    <div
      style={{
        marginTop: 'auto',
        display: 'flex',
        flexDirection: 'column',
        gap: 'var(--space-2)',
      }}
    >
      {user && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '2px' }}>
          <span
            style={{
              fontSize: 'var(--font-size-sm)',
              fontWeight: 'var(--font-weight-medium)',
              color: 'var(--color-text)',
            }}
          >
            {user.displayName}
          </span>
          <span
            style={{
              fontSize: 'var(--font-size-xs)',
              color: 'var(--color-text-subtle)',
            }}
          >
            {user.login}
          </span>
        </div>
      )}
      <button
        type="button"
        onClick={handleClick}
        disabled={pending}
        style={{
          width: '100%',
          padding: 'var(--space-2) var(--space-3)',
          fontSize: 'var(--font-size-sm)',
          fontWeight: 'var(--font-weight-medium)',
          color: 'var(--color-text)',
          backgroundColor: 'var(--color-surface)',
          border: '1px solid var(--color-border)',
          borderRadius: 'var(--radius-md)',
          cursor: pending ? 'wait' : 'pointer',
          opacity: pending ? 0.6 : 1,
        }}
      >
        {pending ? 'Signing out…' : 'Sign out'}
      </button>
    </div>
  );
}

export function AppShell({ children }: { children: ReactNode }) {
  return (
    <div
      style={{
        display: 'flex',
        minHeight: '100vh',
        backgroundColor: 'var(--color-bg)',
      }}
    >
      <aside
        style={{
          width: 'var(--layout-sidebar-width)',
          flexShrink: 0,
          backgroundColor: 'var(--color-surface)',
          borderRight: '1px solid var(--color-border)',
          display: 'flex',
          flexDirection: 'column',
          gap: 'var(--space-4)',
          padding: 'var(--space-4)',
        }}
      >
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 'var(--space-2)',
          }}
        >
          <img
            src="/favicon.svg"
            alt=""
            aria-hidden="true"
            width={22}
            height={22}
            style={{ display: 'block' }}
          />
          <span
            style={{
              fontSize: 'var(--font-size-lg)',
              fontWeight: 'var(--font-weight-semibold)',
              color: 'var(--color-accent)',
              letterSpacing: '-0.3px',
            }}
          >
            Trackify
          </span>
        </div>
        <WorkspaceSwitcher />
        <SignOutControl />
      </aside>
      <main
        style={{
          flex: 1,
          minWidth: 0,
          backgroundColor: 'var(--color-bg)',
        }}
      >
        {children}
      </main>
      <ToastViewport />
    </div>
  );
}
