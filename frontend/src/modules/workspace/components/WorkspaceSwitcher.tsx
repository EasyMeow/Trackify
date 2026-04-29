import { useSearchParams } from 'react-router-dom';
import { useWorkspaces } from '../hooks/useWorkspaces';

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

function workspaceItemStyle(active: boolean) {
  return {
    width: '100%',
    textAlign: 'left' as const,
    padding: 'var(--space-2) var(--space-3)',
    fontSize: 'var(--font-size-sm)',
    fontWeight: active
      ? ('var(--font-weight-semibold)' as const)
      : ('var(--font-weight-medium)' as const),
    color: active ? 'var(--color-accent)' : 'var(--color-text)',
    backgroundColor: active ? 'var(--color-accent-soft)' : 'transparent',
    border: 'none',
    borderRadius: 'var(--radius-md)',
    cursor: 'pointer',
    transition: 'background-color var(--transition-fast), color var(--transition-fast)',
    overflow: 'hidden' as const,
    textOverflow: 'ellipsis' as const,
    whiteSpace: 'nowrap' as const,
  };
}

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

  const handleSelect = (id: string) => {
    const next = new URLSearchParams(searchParams);
    next.set('workspace', id);
    setSearchParams(next);
  };

  return (
    <div>
      <p style={captionStyle}>Workspaces</p>
      <ul style={listStyle}>
        {workspaces.map((workspace) => {
          const active = workspace.id === selectedId;
          return (
            <li key={workspace.id}>
              <button
                type="button"
                onClick={() => handleSelect(workspace.id)}
                style={workspaceItemStyle(active)}
                title={workspace.name}
              >
                {workspace.name}
              </button>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
