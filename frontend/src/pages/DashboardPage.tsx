import { useWorkspaces } from '../modules/workspace/hooks/useWorkspaces';

export default function DashboardPage() {
  const { data: workspaces, isLoading } = useWorkspaces();

  return (
    <section
      style={{
        display: 'flex',
        flexDirection: 'column',
        gap: 'var(--space-2)',
        padding: 'var(--space-6)',
      }}
    >
      <h1>Dashboard</h1>
      <p style={{ color: 'var(--color-text-muted)' }}>
        Project list and create-project flow land in TASK-042 and TASK-043.
      </p>
      {isLoading && <p style={{ color: 'var(--color-text-muted)' }}>Loading workspaces…</p>}
      {workspaces && (
        <p style={{ color: 'var(--color-text-muted)' }}>
          {workspaces.length} workspace{workspaces.length !== 1 ? 's' : ''}
        </p>
      )}
    </section>
  );
}
