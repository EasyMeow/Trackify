import { useEffect, useRef, useState } from 'react';
import { format, parseISO } from 'date-fns';
import { useTaskDetail } from '../hooks/useTaskDetail';
import { useUpdateTask } from '../hooks/useUpdateTask';
import type { TaskPriority, TaskResponse } from '../../kanban/types/task';
import { CommentList } from '../../comment/components/CommentList';

const PRIORITY_OPTIONS: { value: TaskPriority; label: string }[] = [
  { value: 'LOW', label: 'Low' },
  { value: 'MEDIUM', label: 'Medium' },
  { value: 'HIGH', label: 'High' },
  { value: 'URGENT', label: 'Urgent' },
];

interface TaskDrawerProps {
  taskId: string | null;
  projectId: string;
  onClose: () => void;
}

function fmtDate(iso: string | null): string {
  if (!iso) return '—';
  try {
    return format(parseISO(iso), 'PP');
  } catch {
    return iso;
  }
}

// ---------- styles ----------

const backdropStyle: React.CSSProperties = {
  position: 'fixed',
  inset: 0,
  backgroundColor: 'rgba(31, 42, 35, 0.35)',
  zIndex: 'var(--z-drawer)' as React.CSSProperties['zIndex'],
};

const panelStyle: React.CSSProperties = {
  position: 'fixed',
  top: 0,
  right: 0,
  bottom: 0,
  width: 'var(--layout-drawer-width)',
  backgroundColor: 'var(--color-surface)',
  borderLeft: '1px solid var(--color-border)',
  boxShadow: 'var(--shadow-lg)',
  zIndex: 'calc(var(--z-drawer) + 1)' as React.CSSProperties['zIndex'],
  display: 'flex',
  flexDirection: 'column',
  overflowY: 'auto',
};

const headerStyle: React.CSSProperties = {
  display: 'flex',
  alignItems: 'flex-start',
  justifyContent: 'space-between',
  gap: 'var(--space-3)',
  padding: 'var(--space-5) var(--space-5) var(--space-3)',
  borderBottom: '1px solid var(--color-border)',
};

const closeBtnStyle: React.CSSProperties = {
  flexShrink: 0,
  padding: 'var(--space-1) var(--space-2)',
  fontSize: 'var(--font-size-sm)',
  color: 'var(--color-text-muted)',
  backgroundColor: 'transparent',
  border: '1px solid var(--color-border)',
  borderRadius: 'var(--radius-md)',
  cursor: 'pointer',
  lineHeight: 1,
};

const bodyStyle: React.CSSProperties = {
  padding: 'var(--space-4) var(--space-5)',
  display: 'flex',
  flexDirection: 'column',
  gap: 'var(--space-4)',
  flex: 1,
};

const labelStyle: React.CSSProperties = {
  fontSize: 'var(--font-size-xs)',
  fontWeight: 'var(--font-weight-semibold)',
  color: 'var(--color-text-subtle)',
  textTransform: 'uppercase',
  letterSpacing: '0.05em',
  marginBottom: 'var(--space-1)',
};

const valueStyle: React.CSSProperties = {
  fontSize: 'var(--font-size-sm)',
  color: 'var(--color-text)',
  margin: 0,
};

const mutedValueStyle: React.CSSProperties = {
  ...valueStyle,
  color: 'var(--color-text-muted)',
  fontStyle: 'italic',
};

const metaRowStyle: React.CSSProperties = {
  display: 'grid',
  gridTemplateColumns: '1fr 1fr',
  gap: 'var(--space-4)',
};

const fieldStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
};

const footerStyle: React.CSSProperties = {
  padding: 'var(--space-3) var(--space-5)',
  borderTop: '1px solid var(--color-border)',
  fontSize: 'var(--font-size-xs)',
  color: 'var(--color-text-subtle)',
  display: 'flex',
  flexDirection: 'column',
  gap: 'var(--space-1)',
};

const statusBadgeStyle = (status: string): React.CSSProperties => ({
  display: 'inline-block',
  padding: '2px var(--space-2)',
  fontSize: 'var(--font-size-xs)',
  fontWeight: 'var(--font-weight-medium)',
  borderRadius: 'var(--radius-pill)',
  backgroundColor: 'var(--color-accent-soft)',
  color: 'var(--color-accent-active)',
  textTransform: 'capitalize',
  ...(status === 'DONE' ? { backgroundColor: 'var(--color-success-soft)', color: 'var(--color-success)' } : {}),
  ...(status === 'IN_PROGRESS' ? { backgroundColor: 'var(--color-info-soft)', color: 'var(--color-info)' } : {}),
});

const editInputBaseStyle: React.CSSProperties = {
  width: '100%',
  boxSizing: 'border-box',
  padding: 'var(--space-1) var(--space-2)',
  fontSize: 'var(--font-size-xl)',
  fontWeight: 'var(--font-weight-semibold)',
  color: 'var(--color-text)',
  backgroundColor: 'var(--color-bg)',
  border: '1px solid var(--color-border)',
  borderRadius: 'var(--radius-md)',
  outline: 'none',
  lineHeight: 'var(--line-height-snug)',
  fontFamily: 'inherit',
};

const editInputErrorStyle: React.CSSProperties = {
  ...editInputBaseStyle,
  borderColor: 'var(--color-danger)',
};

const editTextareaStyle: React.CSSProperties = {
  width: '100%',
  boxSizing: 'border-box',
  padding: 'var(--space-2)',
  fontSize: 'var(--font-size-sm)',
  color: 'var(--color-text)',
  backgroundColor: 'var(--color-bg)',
  border: '1px solid var(--color-border)',
  borderRadius: 'var(--radius-md)',
  outline: 'none',
  fontFamily: 'inherit',
  resize: 'vertical',
  minHeight: '80px',
  lineHeight: 'var(--line-height-normal)',
};

const inlineErrorStyle: React.CSSProperties = {
  fontSize: 'var(--font-size-xs)',
  color: 'var(--color-danger)',
  marginTop: 'var(--space-1)',
};

const editControlStyle: React.CSSProperties = {
  width: '100%',
  boxSizing: 'border-box',
  padding: 'var(--space-1) var(--space-2)',
  fontSize: 'var(--font-size-sm)',
  color: 'var(--color-text)',
  backgroundColor: 'var(--color-bg)',
  border: '1px solid var(--color-border)',
  borderRadius: 'var(--radius-md)',
  outline: 'none',
  fontFamily: 'inherit',
  lineHeight: 'var(--line-height-snug)',
};

// ---------- inner content (keyed by task id so useState initializers reset on new task) ----------

interface DrawerContentProps {
  data: TaskResponse;
  taskId: string;
  projectId: string;
  onClose: () => void;
  // parent passes a setter so the outer Escape handler can cancel an in-progress edit
  registerCancelEdit: (fn: (() => void) | null) => void;
}

function DrawerContent({ data, taskId, projectId, onClose, registerCancelEdit }: DrawerContentProps) {
  const { mutate: updateTask, isPending } = useUpdateTask(taskId, projectId);

  // draft values — only used while the field is focused; start from current server value
  const [titleDraft, setTitleDraft] = useState(data.title);
  const [titleError, setTitleError] = useState('');
  const [descDraft, setDescDraft] = useState(data.description ?? '');

  // which field (if any) is currently focused
  const [editingField, setEditingField] = useState<'title' | 'description' | null>(null);

  // Keep the outer shell's cancel-edit reference up to date.
  // We only register a cancel function while the user is actively editing.
  useEffect(() => {
    if (editingField === null) {
      registerCancelEdit(null);
      return;
    }
    registerCancelEdit(() => {
      if (editingField === 'title') {
        setTitleDraft(data.title);
        setTitleError('');
      }
      if (editingField === 'description') {
        setDescDraft(data.description ?? '');
      }
      setEditingField(null);
      (document.activeElement as HTMLElement | null)?.blur();
    });
  }, [editingField, data, registerCancelEdit]);

  // Note: no separate data-sync effect is needed. After a successful save the
  // mutation echoes the submitted value back into the TanStack Query cache, and
  // the user has already blurred (editingField === null) before the response
  // arrives, so the draft already matches the server value.
  // The `key={data.id}` on DrawerContent ensures fresh state when a new task
  // is opened.

  const titleId = 'task-drawer-title';

  function commitTitle() {
    setEditingField(null);
    const trimmed = titleDraft.trim();
    if (!trimmed) {
      setTitleError('Title cannot be blank.');
      setTitleDraft(data.title);
      return;
    }
    if (trimmed === data.title) return;
    setTitleError('');
    updateTask({ title: trimmed });
  }

  function commitDescription() {
    setEditingField(null);
    const current = data.description ?? '';
    if (descDraft === current) return;
    // Backend treats null as "keep unchanged" and empty string as "clear" (TASK-055).
    // Send the draft string directly so a cleared textarea actually persists.
    updateTask({ description: descDraft });
  }

  return (
    <>
      <div style={headerStyle}>
        <div style={{ flex: 1, minWidth: 0 }}>
          <input
            id={titleId}
            type="text"
            value={titleDraft}
            disabled={isPending}
            style={titleError ? editInputErrorStyle : editInputBaseStyle}
            maxLength={255}
            aria-label="Task title"
            onFocus={() => setEditingField('title')}
            onChange={(e) => {
              setTitleDraft(e.target.value);
              if (titleError && e.target.value.trim()) setTitleError('');
            }}
            onBlur={commitTitle}
            onKeyDown={(e) => {
              if (e.key === 'Enter') {
                e.preventDefault();
                e.currentTarget.blur();
              }
              // Escape is handled by the document-level listener in the outer shell
            }}
          />
          {titleError && <p style={inlineErrorStyle}>{titleError}</p>}
        </div>
        <button type="button" style={closeBtnStyle} onClick={onClose} aria-label="Close task drawer">
          ✕
        </button>
      </div>

      <div style={bodyStyle}>
        <div style={fieldStyle}>
          <span style={labelStyle}>Description</span>
          <textarea
            value={descDraft}
            disabled={isPending}
            style={editTextareaStyle}
            placeholder="No description"
            aria-label="Task description"
            onFocus={() => setEditingField('description')}
            onChange={(e) => setDescDraft(e.target.value)}
            onBlur={commitDescription}
            // Shift+Enter inserts newline (default textarea behavior).
            // Plain Enter also inserts newline — no submit on Enter for multi-line fields.
            // Escape is handled by the document-level listener.
          />
          {isPending && (
            <p style={{ ...mutedValueStyle, marginTop: 'var(--space-1)' }}>Saving…</p>
          )}
        </div>

        <div style={metaRowStyle}>
          <div style={fieldStyle}>
            <span style={labelStyle}>Status</span>
            <span style={statusBadgeStyle(data.status)}>{data.status.replace('_', ' ')}</span>
          </div>
          <div style={fieldStyle}>
            <span style={labelStyle}>Priority</span>
            <select
              style={editControlStyle}
              value={data.priority}
              disabled={isPending}
              aria-label="Task priority"
              onChange={(e) => {
                const next = e.target.value as TaskPriority;
                if (next === data.priority) return;
                updateTask({ priority: next });
              }}
            >
              {PRIORITY_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div style={metaRowStyle}>
          <div style={fieldStyle}>
            <span style={labelStyle}>Start date</span>
            <input
              type="date"
              style={editControlStyle}
              value={data.startDate ?? ''}
              disabled={isPending}
              aria-label="Task start date"
              max={data.dueDate ?? undefined}
              onChange={(e) => {
                const next = e.target.value;
                if (!next) return;
                if (next === (data.startDate ?? '')) return;
                updateTask({ startDate: next });
              }}
            />
          </div>
          <div style={fieldStyle}>
            <span style={labelStyle}>Due date</span>
            <input
              type="date"
              style={editControlStyle}
              value={data.dueDate ?? ''}
              disabled={isPending}
              aria-label="Task due date"
              min={data.startDate ?? undefined}
              onChange={(e) => {
                const next = e.target.value;
                if (!next) return;
                if (next === (data.dueDate ?? '')) return;
                updateTask({ dueDate: next });
              }}
            />
          </div>
        </div>

        <CommentList taskId={taskId} />
      </div>

      <div style={footerStyle}>
        <span>Created: {fmtDate(data.createdAt)}</span>
        <span>Updated: {fmtDate(data.updatedAt)}</span>
      </div>
    </>
  );
}

// ---------- outer shell ----------

export function TaskDrawer({ taskId, projectId, onClose }: TaskDrawerProps) {
  const { data, isLoading, isError } = useTaskDetail(taskId);

  // cancelEditRef holds a function that DrawerContent registers when a field is focused.
  // The Escape handler calls it to cancel the edit instead of closing the drawer.
  const cancelEditRef = useRef<(() => void) | null>(null);

  useEffect(() => {
    if (!taskId) return;
    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === 'Escape') {
        if (cancelEditRef.current) {
          cancelEditRef.current();
          cancelEditRef.current = null;
          e.stopPropagation();
        } else {
          onClose();
        }
      }
    }
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [taskId, onClose]);

  if (!taskId) return null;

  const titleId = 'task-drawer-title';

  return (
    <>
      <div style={backdropStyle} onClick={onClose} aria-hidden="true" />
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        style={panelStyle}
      >
        {(isLoading || isError || !data) ? (
          <div style={headerStyle}>
            <p
              id={titleId}
              style={isError ? { ...valueStyle, color: 'var(--color-danger)', margin: 0 } : mutedValueStyle}
            >
              {isLoading ? 'Loading task…' : 'Couldn\'t load task.'}
            </p>
            <button type="button" style={closeBtnStyle} onClick={onClose} aria-label="Close task drawer">
              ✕
            </button>
          </div>
        ) : (
          <DrawerContent
            key={data.id}
            data={data}
            taskId={taskId}
            projectId={projectId}
            onClose={onClose}
            registerCancelEdit={(fn) => { cancelEditRef.current = fn; }}
          />
        )}
      </div>
    </>
  );
}
