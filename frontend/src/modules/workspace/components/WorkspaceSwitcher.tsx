import type { ChangeEvent } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useWorkspaces } from '../hooks/useWorkspaces';

const labelStyle = {
  display: 'flex',
  flexDirection: 'column' as const,
  gap: 'var(--space-1)',
};

const captionStyle = {
  fontSize: 'var(--font-size-xs)',
  color: 'var(--color-text-subtle)',
  fontWeight: 'var(--font-weight-medium)',
  letterSpacing: '0.4px',
  textTransform: 'uppercase' as const,
};

const placeholderStyle = {
  fontSize: 'var(--font-size-sm)',
  color: 'var(--color-text-muted)',
  margin: 0,
};

const selectStyle = {
  width: '100%',
  padding: 'var(--space-2) var(--space-3)',
  fontSize: 'var(--font-size-sm)',
  color: 'var(--color-text)',
  backgroundColor: 'var(--color-surface)',
  border: '1px solid var(--color-border)',
  borderRadius: 'var(--radius-md)',
  outline: 'none',
  cursor: 'pointer',
};

export function WorkspaceSwitcher() {
  const { data: workspaces, isLoading } = useWorkspaces();
  const [searchParams, setSearchParams] = useSearchParams();

  if (isLoading) {
    return <p style={placeholderStyle}>Loading workspaces…</p>;
  }

  if (!workspaces || workspaces.length === 0) {
    return <p style={placeholderStyle}>No workspaces yet</p>;
  }

  const urlValue = searchParams.get('workspace');
  const selectedId = workspaces.some((workspace) => workspace.id === urlValue)
    ? (urlValue as string)
    : workspaces[0].id;

  const handleChange = (event: ChangeEvent<HTMLSelectElement>) => {
    const next = new URLSearchParams(searchParams);
    next.set('workspace', event.target.value);
    setSearchParams(next);
  };

  return (
    <label style={labelStyle}>
      <span style={captionStyle}>Workspace</span>
      <select value={selectedId} onChange={handleChange} style={selectStyle}>
        {workspaces.map((workspace) => (
          <option key={workspace.id} value={workspace.id}>
            {workspace.name}
          </option>
        ))}
      </select>
    </label>
  );
}
