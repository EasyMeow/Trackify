import type { ReactNode } from 'react';

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
        <p
          style={{
            fontSize: 'var(--font-size-sm)',
            color: 'var(--color-text-subtle)',
            margin: 0,
          }}
        >
          Workspace switcher — TASK-037
        </p>
        <div style={{ marginTop: 'auto' }}>
          <p
            style={{
              fontSize: 'var(--font-size-sm)',
              color: 'var(--color-text-subtle)',
              margin: 0,
            }}
          >
            Sign out — TASK-029/030
          </p>
        </div>
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
    </div>
  );
}
