import { useParams } from 'react-router-dom';

export default function ProjectSettingsPage() {
  const { projectId } = useParams<{ projectId: string }>();
  return (
    <section
      style={{
        display: 'flex',
        flexDirection: 'column',
        gap: 'var(--space-2)',
        padding: 'var(--space-6)',
      }}
    >
      <h1>Project settings</h1>
      <p style={{ color: 'var(--color-text-muted)' }}>
        Project: <code>{projectId}</code>. Settings view lands in TASK-080.
      </p>
    </section>
  );
}
