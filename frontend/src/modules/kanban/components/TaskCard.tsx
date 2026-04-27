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

const baseCardStyle: React.CSSProperties = {
  padding: 'var(--space-3)',
  backgroundColor: 'var(--color-surface)',
  border: '1px solid var(--color-border)',
  borderRadius: 'var(--radius-md)',
  boxShadow: 'var(--shadow-xs)',
  display: 'flex',
  flexDirection: 'column',
  gap: 'var(--space-2)',
  cursor: 'pointer',
  textAlign: 'left',
  width: '100%',
  fontFamily: 'inherit',
  transition: 'box-shadow var(--transition-fast), outline-color var(--transition-fast)',
  outline: '2px solid transparent',
  outlineOffset: '2px',
};

const activeCardStyle: React.CSSProperties = {
  ...baseCardStyle,
  outline: '2px solid var(--color-accent)',
  boxShadow: 'var(--shadow-md)',
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

interface TaskCardProps {
  task: BoardTaskCard;
  isActive?: boolean;
  onSelect?: (id: string) => void;
}

export function TaskCard({ task, isActive = false, onSelect }: TaskCardProps) {
  function handleClick() {
    onSelect?.(task.id);
  }

  function handleKeyDown(e: React.KeyboardEvent) {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      onSelect?.(task.id);
    }
  }

  return (
    <button
      type="button"
      style={isActive ? activeCardStyle : baseCardStyle}
      onClick={handleClick}
      onKeyDown={handleKeyDown}
      aria-pressed={isActive}
      aria-label={`Open task: ${task.title}`}
    >
      <p style={titleStyle}>{task.title}</p>
      <span style={{ ...badgeStyle, ...PRIORITY_STYLE[task.priority] }}>
        {PRIORITY_LABEL[task.priority]}
      </span>
    </button>
  );
}
