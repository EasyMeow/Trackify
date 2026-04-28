import { Link, NavLink } from 'react-router-dom';

const wrapperStyle: React.CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  gap: 'var(--space-3)',
  flexWrap: 'wrap',
};

const backLinkStyle: React.CSSProperties = {
  fontSize: 'var(--font-size-sm)',
  color: 'var(--color-text-muted)',
  textDecoration: 'none',
};

const tabsStyle: React.CSSProperties = {
  display: 'flex',
  gap: 'var(--space-1)',
  padding: 'var(--space-1)',
  backgroundColor: 'var(--color-bg-muted)',
  borderRadius: 'var(--radius-md)',
};

const baseTabStyle: React.CSSProperties = {
  padding: 'var(--space-1) var(--space-3)',
  fontSize: 'var(--font-size-sm)',
  fontWeight: 'var(--font-weight-medium)',
  borderRadius: 'var(--radius-sm)',
  textDecoration: 'none',
  transition: 'background-color var(--transition-fast), color var(--transition-fast)',
};

const activeTabStyle: React.CSSProperties = {
  ...baseTabStyle,
  backgroundColor: 'var(--color-accent)',
  color: 'var(--color-text-on-accent)',
};

const inactiveTabStyle: React.CSSProperties = {
  ...baseTabStyle,
  color: 'var(--color-text-muted)',
};

const tabs = [
  { to: 'board', label: 'Board' },
  { to: 'timeline', label: 'Timeline' },
  { to: 'settings', label: 'Settings' },
];

export function ProjectNav({ projectId }: { projectId: string | undefined }) {
  if (!projectId) return null;

  return (
    <nav style={wrapperStyle} aria-label="Project navigation">
      <Link to="/" style={backLinkStyle}>
        ← Dashboard
      </Link>
      <div style={tabsStyle} role="tablist">
        {tabs.map((tab) => (
          <NavLink
            key={tab.to}
            to={`/projects/${projectId}/${tab.to}`}
            style={({ isActive }) => (isActive ? activeTabStyle : inactiveTabStyle)}
          >
            {tab.label}
          </NavLink>
        ))}
      </div>
    </nav>
  );
}
