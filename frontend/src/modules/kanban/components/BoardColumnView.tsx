import { useDroppable } from '@dnd-kit/core';
import type { BoardColumn } from '../types/board';
import { DraggableTaskCard } from './DraggableTaskCard';

const columnStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  gap: 'var(--space-2)',
  minWidth: '260px',
  maxWidth: '300px',
  flex: '0 0 auto',
  backgroundColor: 'var(--color-bg-muted)',
  border: '1px solid var(--color-border)',
  borderRadius: 'var(--radius-lg)',
  padding: 'var(--space-3)',
};

const headerStyle: React.CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'space-between',
  gap: 'var(--space-2)',
  marginBottom: 'var(--space-1)',
};

const columnNameStyle: React.CSSProperties = {
  margin: 0,
  fontSize: 'var(--font-size-sm)',
  fontWeight: 'var(--font-weight-semibold)',
  color: 'var(--color-text)',
  textTransform: 'uppercase',
  letterSpacing: '0.04em',
};

const countBadgeStyle: React.CSSProperties = {
  fontSize: 'var(--font-size-xs)',
  color: 'var(--color-text-subtle)',
  backgroundColor: 'var(--color-surface-alt)',
  border: '1px solid var(--color-border)',
  borderRadius: 'var(--radius-pill)',
  padding: '1px 7px',
  fontWeight: 'var(--font-weight-medium)',
};

const emptyHintStyle: React.CSSProperties = {
  margin: 0,
  fontSize: 'var(--font-size-xs)',
  color: 'var(--color-text-subtle)',
  textAlign: 'center',
  padding: 'var(--space-4) 0',
};

const tasksListStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  gap: 'var(--space-2)',
  minHeight: '40px',
};

interface BoardColumnViewProps {
  column: BoardColumn;
  selectedTaskId?: string | null;
  onSelectTask?: (id: string) => void;
}

export function BoardColumnView({ column, selectedTaskId, onSelectTask }: BoardColumnViewProps) {
  const { setNodeRef, isOver } = useDroppable({ id: column.id });

  const droppableStyle: React.CSSProperties = {
    ...tasksListStyle,
    backgroundColor: isOver ? 'var(--color-accent-soft, rgba(0,0,0,0.04))' : undefined,
    borderRadius: 'var(--radius-md)',
    transition: 'background-color 150ms ease',
  };

  return (
    <div style={columnStyle}>
      <div style={headerStyle}>
        <h2 style={columnNameStyle}>{column.name}</h2>
        <span style={countBadgeStyle}>{column.tasks.length}</span>
      </div>
      <div ref={setNodeRef} style={droppableStyle}>
        {column.tasks.length === 0 ? (
          <p style={emptyHintStyle}>No tasks</p>
        ) : (
          column.tasks.map((task) => (
            <DraggableTaskCard
              key={task.id}
              task={task}
              columnId={column.id}
              isActive={task.id === selectedTaskId}
              onSelect={onSelectTask}
            />
          ))
        )}
      </div>
    </div>
  );
}
