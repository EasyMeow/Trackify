export default function DashboardPage() {
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
    </section>
  );
}
