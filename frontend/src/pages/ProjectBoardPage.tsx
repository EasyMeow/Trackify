import { useState, useMemo, useRef } from 'react';
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
import { SortableContext, arrayMove, horizontalListSortingStrategy } from '@dnd-kit/sortable';
import { useBoardQuery } from '../modules/kanban/hooks/useBoardQuery';
import { useMoveTask } from '../modules/kanban/hooks/useMoveTask';
import { useCreateColumn } from '../modules/kanban/hooks/useCreateColumn';
import { useRenameColumn } from '../modules/kanban/hooks/useRenameColumn';
import { useDeleteColumn } from '../modules/kanban/hooks/useDeleteColumn';
import { useReorderColumns } from '../modules/kanban/hooks/useReorderColumns';
import { ProjectNav } from '../modules/project/components/ProjectNav';
import { BoardColumnView } from '../modules/kanban/components/BoardColumnView';
import { TaskCard } from '../modules/kanban/components/TaskCard';
import { useSelectedTaskId } from '../modules/task/hooks/useSelectedTaskId';
import { TaskDrawer } from '../modules/task/components/TaskDrawer';
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

/**
 * Local-only DnD overrides: maps taskId → columnId for tasks that have been
 * dragged to a different column since the last server refetch.
 * Cleared automatically when TanStack Query refetches (data reference changes).
 */
type DragOverrides = Map<string, string>;

/** Derive display columns by applying local drag overrides to server data. */
function applyOverrides(serverColumns: BoardColumn[], overrides: DragOverrides): BoardColumn[] {
  if (overrides.size === 0) return serverColumns;

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
    const remaining = col.tasks.filter((t) => !moved.has(t.id));
    const arrivals = [...moved.values()]
      .filter(({ toColId }) => toColId === col.id)
      .map(({ task }) => task);
    return { ...col, tasks: [...remaining, ...arrivals] };
  });
}

/** Apply column order override: reorder columns by the given id array. */
function applyColumnOrder(columns: BoardColumn[], order: string[]): BoardColumn[] {
  const map = new Map(columns.map((c) => [c.id, c]));
  const ordered: BoardColumn[] = [];
  for (const id of order) {
    const col = map.get(id);
    if (col) ordered.push(col);
  }
  // Append any columns not in the order array
  for (const col of columns) {
    if (!order.includes(col.id)) ordered.push(col);
  }
  return ordered;
}

export default function ProjectBoardPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const { data, isLoading, isError } = useBoardQuery(projectId);
  const [selectedTaskId, setSelectedTaskId] = useSelectedTaskId();
  const moveTask = useMoveTask(projectId);
  const renameColumn = useRenameColumn(projectId);
  const deleteColumn = useDeleteColumn(projectId);
  const reorderColumns = useReorderColumns(projectId);

  const [isCreating, setIsCreating] = useState(false);
  const [dragOverrides, setDragOverrides] = useState<DragOverrides>(new Map());
  const [activeTask, setActiveTask] = useState<BoardTaskCard | null>(null);
  const [activeColumnId, setActiveColumnId] = useState<string | null>(null);
  const [columnOrderOverride, setColumnOrderOverride] = useState<string[] | null>(null);

  const displayColumns = useMemo<BoardColumn[]>(() => {
    if (!data) return [];
    const withTaskOverrides = applyOverrides(data.columns, dragOverrides);
    if (columnOrderOverride) {
      return applyColumnOrder(withTaskOverrides, columnOrderOverride);
    }
    return withTaskOverrides;
  }, [data, dragOverrides, columnOrderOverride]);

  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: { distance: 8 },
    })
  );

  function handleDragStart(event: DragStartEvent) {
    const type = event.active.data.current?.type;
    if (type === 'column') {
      setActiveColumnId(event.active.id as string);
      return;
    }
    // Task drag
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
    const { active, over } = event;

    if (active.data.current?.type === 'column') {
      setActiveColumnId(null);
      if (!over || active.id === over.id) return;

      const oldIds = displayColumns.map((c) => c.id);
      const oldIndex = oldIds.indexOf(active.id as string);
      const newIndex = oldIds.indexOf(over.id as string);
      if (oldIndex === -1 || newIndex === -1) return;

      const newOrder = arrayMove(oldIds, oldIndex, newIndex);
      setColumnOrderOverride(newOrder);
      reorderColumns.mutate(newOrder, {
        onError: () => setColumnOrderOverride(null),
      });
      return;
    }

    // Task drag
    setActiveTask(null);
    if (!over) return;

    const draggedTaskId = active.id as string;
    const targetColumnId = over.id as string;

    const currentColumn = displayColumns.find((col) =>
      col.tasks.some((t) => t.id === draggedTaskId)
    );
    if (!currentColumn || currentColumn.id === targetColumnId) return;

    setDragOverrides((prev) => {
      const next = new Map(prev);
      next.set(draggedTaskId, targetColumnId);
      return next;
    });

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
        onError: () => {
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

  const activeColumn = activeColumnId
    ? displayColumns.find((c) => c.id === activeColumnId) ?? null
    : null;

  return (
    <section style={pageStyle}>
      <ProjectNav projectId={projectId} />
      <h1 style={headingStyle}>Board</h1>
      <button
        type="button"
        style={{
          alignSelf: 'flex-start',
          padding: 'var(--space-2) var(--space-4)',
          fontSize: 'var(--font-size-sm)',
          fontWeight: 'var(--font-weight-semibold)',
          color: 'var(--color-text-on-accent)',
          backgroundColor: 'var(--color-accent)',
          border: 'none',
          borderRadius: 'var(--radius-md)',
          cursor: 'pointer',
        }}
        onClick={() => setIsCreating(true)}
      >
        + Create task
      </button>

      {isLoading && <p style={mutedStyle}>Loading board…</p>}
      {isError && <p style={{ ...mutedStyle, color: 'var(--color-danger)' }}>Couldn't load board.</p>}
      {!isLoading && !isError && data && displayColumns.length === 0 && (
        <p style={mutedStyle}>This board has no columns yet.</p>
      )}

      <DndContext sensors={sensors} onDragStart={handleDragStart} onDragEnd={handleDragEnd}>
        <div style={boardStyle}>
          <SortableContext
            items={displayColumns.map((c) => c.id)}
            strategy={horizontalListSortingStrategy}
          >
            {displayColumns.map((column) => (
              <BoardColumnView
                key={column.id}
                column={column}
                selectedTaskId={selectedTaskId}
                onSelectTask={setSelectedTaskId}
                onRename={(colId, name) => renameColumn.mutate({ columnId: colId, name })}
                onDelete={(colId) => deleteColumn.mutate(colId)}
              />
            ))}
          </SortableContext>
          <AddColumnForm projectId={projectId} />
        </div>
        <DragOverlay>
          {activeTask ? (
            <div style={{ opacity: 0.9, pointerEvents: 'none', width: '260px' }}>
              <TaskCard task={activeTask} />
            </div>
          ) : activeColumn ? (
            <div
              style={{
                opacity: 0.9,
                pointerEvents: 'none',
                minWidth: '260px',
                maxWidth: '300px',
                backgroundColor: 'var(--color-bg-muted)',
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-lg)',
                padding: 'var(--space-3)',
              }}
            >
              <span
                style={{
                  fontSize: 'var(--font-size-sm)',
                  fontWeight: 'var(--font-weight-semibold)',
                  textTransform: 'uppercase',
                  letterSpacing: '0.04em',
                  color: 'var(--color-text)',
                }}
              >
                {activeColumn.name}
              </span>
            </div>
          ) : null}
        </DragOverlay>
      </DndContext>

      <TaskDrawer
        taskId={selectedTaskId}
        projectId={projectId ?? ''}
        createMode={isCreating && !selectedTaskId}
        onClose={() => {
          setSelectedTaskId(null);
          setIsCreating(false);
        }}
      />
    </section>
  );
}

function AddColumnForm({ projectId }: { projectId: string | undefined }) {
  const [open, setOpen] = useState(false);
  const [name, setName] = useState('');
  const inputRef = useRef<HTMLInputElement>(null);
  const createColumn = useCreateColumn(projectId);

  function openForm() {
    setOpen(true);
    setName('');
    // focus next tick after render
    setTimeout(() => inputRef.current?.focus(), 0);
  }

  function closeForm() {
    setOpen(false);
    setName('');
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const trimmed = name.trim();
    if (!trimmed) return;
    createColumn.mutate(trimmed, { onSuccess: () => closeForm() });
  }

  const addBtnStyle: React.CSSProperties = {
    display: 'flex',
    alignItems: 'center',
    gap: 'var(--space-1)',
    padding: 'var(--space-2) var(--space-3)',
    fontSize: 'var(--font-size-sm)',
    fontWeight: 'var(--font-weight-medium)',
    color: 'var(--color-text-subtle)',
    backgroundColor: 'var(--color-bg-muted)',
    border: '1px dashed var(--color-border)',
    borderRadius: 'var(--radius-lg)',
    cursor: 'pointer',
    whiteSpace: 'nowrap',
    minWidth: '140px',
    alignSelf: 'flex-start',
  };

  const formContainerStyle: React.CSSProperties = {
    display: 'flex',
    flexDirection: 'column',
    gap: 'var(--space-2)',
    minWidth: '200px',
    maxWidth: '260px',
    flex: '0 0 auto',
    backgroundColor: 'var(--color-bg-muted)',
    border: '1px solid var(--color-border)',
    borderRadius: 'var(--radius-lg)',
    padding: 'var(--space-3)',
    alignSelf: 'flex-start',
  };

  const colInputStyle: React.CSSProperties = {
    padding: 'var(--space-2) var(--space-3)',
    fontSize: 'var(--font-size-sm)',
    color: 'var(--color-text)',
    backgroundColor: 'var(--color-bg)',
    border: '1px solid var(--color-border)',
    borderRadius: 'var(--radius-md)',
    outline: 'none',
    width: '100%',
    boxSizing: 'border-box',
  };

  const formActionsStyle: React.CSSProperties = {
    display: 'flex',
    gap: 'var(--space-2)',
  };

  const cancelBtnStyle: React.CSSProperties = {
    flex: 1,
    padding: 'var(--space-1) var(--space-2)',
    fontSize: 'var(--font-size-sm)',
    color: 'var(--color-text-subtle)',
    backgroundColor: 'transparent',
    border: '1px solid var(--color-border)',
    borderRadius: 'var(--radius-md)',
    cursor: 'pointer',
  };

  const saveBtnStyle: React.CSSProperties = {
    flex: 1,
    padding: 'var(--space-1) var(--space-2)',
    fontSize: 'var(--font-size-sm)',
    fontWeight: 'var(--font-weight-semibold)',
    color: 'var(--color-text-on-accent)',
    backgroundColor: 'var(--color-accent)',
    border: 'none',
    borderRadius: 'var(--radius-md)',
    cursor: 'pointer',
  };

  const saveBtnDisabledStyle: React.CSSProperties = {
    ...saveBtnStyle,
    opacity: 0.5,
    cursor: 'not-allowed',
  };

  if (!open) {
    return (
      <button style={addBtnStyle} onClick={openForm}>
        + Add column
      </button>
    );
  }

  const isDisabled = createColumn.isPending || !name.trim();

  return (
    <form style={formContainerStyle} onSubmit={handleSubmit}>
      <input
        ref={inputRef}
        style={colInputStyle}
        type="text"
        value={name}
        onChange={(e) => setName(e.target.value)}
        placeholder="Column name…"
        maxLength={100}
        disabled={createColumn.isPending}
        onKeyDown={(e) => e.key === 'Escape' && closeForm()}
      />
      <div style={formActionsStyle}>
        <button type="submit" style={isDisabled ? saveBtnDisabledStyle : saveBtnStyle} disabled={isDisabled}>
          {createColumn.isPending ? 'Adding…' : 'Add'}
        </button>
        <button type="button" style={cancelBtnStyle} onClick={closeForm}>
          Cancel
        </button>
      </div>
    </form>
  );
}
