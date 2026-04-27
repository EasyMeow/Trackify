import { useEffect } from 'react';
import { format, parseISO } from 'date-fns';
import { useTaskDetail } from '../hooks/useTaskDetail';

interface TaskDrawerProps {
  taskId: string | null;
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

const titleStyle: React.CSSProperties = {
  margin: 0,
  fontSize: 'var(--font-size-xl)',
  fontWeight: 'var(--font-weight-semibold)',
  color: 'var(--color-text)',
  lineHeight: 'var(--line-height-snug)',
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

const priorityBadgeStyle = (priority: string): React.CSSProperties => ({
  display: 'inline-block',
  padding: '2px var(--space-2)',
  fontSize: 'var(--font-size-xs)',
  fontWeight: 'var(--font-weight-medium)',
  borderRadius: 'var(--radius-pill)',
  backgroundColor: 'var(--color-warning-soft)',
  color: 'var(--color-warning)',
  textTransform: 'capitalize',
  ...(priority === 'LOW' ? { backgroundColor: 'var(--color-bg-muted)', color: 'var(--color-text-muted)' } : {}),
  ...(priority === 'URGENT' ? { backgroundColor: 'var(--color-danger-soft)', color: 'var(--color-danger)' } : {}),
  ...(priority === 'HIGH' ? { backgroundColor: 'var(--color-warning-soft)', color: 'var(--color-warning)' } : {}),
});

export function TaskDrawer({ taskId, onClose }: TaskDrawerProps) {
  const { data, isLoading, isError } = useTaskDetail(taskId);

  useEffect(() => {
    if (!taskId) return;
    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === 'Escape') onClose();
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
        <div style={headerStyle}>
          {isLoading || isError || !data ? (
            <p
              id={titleId}
              style={isError ? { ...valueStyle, color: 'var(--color-danger)', margin: 0 } : mutedValueStyle}
            >
              {isLoading ? 'Loading task…' : 'Couldn\'t load task.'}
            </p>
          ) : (
            <h2 id={titleId} style={titleStyle}>{data.title}</h2>
          )}
          <button type="button" style={closeBtnStyle} onClick={onClose} aria-label="Close task drawer">
            ✕
          </button>
        </div>

        {data && (
          <div style={bodyStyle}>
            <div style={fieldStyle}>
              <span style={labelStyle}>Description</span>
              {data.description ? (
                <p style={valueStyle}>{data.description}</p>
              ) : (
                <p style={mutedValueStyle}>No description</p>
              )}
            </div>

            <div style={metaRowStyle}>
              <div style={fieldStyle}>
                <span style={labelStyle}>Status</span>
                <span style={statusBadgeStyle(data.status)}>{data.status.replace('_', ' ')}</span>
              </div>
              <div style={fieldStyle}>
                <span style={labelStyle}>Priority</span>
                <span style={priorityBadgeStyle(data.priority)}>{data.priority}</span>
              </div>
            </div>

            <div style={metaRowStyle}>
              <div style={fieldStyle}>
                <span style={labelStyle}>Start date</span>
                <p style={valueStyle}>{fmtDate(data.startDate)}</p>
              </div>
              <div style={fieldStyle}>
                <span style={labelStyle}>Due date</span>
                <p style={valueStyle}>{fmtDate(data.dueDate)}</p>
              </div>
            </div>
          </div>
        )}

        {data && (
          <div style={footerStyle}>
            <span>Created: {fmtDate(data.createdAt)}</span>
            <span>Updated: {fmtDate(data.updatedAt)}</span>
          </div>
        )}
      </div>
    </>
  );
}
