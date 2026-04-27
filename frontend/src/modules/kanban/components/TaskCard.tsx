import type { BoardTaskCard, TaskPriority } from '../types/board';

const PRIORITY_LABEL: Record<TaskPriority, string> = {
  LOW: 'Low',
  MEDIUM: 'Medium',
  HIGH: 'High',
  URGENT: 'Urgent',
};

const PRIORITY_STYLE: Record<TaskPriority, React.CSSProperties> = {
  LOW: {
    backgroundColor: 'var(--color-surface-alt)',
    color: 'var(--color-text-muted)',
    border: '1px solid var(--color-border)',
  },
  MEDIUM: {
    backgroundColor: 'var(--color-info-soft)',
    color: 'var(--color-info)',
    border: '1px solid var(--color-border)',
  },
  HIGH: {
    backgroundColor: 'var(--color-warning-soft)',
    color: 'var(--color-warning)',
    border: '1px solid var(--color-border)',
  },
  URGENT: {
    backgroundColor: 'var(--color-danger-soft)',
    color: 'var(--color-danger)',
    border: '1px solid var(--color-border)',
  },
};

const cardStyle: React.CSSProperties = {
  padding: 'var(--space-3)',
  backgroundColor: 'var(--color-surface)',
  border: '1px solid var(--color-border)',
  borderRadius: 'var(--radius-md)',
  boxShadow: 'var(--shadow-xs)',
  display: 'flex',
  flexDirection: 'column',
  gap: 'var(--space-2)',
};

const titleStyle: React.CSSProperties = {
  margin: 0,
  fontSize: 'var(--font-size-sm)',
  fontWeight: 'var(--font-weight-medium)',
  color: 'var(--color-text)',
  lineHeight: 'var(--line-height-snug)',
};

const badgeStyle: React.CSSProperties = {
  display: 'inline-block',
  padding: '2px var(--space-2)',
  borderRadius: 'var(--radius-pill)',
  fontSize: 'var(--font-size-xs)',
  fontWeight: 'var(--font-weight-medium)',
  lineHeight: 1.4,
  alignSelf: 'flex-start',
};

export function TaskCard({ task }: { task: BoardTaskCard }) {
  return (
    <div style={cardStyle}>
      <p style={titleStyle}>{task.title}</p>
      <span style={{ ...badgeStyle, ...PRIORITY_STYLE[task.priority] }}>
        {PRIORITY_LABEL[task.priority]}
      </span>
    </div>
  );
}
