import { useState, useMemo } from 'react';
import { useParams } from 'react-router-dom';
import {
  DndContext,
  DragOverlay,
  PointerSensor,
  useSensor,
  useSensors,
  type DragStartEvent,
  type DragEndEvent,
} from '@dnd-kit/core';
import { useBoardQuery } from '../modules/kanban/hooks/useBoardQuery';
import { useCreateTask } from '../modules/kanban/hooks/useCreateTask';
import { useMoveTask } from '../modules/kanban/hooks/useMoveTask';
import { BoardColumnView } from '../modules/kanban/components/BoardColumnView';
import { TaskCard } from '../modules/kanban/components/TaskCard';
import { useSelectedTaskId } from '../modules/task/hooks/useSelectedTaskId';
import { TaskDrawer } from '../modules/task/components/TaskDrawer';
import { ApiError } from '../shared/api/httpClient';
import type { BoardColumn, BoardTaskCard } from '../modules/kanban/types/board';

const pageStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  gap: 'var(--space-4)',
  padding: 'var(--space-5)',
  height: '100%',
  boxSizing: 'border-box',
};

const boardStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'row',
  gap: 'var(--space-4)',
  alignItems: 'flex-start',
  overflowX: 'auto',
  paddingBottom: 'var(--space-4)',
  flex: 1,
};

const mutedStyle: React.CSSProperties = {
  margin: 0,
  color: 'var(--color-text-muted)',
  fontSize: 'var(--font-size-sm)',
};

const headingStyle: React.CSSProperties = {
  margin: 0,
  fontSize: 'var(--font-size-xl)',
  fontWeight: 'var(--font-weight-semibold)',
  color: 'var(--color-text)',
};

const createRowStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'row',
  alignItems: 'center',
  gap: 'var(--space-2)',
  flexWrap: 'wrap',
};

const inputStyle: React.CSSProperties = {
  padding: 'var(--space-2) var(--space-3)',
  fontSize: 'var(--font-size-sm)',
  color: 'var(--color-text)',
  backgroundColor: 'var(--color-bg)',
  border: '1px solid var(--color-border)',
  borderRadius: 'var(--radius-md)',
  outline: 'none',
  minWidth: '220px',
  flex: '1 1 220px',
  maxWidth: '400px',
};

const submitButtonStyle: React.CSSProperties = {
  padding: 'var(--space-2) var(--space-4)',
  fontSize: 'var(--font-size-sm)',
  fontWeight: 'var(--font-weight-semibold)',
  color: 'var(--color-text-on-accent)',
  backgroundColor: 'var(--color-accent)',
  border: 'none',
  borderRadius: 'var(--radius-md)',
  cursor: 'pointer',
  whiteSpace: 'nowrap',
};

const submitButtonDisabledStyle: React.CSSProperties = {
  ...submitButtonStyle,
  opacity: 0.5,
  cursor: 'not-allowed',
};

const errorTextStyle: React.CSSProperties = {
  fontSize: 'var(--font-size-sm)',
  color: 'var(--color-danger)',
  margin: 0,
};

/**
 * Local-only DnD overrides: maps taskId → columnId for tasks that have been
 * dragged to a different column since the last server refetch.
 * Cleared automatically when TanStack Query refetches (data reference changes).
 */
type DragOverrides = Map<string, string>;

/** Derive display columns by applying local drag overrides to server data. */
function applyOverrides(serverColumns: BoardColumn[], overrides: DragOverrides): BoardColumn[] {
  if (overrides.size === 0) return serverColumns;

  // Determine which taskIds actually moved to a different column
  const moved = new Map<string, { task: BoardTaskCard; toColId: string }>();
  for (const col of serverColumns) {
    for (const task of col.tasks) {
      const toColId = overrides.get(task.id);
      if (toColId && toColId !== col.id) {
        moved.set(task.id, { task, toColId });
      }
    }
  }

  return serverColumns.map((col) => {
    // Keep tasks not moved away from this column
    const remaining = col.tasks.filter((t) => !moved.has(t.id));
    // Append tasks moved INTO this column
    const arrivals = [...moved.values()]
      .filter(({ toColId }) => toColId === col.id)
      .map(({ task }) => task);
    return { ...col, tasks: [...remaining, ...arrivals] };
  });
}

export default function ProjectBoardPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const { data, isLoading, isError } = useBoardQuery(projectId);
  const [selectedTaskId, setSelectedTaskId] = useSelectedTaskId();
  const moveTask = useMoveTask(projectId);

  // Local DnD overrides — taskId → targetColumnId
  // NOT stored in TanStack cache; cleared on next server refetch
  const [dragOverrides, setDragOverrides] = useState<DragOverrides>(new Map());
  const [activeTask, setActiveTask] = useState<BoardTaskCard | null>(null);

  // Derive display columns from server data + local overrides
  const displayColumns = useMemo<BoardColumn[]>(
    () => (data ? applyOverrides(data.columns, dragOverrides) : []),
    [data, dragOverrides]
  );

  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: {
        // 8px movement threshold so clicks still fire the drawer
        distance: 8,
      },
    })
  );

  function handleDragStart(event: DragStartEvent) {
    const taskId = event.active.id as string;
    for (const col of displayColumns) {
      const found = col.tasks.find((t) => t.id === taskId);
      if (found) {
        setActiveTask(found);
        break;
      }
    }
  }

  function handleDragEnd(event: DragEndEvent) {
    setActiveTask(null);

    const { active, over } = event;
    if (!over) return;

    const draggedTaskId = active.id as string;
    const targetColumnId = over.id as string;

    // Find the current column of the dragged task
    const currentColumn = displayColumns.find((col) =>
      col.tasks.some((t) => t.id === draggedTaskId)
    );
    if (!currentColumn || currentColumn.id === targetColumnId) return;

    // Optimistic local move so the card stays put while the request is in flight.
    setDragOverrides((prev) => {
      const next = new Map(prev);
      next.set(draggedTaskId, targetColumnId);
      return next;
    });

    // Append-at-end sort: max existing sortOrder in target column + 1, or 0 if empty.
    const targetColumn = displayColumns.find((col) => col.id === targetColumnId);
    const lastSortOrder = targetColumn?.tasks.reduce(
      (max, task) => (task.sortOrder > max ? task.sortOrder : max),
      Number.NEGATIVE_INFINITY
    );
    const sortOrder =
      lastSortOrder === undefined || lastSortOrder === Number.NEGATIVE_INFINITY
        ? 0
        : lastSortOrder + 1;

    moveTask.mutate(
      { taskId: draggedTaskId, columnId: targetColumnId, sortOrder },
      {
        onSettled: () => {
          // Drop the local override either way: on success the refetched board
          // already shows the card in the new column, on error TASK-061 will
          // add proper rollback — for now snapping back to server truth is fine.
          setDragOverrides((prev) => {
            if (!prev.has(draggedTaskId)) return prev;
            const next = new Map(prev);
            next.delete(draggedTaskId);
            return next;
          });
        },
      }
    );
  }

  return (
    <section style={pageStyle}>
      <h1 style={headingStyle}>Board</h1>
      <CreateTaskRow projectId={projectId} />

      {isLoading && <p style={mutedStyle}>Loading board…</p>}
      {isError && <p style={{ ...mutedStyle, color: 'var(--color-danger)' }}>Couldn't load board.</p>}
      {displayColumns.length > 0 && (
        <DndContext sensors={sensors} onDragStart={handleDragStart} onDragEnd={handleDragEnd}>
          <div style={boardStyle}>
            {displayColumns.map((column) => (
              <BoardColumnView
                key={column.id}
                column={column}
                selectedTaskId={selectedTaskId}
                onSelectTask={setSelectedTaskId}
              />
            ))}
          </div>
          <DragOverlay>
            {activeTask ? (
              <div style={{ opacity: 0.9, pointerEvents: 'none', width: '260px' }}>
                <TaskCard task={activeTask} />
              </div>
            ) : null}
          </DragOverlay>
        </DndContext>
      )}
      <TaskDrawer taskId={selectedTaskId} projectId={projectId ?? ''} onClose={() => setSelectedTaskId(null)} />
    </section>
  );
}

function CreateTaskRow({ projectId }: { projectId: string | undefined }) {
  const [title, setTitle] = useState('');
  const { mutate, isPending, error, reset } = useCreateTask(projectId);

  const errorMessage =
    error instanceof ApiError ? error.message : error ? String(error) : null;

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!title.trim()) return;
    mutate(
      { title: title.trim() },
      {
        onSuccess: () => {
          setTitle('');
          reset();
        },
      }
    );
  }

  const isDisabled = isPending || !title.trim();

  return (
    <form style={createRowStyle} onSubmit={handleSubmit}>
      <input
        style={inputStyle}
        type="text"
        value={title}
        onChange={(e) => setTitle(e.target.value)}
        placeholder="New task title…"
        maxLength={255}
        disabled={isPending}
      />
      <button
        type="submit"
        style={isDisabled ? submitButtonDisabledStyle : submitButtonStyle}
        disabled={isDisabled}
      >
        {isPending ? 'Creating…' : 'Create task'}
      </button>
      {errorMessage && <p style={errorTextStyle}>{errorMessage}</p>}
    </form>
  );
}
