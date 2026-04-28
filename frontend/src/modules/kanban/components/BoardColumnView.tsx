import { useState, useRef, useEffect } from 'react';
import { useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import type { BoardColumn } from '../types/board';
import { DraggableTaskCard } from './DraggableTaskCard';

const columnNameStyle: React.CSSProperties = {
  margin: 0,
  fontSize: 'var(--font-size-sm)',
  fontWeight: 'var(--font-weight-semibold)',
  color: 'var(--color-text)',
  textTransform: 'uppercase',
  letterSpacing: '0.04em',
  cursor: 'text',
  flex: 1,
  minWidth: 0,
  overflow: 'hidden',
  textOverflow: 'ellipsis',
  whiteSpace: 'nowrap',
};

const countBadgeStyle: React.CSSProperties = {
  fontSize: 'var(--font-size-xs)',
  color: 'var(--color-text-subtle)',
  backgroundColor: 'var(--color-surface-alt)',
  border: '1px solid var(--color-border)',
  borderRadius: 'var(--radius-pill)',
  padding: '1px 7px',
  fontWeight: 'var(--font-weight-medium)',
  flexShrink: 0,
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

const gripStyle: React.CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  cursor: 'grab',
  color: 'var(--color-text-subtle)',
  flexShrink: 0,
  width: '16px',
  fontSize: '14px',
  userSelect: 'none',
  touchAction: 'none',
};

const deleteBtnStyle: React.CSSProperties = {
  background: 'none',
  border: 'none',
  cursor: 'pointer',
  color: 'var(--color-text-subtle)',
  fontSize: '16px',
  lineHeight: 1,
  padding: '0 2px',
  flexShrink: 0,
  borderRadius: 'var(--radius-sm)',
};

const renameInputStyle: React.CSSProperties = {
  flex: 1,
  minWidth: 0,
  fontSize: 'var(--font-size-sm)',
  fontWeight: 'var(--font-weight-semibold)',
  textTransform: 'uppercase',
  letterSpacing: '0.04em',
  color: 'var(--color-text)',
  backgroundColor: 'var(--color-bg)',
  border: '1px solid var(--color-accent)',
  borderRadius: 'var(--radius-sm)',
  padding: '1px 4px',
  outline: 'none',
};

interface BoardColumnViewProps {
  column: BoardColumn;
  selectedTaskId?: string | null;
  onSelectTask?: (id: string) => void;
  onRename?: (columnId: string, name: string) => void;
  onDelete?: (columnId: string) => void;
}

export function BoardColumnView({
  column,
  selectedTaskId,
  onSelectTask,
  onRename,
  onDelete,
}: BoardColumnViewProps) {
  const {
    setNodeRef,
    attributes,
    listeners,
    transform,
    transition,
    isDragging,
    isOver,
  } = useSortable({ id: column.id, data: { type: 'column' } });

  const [editing, setEditing] = useState(false);
  const [editValue, setEditValue] = useState(column.name);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (editing && inputRef.current) {
      inputRef.current.focus();
      inputRef.current.select();
    }
  }, [editing]);

  function startEdit() {
    setEditValue(column.name);
    setEditing(true);
  }

  function commitEdit() {
    const trimmed = editValue.trim();
    if (trimmed && trimmed !== column.name && onRename) {
      onRename(column.id, trimmed);
    }
    setEditing(false);
  }

  function handleKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'Enter') {
      commitEdit();
    } else if (e.key === 'Escape') {
      setEditing(false);
    }
  }

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
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.4 : 1,
  };

  const headerStyle: React.CSSProperties = {
    display: 'flex',
    alignItems: 'center',
    gap: 'var(--space-2)',
    marginBottom: 'var(--space-1)',
  };

  // isOver is true for any draggable hovering over this column — show highlight only when not dragging the column itself
  const droppableStyle: React.CSSProperties = {
    ...tasksListStyle,
    backgroundColor: isOver && !isDragging ? 'var(--color-accent-soft, rgba(0,0,0,0.04))' : undefined,
    borderRadius: 'var(--radius-md)',
    transition: 'background-color 150ms ease',
  };

  return (
    <div ref={setNodeRef} style={columnStyle}>
      <div style={headerStyle}>
        {/* Drag handle — only this element gets the DnD listeners */}
        <span style={gripStyle} {...attributes} {...listeners} title="Drag to reorder">
          ⠿
        </span>

        {editing ? (
          <input
            ref={inputRef}
            style={renameInputStyle}
            value={editValue}
            onChange={(e) => setEditValue(e.target.value)}
            onBlur={commitEdit}
            onKeyDown={handleKeyDown}
            maxLength={100}
          />
        ) : (
          <h2 style={columnNameStyle} onClick={startEdit} title="Click to rename">
            {column.name}
          </h2>
        )}

        <span style={countBadgeStyle}>{column.tasks.length}</span>

        <button
          style={deleteBtnStyle}
          onClick={() => onDelete?.(column.id)}
          title="Delete column"
          aria-label="Delete column"
        >
          ×
        </button>
      </div>

      <div style={droppableStyle}>
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
