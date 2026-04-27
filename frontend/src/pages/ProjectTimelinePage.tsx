import { useParams } from 'react-router-dom';
import { useProjectTimeline } from '../modules/gantt/hooks/useProjectTimeline';

export default function ProjectTimelinePage() {
  const { projectId } = useParams<{ projectId: string }>();
  const { data, isLoading } = useProjectTimeline(projectId);

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
      {isLoading ? (
        <p style={{ color: 'var(--color-text-muted)' }}>Loading…</p>
      ) : (
        <p style={{ color: 'var(--color-text-muted)' }}>
          {data ? `${data.tasks.length} task(s)` : 'No data'}. Gantt view lands in TASK-069.
        </p>
      )}
    </section>
  );
}
