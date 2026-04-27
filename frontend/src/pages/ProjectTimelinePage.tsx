import 'gantt-task-react/dist/index.css';
import { useParams } from 'react-router-dom';
import { Gantt, ViewMode } from 'gantt-task-react';
import { useProjectTimeline } from '../modules/gantt/hooks/useProjectTimeline';
import { mapTimelineToGantt } from '../modules/gantt/utils/mapTimelineToGantt';

export default function ProjectTimelinePage() {
  const { projectId } = useParams<{ projectId: string }>();
  const { data, isLoading, error } = useProjectTimeline(projectId);

  if (isLoading) {
    return (
      <section style={sectionStyle}>
        <h1>Project timeline</h1>
        <p style={mutedStyle}>Loading…</p>
      </section>
    );
  }

  if (error) {
    return (
      <section style={sectionStyle}>
        <h1>Project timeline</h1>
        <p style={{ color: 'var(--color-danger)' }}>Failed to load timeline.</p>
      </section>
    );
  }

  if (!data || data.tasks.length === 0) {
    return (
      <section style={sectionStyle}>
        <h1>Project timeline</h1>
        <p style={mutedStyle}>No tasks yet.</p>
      </section>
    );
  }

  const { ganttTasks, datelessCount } = mapTimelineToGantt(data.tasks);

  if (ganttTasks.length === 0) {
    return (
      <section style={sectionStyle}>
        <h1>Project timeline</h1>
        <p style={mutedStyle}>
          No tasks have start and due dates set ({datelessCount} task
          {datelessCount !== 1 ? 's' : ''} without dates).
        </p>
      </section>
    );
  }

  return (
    <section style={sectionStyle}>
      <h1>Project timeline</h1>
      <Gantt
        tasks={ganttTasks}
        viewMode={ViewMode.Week}
        listCellWidth="180px"
        ganttHeight={400}
        barBackgroundColor="var(--color-accent-soft)"
        barBackgroundSelectedColor="var(--color-accent)"
        todayColor="rgba(74, 139, 111, 0.12)"
      />
      {datelessCount > 0 && (
        <p style={mutedStyle}>
          {datelessCount} task{datelessCount !== 1 ? 's' : ''} not shown — no
          start or due date set.
        </p>
      )}
    </section>
  );
}

const sectionStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  gap: 'var(--space-4)',
  padding: 'var(--space-6)',
};

const mutedStyle: React.CSSProperties = {
  color: 'var(--color-text-muted)',
  fontSize: 'var(--font-size-sm)',
};
