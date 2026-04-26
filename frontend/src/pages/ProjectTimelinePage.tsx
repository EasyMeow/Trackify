import { useParams } from 'react-router-dom';

export default function ProjectTimelinePage() {
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
      <h1>Project timeline</h1>
      <p style={{ color: 'var(--color-text-muted)' }}>
        Project: <code>{projectId}</code>. Gantt view lands in TASK-069.
      </p>
    </section>
  );
}
