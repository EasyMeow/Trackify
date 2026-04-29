import { useState, useRef, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useWorkspaces } from '../hooks/useWorkspaces';
import { useCreateWorkspace } from '../hooks/useCreateWorkspace';
import { useUpdateWorkspace } from '../hooks/useUpdateWorkspace';
import { useDeleteWorkspace } from '../hooks/useDeleteWorkspace';
import type { Workspace } from '../types/workspace';

// ─── styles ──────────────────────────────────────────────────────────────────

const captionStyle = {
  fontSize: 'var(--font-size-xs)',
  color: 'var(--color-text-subtle)',
  fontWeight: 'var(--font-weight-medium)' as const,
  letterSpacing: '0.4px',
  textTransform: 'uppercase' as const,
  marginBottom: 'var(--space-1)',
};

const placeholderStyle = {
  fontSize: 'var(--font-size-sm)',
  color: 'var(--color-text-muted)',
  margin: 0,
};

const listStyle = {
  listStyle: 'none' as const,
  margin: 0,
  padding: 0,
  display: 'flex',
  flexDirection: 'column' as const,
  gap: '2px',
};

const rowStyle = {
  display: 'flex',
  alignItems: 'center',
  gap: '2px',
  borderRadius: 'var(--radius-md)',
};

const workspaceNameButtonStyle = (active: boolean): React.CSSProperties => ({
  flex: 1,
  minWidth: 0,
  textAlign: 'left',
  padding: 'var(--space-2) var(--space-3)',
  fontSize: 'var(--font-size-sm)',
  fontWeight: active ? 'var(--font-weight-semibold)' : 'var(--font-weight-medium)',
  color: active ? 'var(--color-accent)' : 'var(--color-text)',
  backgroundColor: active ? 'var(--color-accent-soft)' : 'transparent',
  border: 'none',
  borderRadius: 'var(--radius-md)',
  cursor: 'pointer',
  overflow: 'hidden',
  textOverflow: 'ellipsis',
  whiteSpace: 'nowrap',
  transition: 'background-color var(--transition-fast), color var(--transition-fast)',
});

const iconButtonStyle: React.CSSProperties = {
  flexShrink: 0,
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  width: '24px',
  height: '24px',
  padding: 0,
  backgroundColor: 'transparent',
  border: 'none',
  borderRadius: 'var(--radius-sm)',
  cursor: 'pointer',
  color: 'var(--color-text-subtle)',
  fontSize: '13px',
  opacity: 0,
  transition: 'opacity var(--transition-fast), background-color var(--transition-fast)',
};

const inlineInputStyle: React.CSSProperties = {
  flex: 1,
  minWidth: 0,
  padding: 'var(--space-1) var(--space-2)',
  fontSize: 'var(--font-size-sm)',
  color: 'var(--color-text)',
  backgroundColor: 'var(--color-bg)',
  border: '1px solid var(--color-accent)',
  borderRadius: 'var(--radius-md)',
  outline: 'none',
};

const newWorkspaceInputStyle: React.CSSProperties = {
  ...inlineInputStyle,
  width: '100%',
  boxSizing: 'border-box',
};

const addButtonStyle: React.CSSProperties = {
  width: '100%',
  textAlign: 'left',
  padding: 'var(--space-2) var(--space-3)',
  fontSize: 'var(--font-size-sm)',
  color: 'var(--color-text-muted)',
  backgroundColor: 'transparent',
  border: '1px dashed var(--color-border)',
  borderRadius: 'var(--radius-md)',
  cursor: 'pointer',
  marginTop: 'var(--space-1)',
  transition: 'border-color var(--transition-fast), color var(--transition-fast)',
};

const overlayStyle: React.CSSProperties = {
  position: 'fixed',
  inset: 0,
  backgroundColor: 'rgba(0,0,0,0.4)',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  zIndex: 1000,
};

const modalStyle: React.CSSProperties = {
  backgroundColor: 'var(--color-surface)',
  border: '1px solid var(--color-border)',
  borderRadius: 'var(--radius-lg)',
  boxShadow: 'var(--shadow-lg)',
  padding: 'var(--space-6)',
  maxWidth: '440px',
  width: '90%',
  display: 'flex',
  flexDirection: 'column',
  gap: 'var(--space-4)',
};

const modalInputStyle: React.CSSProperties = {
  width: '100%',
  padding: 'var(--space-2) var(--space-3)',
  fontSize: 'var(--font-size-sm)',
  color: 'var(--color-text)',
  backgroundColor: 'var(--color-bg)',
  border: '1px solid var(--color-border)',
  borderRadius: 'var(--radius-md)',
  outline: 'none',
  boxSizing: 'border-box',
};

const modalActionsStyle: React.CSSProperties = {
  display: 'flex',
  gap: 'var(--space-2)',
  justifyContent: 'flex-end',
};

const cancelButtonStyle: React.CSSProperties = {
  padding: 'var(--space-2) var(--space-4)',
  fontSize: 'var(--font-size-sm)',
  color: 'var(--color-text-muted)',
  backgroundColor: 'transparent',
  border: '1px solid var(--color-border)',
  borderRadius: 'var(--radius-md)',
  cursor: 'pointer',
};

const confirmDeleteButtonStyle: React.CSSProperties = {
  padding: 'var(--space-2) var(--space-4)',
  fontSize: 'var(--font-size-sm)',
  fontWeight: 'var(--font-weight-medium)',
  color: '#fff',
  backgroundColor: 'var(--color-danger)',
  border: 'none',
  borderRadius: 'var(--radius-md)',
  cursor: 'pointer',
};

const confirmDeleteButtonDisabledStyle: React.CSSProperties = {
  ...confirmDeleteButtonStyle,
  opacity: 0.4,
  cursor: 'not-allowed',
};

// ─── workspace row ────────────────────────────────────────────────────────────

interface WorkspaceRowProps {
  workspace: Workspace;
  active: boolean;
  onSelect: (id: string) => void;
  onDeleteRequest: (workspace: Workspace) => void;
}

function WorkspaceRow({ workspace, active, onSelect, onDeleteRequest }: WorkspaceRowProps) {
  const [editing, setEditing] = useState(false);
  const [draftName, setDraftName] = useState('');
  const [hovered, setHovered] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);
  const updateWorkspace = useUpdateWorkspace();

  function startEdit() {
    setDraftName(workspace.name);
    setEditing(true);
    setTimeout(() => inputRef.current?.focus(), 0);
  }

  function cancelEdit() {
    setEditing(false);
    setDraftName('');
  }

  function commitEdit() {
    const trimmed = draftName.trim();
    if (!trimmed || trimmed === workspace.name) {
      cancelEdit();
      return;
    }
    updateWorkspace.mutate(
      { id: workspace.id, name: trimmed },
      { onSettled: () => setEditing(false) }
    );
  }

  function handleKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'Enter') commitEdit();
    if (e.key === 'Escape') cancelEdit();
  }

  return (
    <li>
      <div
        style={rowStyle}
        onMouseEnter={() => setHovered(true)}
        onMouseLeave={() => setHovered(false)}
      >
        {editing ? (
          <>
            <input
              ref={inputRef}
              style={inlineInputStyle}
              value={draftName}
              onChange={(e) => setDraftName(e.target.value)}
              onKeyDown={handleKeyDown}
              onBlur={commitEdit}
              maxLength={200}
              disabled={updateWorkspace.isPending}
            />
          </>
        ) : (
          <>
            <button
              type="button"
              style={workspaceNameButtonStyle(active)}
              onClick={() => onSelect(workspace.id)}
              title={workspace.name}
            >
              {workspace.name}
            </button>
            <button
              type="button"
              style={{ ...iconButtonStyle, opacity: hovered ? 1 : 0 }}
              onClick={startEdit}
              title="Rename workspace"
              aria-label="Rename workspace"
            >
              ✎
            </button>
            <button
              type="button"
              style={{ ...iconButtonStyle, opacity: hovered ? 1 : 0, color: 'var(--color-danger)' }}
              onClick={() => onDeleteRequest(workspace)}
              title="Delete workspace"
              aria-label="Delete workspace"
            >
              ✕
            </button>
          </>
        )}
      </div>
    </li>
  );
}

// ─── delete confirmation modal ────────────────────────────────────────────────

interface DeleteModalProps {
  workspace: Workspace;
  onCancel: () => void;
  onConfirm: () => void;
  isPending: boolean;
}

function DeleteModal({ workspace, onCancel, onConfirm, isPending }: DeleteModalProps) {
  const [confirmName, setConfirmName] = useState('');
  const canDelete = confirmName === workspace.name && !isPending;

  return (
    <div style={overlayStyle} onClick={onCancel}>
      <div style={modalStyle} onClick={(e) => e.stopPropagation()}>
        <h3 style={{ margin: 0, fontSize: 'var(--font-size-md)', fontWeight: 'var(--font-weight-semibold)', color: 'var(--color-text)' }}>
          Delete "{workspace.name}"?
        </h3>
        <p style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-text-muted)', margin: 0, lineHeight: '1.5' }}>
          This will permanently delete the workspace and all of its projects, tasks, comments, and
          dependencies. To confirm, type the workspace name below.
        </p>
        <input
          style={modalInputStyle}
          value={confirmName}
          onChange={(e) => setConfirmName(e.target.value)}
          placeholder={workspace.name}
          autoFocus
          disabled={isPending}
        />
        <div style={modalActionsStyle}>
          <button style={cancelButtonStyle} onClick={onCancel} disabled={isPending}>
            Cancel
          </button>
          <button
            style={canDelete ? confirmDeleteButtonStyle : confirmDeleteButtonDisabledStyle}
            onClick={onConfirm}
            disabled={!canDelete}
          >
            {isPending ? 'Deleting…' : 'Delete workspace'}
          </button>
        </div>
      </div>
    </div>
  );
}

// ─── create workspace inline form ─────────────────────────────────────────────

interface CreateFormProps {
  onDone: (newId?: string) => void;
}

function CreateWorkspaceForm({ onDone }: CreateFormProps) {
  const [name, setName] = useState('');
  const inputRef = useRef<HTMLInputElement>(null);
  const createWorkspace = useCreateWorkspace();

  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  function handleSubmit() {
    const trimmed = name.trim();
    if (!trimmed) {
      onDone();
      return;
    }
    createWorkspace.mutate(trimmed, {
      onSuccess: (created) => onDone(created.id),
      onError: () => onDone(),
    });
  }

  function handleKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'Enter') handleSubmit();
    if (e.key === 'Escape') onDone();
  }

  return (
    <div style={{ display: 'flex', gap: 'var(--space-1)', marginTop: 'var(--space-1)' }}>
      <input
        ref={inputRef}
        style={newWorkspaceInputStyle}
        value={name}
        onChange={(e) => setName(e.target.value)}
        onKeyDown={handleKeyDown}
        onBlur={handleSubmit}
        placeholder="Workspace name"
        maxLength={200}
        disabled={createWorkspace.isPending}
      />
    </div>
  );
}

// ─── main component ───────────────────────────────────────────────────────────

export function WorkspaceSwitcher() {
  const { data: workspaces, isLoading } = useWorkspaces();
  const [searchParams, setSearchParams] = useSearchParams();
  const [showCreate, setShowCreate] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<Workspace | null>(null);
  const deleteWorkspace = useDeleteWorkspace();

  if (isLoading) {
    return <p style={placeholderStyle}>Loading workspaces…</p>;
  }

  if (!workspaces || workspaces.length === 0) {
    return (
      <div>
        <p style={captionStyle}>Workspaces</p>
        <p style={placeholderStyle}>No workspaces yet</p>
        {showCreate ? (
          <CreateWorkspaceForm
            onDone={(newId) => {
              setShowCreate(false);
              if (newId) {
                const next = new URLSearchParams(searchParams);
                next.set('workspace', newId);
                setSearchParams(next);
              }
            }}
          />
        ) : (
          <button type="button" style={addButtonStyle} onClick={() => setShowCreate(true)}>
            + New workspace
          </button>
        )}
      </div>
    );
  }

  const urlValue = searchParams.get('workspace');
  const selectedId = workspaces.some((w) => w.id === urlValue)
    ? (urlValue as string)
    : workspaces[0].id;

  function handleSelect(id: string) {
    const next = new URLSearchParams(searchParams);
    next.set('workspace', id);
    setSearchParams(next);
  }

  function handleDeleteConfirm() {
    if (!deleteTarget) return;
    const targetId = deleteTarget.id;
    deleteWorkspace.mutate(targetId, {
      onSuccess: () => {
        setDeleteTarget(null);
        // If we just deleted the selected workspace, switch to first remaining one
        if (selectedId === targetId) {
          const remaining = (workspaces ?? []).filter((w) => w.id !== targetId);
          const next = new URLSearchParams(searchParams);
          if (remaining.length > 0) {
            next.set('workspace', remaining[0].id);
          } else {
            next.delete('workspace');
          }
          setSearchParams(next);
        }
      },
    });
  }

  return (
    <div>
      <p style={captionStyle}>Workspaces</p>
      <ul style={listStyle}>
        {workspaces.map((workspace) => (
          <WorkspaceRow
            key={workspace.id}
            workspace={workspace}
            active={workspace.id === selectedId}
            onSelect={handleSelect}
            onDeleteRequest={setDeleteTarget}
          />
        ))}
      </ul>

      {showCreate ? (
        <CreateWorkspaceForm
          onDone={(newId) => {
            setShowCreate(false);
            if (newId) {
              const next = new URLSearchParams(searchParams);
              next.set('workspace', newId);
              setSearchParams(next);
            }
          }}
        />
      ) : (
        <button type="button" style={addButtonStyle} onClick={() => setShowCreate(true)}>
          + New workspace
        </button>
      )}

      {deleteTarget && (
        <DeleteModal
          workspace={deleteTarget}
          onCancel={() => setDeleteTarget(null)}
          onConfirm={handleDeleteConfirm}
          isPending={deleteWorkspace.isPending}
        />
      )}
    </div>
  );
}
