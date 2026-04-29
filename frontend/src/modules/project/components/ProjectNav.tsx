import React, { useRef, useState, useLayoutEffect } from 'react';
import { Link, NavLink, useLocation } from 'react-router-dom';

const prefersReducedMotion =
  typeof window !== 'undefined' &&
  window.matchMedia('(prefers-reduced-motion: reduce)').matches;

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

const tabsContainerStyle: React.CSSProperties = {
  display: 'flex',
  gap: 'var(--space-1)',
  padding: 'var(--space-1)',
  backgroundColor: 'var(--color-bg-muted)',
  borderRadius: 'var(--radius-md)',
  position: 'relative',
};

const tabLinkBase: React.CSSProperties = {
  padding: 'var(--space-1) var(--space-3)',
  fontSize: 'var(--font-size-sm)',
  fontWeight: 'var(--font-weight-medium)',
  borderRadius: 'var(--radius-sm)',
  textDecoration: 'none',
  position: 'relative',
  zIndex: 1,
  transition: 'color var(--transition-fast)',
};

const tabs = [
  { to: 'board', label: 'Board' },
  { to: 'timeline', label: 'Timeline' },
  { to: 'settings', label: 'Settings' },
];

interface IndicatorRect {
  left: number;
  top: number;
  width: number;
  height: number;
}

export function ProjectNav({ projectId }: { projectId: string | undefined }) {
  const location = useLocation();
  const tabRefs = useRef<(HTMLAnchorElement | null)[]>([]);
  const containerRef = useRef<HTMLDivElement>(null);
  const [indicator, setIndicator] = useState<IndicatorRect | null>(null);
  const initialized = useRef(false);

  const activeIndex = tabs.findIndex((tab) =>
    location.pathname.includes(`/${tab.to}`)
  );

  useLayoutEffect(() => {
    const el = tabRefs.current[activeIndex];
    const container = containerRef.current;
    if (!el || !container || activeIndex === -1) return;

    const cRect = container.getBoundingClientRect();
    const eRect = el.getBoundingClientRect();

    setIndicator({
      left: eRect.left - cRect.left,
      top: eRect.top - cRect.top,
      width: eRect.width,
      height: eRect.height,
    });
    initialized.current = true;
  }, [activeIndex]);

  if (!projectId) return null;

  const indicatorStyle: React.CSSProperties = indicator
    ? {
        position: 'absolute',
        backgroundColor: 'var(--color-accent)',
        borderRadius: 'var(--radius-sm)',
        pointerEvents: 'none',
        left: indicator.left,
        top: indicator.top,
        width: indicator.width,
        height: indicator.height,
        transition: prefersReducedMotion
          ? undefined
          : 'left 200ms ease, width 200ms ease, top 200ms ease, height 200ms ease',
      }
    : { display: 'none' };

  return (
    <nav style={wrapperStyle} aria-label="Project navigation">
      <Link to="/" style={backLinkStyle}>
        ← Dashboard
      </Link>
      <div ref={containerRef} style={tabsContainerStyle} role="tablist">
        <div style={indicatorStyle} aria-hidden="true" />
        {tabs.map((tab, i) => (
          <NavLink
            key={tab.to}
            to={`/projects/${projectId}/${tab.to}`}
            ref={(el) => {
              tabRefs.current[i] = el;
            }}
            style={({ isActive }) => ({
              ...tabLinkBase,
              color: isActive
                ? 'var(--color-text-on-accent)'
                : 'var(--color-text-muted)',
            })}
          >
            {tab.label}
          </NavLink>
        ))}
      </div>
    </nav>
  );
}
