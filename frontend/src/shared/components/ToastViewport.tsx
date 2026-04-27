import { useToastStore } from '../state/toastStore';
import type { Toast, ToastKind } from '../state/toastStore';

// ---------- styles ----------

const viewportStyle: React.CSSProperties = {
  position: 'fixed',
  bottom: 'var(--space-5)',
  right: 'var(--space-5)',
  display: 'flex',
  flexDirection: 'column',
  gap: 'var(--space-2)',
  zIndex: 'var(--z-toast)' as React.CSSProperties['zIndex'],
  pointerEvents: 'none',
};

function toastBgColor(kind: ToastKind): string {
  switch (kind) {
    case 'error':
      return 'var(--color-danger-soft)';
    case 'warning':
      return 'var(--color-warning-soft)';
    case 'info':
      return 'var(--color-info-soft)';
  }
}

function toastTextColor(kind: ToastKind): string {
  switch (kind) {
    case 'error':
      return 'var(--color-danger)';
    case 'warning':
      return 'var(--color-warning)';
    case 'info':
      return 'var(--color-info)';
  }
}

function toastBorderColor(kind: ToastKind): string {
  switch (kind) {
    case 'error':
      return 'var(--color-danger)';
    case 'warning':
      return 'var(--color-warning)';
    case 'info':
      return 'var(--color-info)';
  }
}

function toastStyle(kind: ToastKind): React.CSSProperties {
  return {
    pointerEvents: 'auto',
    display: 'flex',
    alignItems: 'flex-start',
    justifyContent: 'space-between',
    gap: 'var(--space-3)',
    padding: 'var(--space-3) var(--space-4)',
    maxWidth: '360px',
    minWidth: '240px',
    backgroundColor: toastBgColor(kind),
    color: toastTextColor(kind),
    border: `1px solid ${toastBorderColor(kind)}`,
    borderRadius: 'var(--radius-md)',
    boxShadow: 'var(--shadow-md)',
    fontSize: 'var(--font-size-sm)',
    lineHeight: 'var(--line-height-snug)',
  };
}

const dismissBtnStyle: React.CSSProperties = {
  flexShrink: 0,
  background: 'none',
  border: 'none',
  cursor: 'pointer',
  fontSize: 'var(--font-size-sm)',
  lineHeight: 1,
  padding: 0,
  color: 'inherit',
  opacity: 0.6,
};

// ---------- component ----------

function ToastItem({ toast, onDismiss }: { toast: Toast; onDismiss: (id: string) => void }) {
  return (
    <div role="alert" aria-live="assertive" style={toastStyle(toast.kind)}>
      <span style={{ flex: 1 }}>{toast.message}</span>
      <button
        type="button"
        style={dismissBtnStyle}
        aria-label="Dismiss notification"
        onClick={() => onDismiss(toast.id)}
      >
        ✕
      </button>
    </div>
  );
}

export function ToastViewport() {
  const toasts = useToastStore((s) => s.toasts);
  const dismissToast = useToastStore((s) => s.dismissToast);

  if (toasts.length === 0) return null;

  return (
    <div style={viewportStyle} aria-label="Notifications">
      {toasts.map((t) => (
        <ToastItem key={t.id} toast={t} onDismiss={dismissToast} />
      ))}
    </div>
  );
}
