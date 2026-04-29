import { format } from 'date-fns';
import type { BoardColumn, BoardTaskCard, TaskPriority } from '../types/board';

interface BoardListViewProps {
  columns: BoardColumn[];
  selectedTaskId: string | null;
  onSelectTask: (id: string) => void;
  isFiltered?: boolean;
}

const PRIORITY_COLORS: Record<TaskPriority, string> = {
  LOW: 'var(--color-text-muted)',
  MEDIUM: 'var(--color-text-subtle)',
  HIGH: 'var(--color-warning, #c97b00)',
  URGENT: 'var(--color-danger)',
};

function fmt(dateStr: string | null): string {
  if (!dateStr) return '—';
  return format(new Date(dateStr), 'MMM d, yyyy');
}

export function BoardListView({ columns, selectedTaskId, onSelectTask, isFiltered }: BoardListViewProps) {
  const tasks: Array<BoardTaskCard & { columnName: string }> = columns.flatMap((col) =>
    col.tasks.map((t) => ({ ...t, columnName: col.name }))
  );

  if (tasks.length === 0) {
    return (
      <p style={{ margin: 0, color: 'var(--color-text-muted)', fontSize: 'var(--font-size-sm)' }}>
        {isFiltered ? 'No tasks match your filters.' : 'No tasks yet.'}
      </p>
    );
  }

  const thStyle: React.CSSProperties = {
    padding: 'var(--space-2) var(--space-3)',
    textAlign: 'left',
    fontSize: 'var(--font-size-xs)',
    fontWeight: 'var(--font-weight-semibold)',
    color: 'var(--color-text-muted)',
    textTransform: 'uppercase',
    letterSpacing: '0.05em',
    borderBottom: '1px solid var(--color-border)',
    whiteSpace: 'nowrap',
  };

  return (
    <div style={{ overflowX: 'auto', flex: 1 }}>
      <table
        style={{
          width: '100%',
          borderCollapse: 'collapse',
          fontSize: 'var(--font-size-sm)',
          color: 'var(--color-text)',
        }}
      >
        <thead>
          <tr>
            <th style={thStyle}>Title</th>
            <th style={thStyle}>Status</th>
            <th style={thStyle}>Priority</th>
            <th style={thStyle}>Start date</th>
            <th style={thStyle}>Due date</th>
            <th style={thStyle}>Created</th>
          </tr>
        </thead>
        <tbody>
          {tasks.map((task) => (
            <TaskRow
              key={task.id}
              task={task}
              isSelected={task.id === selectedTaskId}
              onSelect={() => onSelectTask(task.id)}
            />
          ))}
        </tbody>
      </table>
    </div>
  );
}

function TaskRow({
  task,
  isSelected,
  onSelect,
}: {
  task: BoardTaskCard & { columnName: string };
  isSelected: boolean;
  onSelect: () => void;
}) {
  const rowStyle: React.CSSProperties = {
    backgroundColor: isSelected ? 'var(--color-accent-subtle, rgba(74,163,87,0.08))' : 'transparent',
    cursor: 'pointer',
    transition: 'background-color 0.1s',
  };

  const tdBase: React.CSSProperties = {
    padding: 'var(--space-2) var(--space-3)',
    borderBottom: '1px solid var(--color-border)',
    verticalAlign: 'middle',
  };

  const titleStyle: React.CSSProperties = {
    ...tdBase,
    fontWeight: 'var(--font-weight-medium)',
    color: 'var(--color-text)',
    maxWidth: '320px',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  };

  return (
    <tr
      style={rowStyle}
      onClick={onSelect}
      onMouseEnter={(e) => {
        if (!isSelected) (e.currentTarget as HTMLTableRowElement).style.backgroundColor = 'var(--color-bg-muted)';
      }}
      onMouseLeave={(e) => {
        if (!isSelected) (e.currentTarget as HTMLTableRowElement).style.backgroundColor = 'transparent';
      }}
    >
      <td style={titleStyle}>{task.title}</td>
      <td style={tdBase}>
        <span
          style={{
            padding: '2px var(--space-2)',
            borderRadius: 'var(--radius-sm)',
            backgroundColor: 'var(--color-bg-muted)',
            fontSize: 'var(--font-size-xs)',
            fontWeight: 'var(--font-weight-medium)',
            color: 'var(--color-text-subtle)',
            whiteSpace: 'nowrap',
          }}
        >
          {task.columnName}
        </span>
      </td>
      <td style={tdBase}>
        <span
          style={{
            fontSize: 'var(--font-size-xs)',
            fontWeight: 'var(--font-weight-semibold)',
            color: PRIORITY_COLORS[task.priority],
          }}
        >
          {task.priority}
        </span>
      </td>
      <td style={{ ...tdBase, color: 'var(--color-text-subtle)', whiteSpace: 'nowrap' }}>
        {fmt(task.startDate)}
      </td>
      <td style={{ ...tdBase, color: 'var(--color-text-subtle)', whiteSpace: 'nowrap' }}>
        {fmt(task.dueDate)}
      </td>
      <td style={{ ...tdBase, color: 'var(--color-text-muted)', whiteSpace: 'nowrap' }}>
        {fmt(task.createdAt)}
      </td>
    </tr>
  );
}
