import { useParams } from 'react-router-dom';

export default function ProjectBoardPage() {
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
      <h1>Project board</h1>
      <p style={{ color: 'var(--color-text-muted)' }}>
        Project: <code>{projectId}</code>. Kanban view lands in TASK-049.
      </p>
    </section>
  );
}
