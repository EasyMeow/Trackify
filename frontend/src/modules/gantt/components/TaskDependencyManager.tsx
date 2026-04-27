import { useMemo, useState } from 'react';
import { useProjectTimeline } from '../hooks/useProjectTimeline';
import { useCreateDependency } from '../hooks/useCreateDependency';
import { useDeleteDependency } from '../hooks/useDeleteDependency';
import { ApiError } from '../../../shared/api/httpClient';

interface TaskDependencyManagerProps {
  taskId: string;
  projectId: string;
}

export function TaskDependencyManager({ taskId, projectId }: TaskDependencyManagerProps) {
  const { data, isLoading, isError } = useProjectTimeline(projectId);
  const createDependency = useCreateDependency(projectId);
  const deleteDependency = useDeleteDependency(projectId);

  const [selectedPredecessorId, setSelectedPredecessorId] = useState<string>('');

  // Edges where this task is the successor — its predecessors.
  const incomingEdges = useMemo(
    () => (data?.dependencies ?? []).filter((d) => d.successorTaskId === taskId),
    [data, taskId]
  );

  // Title lookup by task id (for showing predecessor names in the list).
  const titleById = useMemo(() => {
    const map = new Map<string, string>();
    for (const t of data?.tasks ?? []) map.set(t.id, t.title);
    return map;
  }, [data]);

  // Candidate predecessors: every task in the project except this one and any
  // already-existing predecessor. Cycle prevention is enforced server-side; if
  // the user picks a task that would form a cycle the backend rejects it.
  const candidates = useMemo(() => {
    const existingPredecessorIds = new Set(incomingEdges.map((e) => e.predecessorTaskId));
    return (data?.tasks ?? []).filter(
      (t) => t.id !== taskId && !existingPredecessorIds.has(t.id)
    );
  }, [data, taskId, incomingEdges]);

  const createError =
    createDependency.error instanceof ApiError
      ? createDependency.error.message
      : createDependency.error
        ? 'Failed to add dependency.'
        : null;

  function handleAdd() {
    if (!selectedPredecessorId) return;
    createDependency.mutate(
      { taskId, predecessorTaskId: selectedPredecessorId },
      {
        onSuccess: () => {
          setSelectedPredecessorId('');
        },
      }
    );
  }

  function handleDelete(dependencyId: string) {
    deleteDependency.mutate({ taskId, dependencyId });
  }

  return (
    <div style={fieldStyle}>
      <span style={labelStyle}>Depends on</span>

      {isLoading && <p style={mutedTextStyle}>Loading dependencies…</p>}
      {isError && <p style={errorTextStyle}>Couldn't load dependencies.</p>}

      {!isLoading && !isError && incomingEdges.length === 0 && (
        <p style={mutedTextStyle}>No dependencies.</p>
      )}

      {incomingEdges.length > 0 && (
        <ul style={listStyle}>
          {incomingEdges.map((edge) => (
            <li key={edge.id} style={listItemStyle}>
              <span style={depTitleStyle}>
                {titleById.get(edge.predecessorTaskId) ?? '(unknown task)'}
              </span>
              <button
                type="button"
                onClick={() => handleDelete(edge.id)}
                disabled={deleteDependency.isPending}
                style={removeButtonStyle}
                aria-label="Remove dependency"
              >
                Remove
              </button>
            </li>
          ))}
        </ul>
      )}

      {!isLoading && !isError && (
        <div style={addRowStyle}>
          <select
            value={selectedPredecessorId}
            disabled={createDependency.isPending || candidates.length === 0}
            onChange={(e) => setSelectedPredecessorId(e.target.value)}
            style={selectStyle}
            aria-label="Select predecessor task"
          >
            <option value="">
              {candidates.length === 0 ? 'No tasks available' : 'Select a task…'}
            </option>
            {candidates.map((t) => (
              <option key={t.id} value={t.id}>
                {t.title}
              </option>
            ))}
          </select>
          <button
            type="button"
            onClick={handleAdd}
            disabled={!selectedPredecessorId || createDependency.isPending}
            style={
              !selectedPredecessorId || createDependency.isPending
                ? addButtonDisabledStyle
                : addButtonStyle
            }
          >
            {createDependency.isPending ? 'Adding…' : 'Add'}
          </button>
        </div>
      )}

      {createError && <p style={errorTextStyle}>{createError}</p>}
    </div>
  );
}

const fieldStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  gap: 'var(--space-2)',
};

const labelStyle: React.CSSProperties = {
  fontSize: 'var(--font-size-xs)',
  fontWeight: 'var(--font-weight-semibold)',
  color: 'var(--color-text-subtle)',
  textTransform: 'uppercase',
  letterSpacing: '0.05em',
};

const mutedTextStyle: React.CSSProperties = {
  fontSize: 'var(--font-size-sm)',
  color: 'var(--color-text-muted)',
  fontStyle: 'italic',
  margin: 0,
};

const errorTextStyle: React.CSSProperties = {
  fontSize: 'var(--font-size-xs)',
  color: 'var(--color-danger)',
  margin: 0,
};

const listStyle: React.CSSProperties = {
  listStyle: 'none',
  margin: 0,
  padding: 0,
  display: 'flex',
  flexDirection: 'column',
  gap: 'var(--space-1)',
};

const listItemStyle: React.CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'space-between',
  gap: 'var(--space-2)',
  padding: 'var(--space-1) var(--space-2)',
  fontSize: 'var(--font-size-sm)',
  color: 'var(--color-text)',
  backgroundColor: 'var(--color-bg)',
  border: '1px solid var(--color-border)',
  borderRadius: 'var(--radius-md)',
};

const depTitleStyle: React.CSSProperties = {
  flex: 1,
  minWidth: 0,
  overflow: 'hidden',
  textOverflow: 'ellipsis',
  whiteSpace: 'nowrap',
};

const removeButtonStyle: React.CSSProperties = {
  flexShrink: 0,
  padding: '2px var(--space-2)',
  fontSize: 'var(--font-size-xs)',
  color: 'var(--color-text-muted)',
  backgroundColor: 'transparent',
  border: '1px solid var(--color-border)',
  borderRadius: 'var(--radius-md)',
  cursor: 'pointer',
};

const addRowStyle: React.CSSProperties = {
  display: 'flex',
  gap: 'var(--space-2)',
};

const selectStyle: React.CSSProperties = {
  flex: 1,
  minWidth: 0,
  padding: 'var(--space-1) var(--space-2)',
  fontSize: 'var(--font-size-sm)',
  color: 'var(--color-text)',
  backgroundColor: 'var(--color-bg)',
  border: '1px solid var(--color-border)',
  borderRadius: 'var(--radius-md)',
  outline: 'none',
  fontFamily: 'inherit',
};

const addButtonStyle: React.CSSProperties = {
  flexShrink: 0,
  padding: 'var(--space-1) var(--space-3)',
  fontSize: 'var(--font-size-sm)',
  fontWeight: 'var(--font-weight-semibold)',
  color: 'var(--color-text-on-accent)',
  backgroundColor: 'var(--color-accent)',
  border: 'none',
  borderRadius: 'var(--radius-md)',
  cursor: 'pointer',
};

const addButtonDisabledStyle: React.CSSProperties = {
  ...addButtonStyle,
  opacity: 0.5,
  cursor: 'not-allowed',
};
