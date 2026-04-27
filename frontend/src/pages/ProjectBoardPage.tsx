import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { useBoardQuery } from '../modules/kanban/hooks/useBoardQuery';
import { useCreateTask } from '../modules/kanban/hooks/useCreateTask';
import { BoardColumnView } from '../modules/kanban/components/BoardColumnView';
import { useSelectedTaskId } from '../modules/task/hooks/useSelectedTaskId';
import { TaskDrawer } from '../modules/task/components/TaskDrawer';
import { ApiError } from '../shared/api/httpClient';

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

export default function ProjectBoardPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const { data, isLoading, isError } = useBoardQuery(projectId);
  const [selectedTaskId, setSelectedTaskId] = useSelectedTaskId();

  return (
    <section style={pageStyle}>
      <h1 style={headingStyle}>Board</h1>
      <CreateTaskRow projectId={projectId} />

      {isLoading && <p style={mutedStyle}>Loading board…</p>}
      {isError && <p style={{ ...mutedStyle, color: 'var(--color-danger)' }}>Couldn't load board.</p>}
      {data && (
        <div style={boardStyle}>
          {data.columns.map((column) => (
            <BoardColumnView
              key={column.id}
              column={column}
              selectedTaskId={selectedTaskId}
              onSelectTask={setSelectedTaskId}
            />
          ))}
        </div>
      )}
      <TaskDrawer taskId={selectedTaskId} onClose={() => setSelectedTaskId(null)} />
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
