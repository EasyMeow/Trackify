import { useState, useRef, useEffect } from 'react';
import { createPortal } from 'react-dom';
import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../modules/auth/hooks/useAuth';
import { WorkspaceSwitcher } from '../../modules/workspace/components/WorkspaceSwitcher';
import { ToastViewport } from '../../shared/components/ToastViewport';
import { addErrorToast } from '../../shared/state/toastStore';
import { useAvatarStore } from '../../shared/state/avatarStore';

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? '/api';

const SIDEBAR_COLLAPSED_WIDTH = 52;
const SIDEBAR_EXPANDED_WIDTH = 240;

function AvatarCircle({
  userId,
  displayName,
  version,
  size = 32,
}: {
  userId: string;
  displayName: string;
  version: number;
  size?: number;
}) {
  const [imgError, setImgError] = useState(false);
  useEffect(() => { setImgError(false); }, [version]);
  const avatarSrc = `${API_BASE}/users/${userId}/avatar?v=${version}`;
  const letter = displayName.trim().charAt(0).toUpperCase() || '?';

  const circleStyle: React.CSSProperties = {
    width: size,
    height: size,
    borderRadius: '50%',
    flexShrink: 0,
    userSelect: 'none',
    overflow: 'hidden',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'var(--color-accent-soft)',
    color: 'var(--color-accent)',
    fontSize: size <= 32 ? 'var(--font-size-sm)' : 'var(--font-size-lg)',
    fontWeight: 'var(--font-weight-semibold)',
  };

  if (!imgError) {
    return (
      <div style={circleStyle}>
        <img
          src={avatarSrc}
          alt={displayName}
          width={size}
          height={size}
          style={{ width: size, height: size, objectFit: 'cover', display: 'block' }}
          onError={() => setImgError(true)}
        />
      </div>
    );
  }

  return <div style={circleStyle}>{letter}</div>;
}

function AvatarMenu({ collapsed }: { collapsed: boolean }) {
  const { user, signOut } = useAuth();
  const version = useAvatarStore((s) => s.version);
  const [open, setOpen] = useState(false);
  const [pending, setPending] = useState(false);
  const buttonRef = useRef<HTMLButtonElement>(null);
  const menuRef = useRef<HTMLDivElement>(null);
  const [menuStyle, setMenuStyle] = useState<React.CSSProperties>({});

  useEffect(() => {
    if (!open) return;
    function handleClickOutside(e: MouseEvent) {
      const target = e.target as Node;
      const insideMenu = menuRef.current?.contains(target) ?? false;
      const insideButton = buttonRef.current?.contains(target) ?? false;
      if (!insideMenu && !insideButton) {
        setOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [open]);

  function toggleMenu() {
    if (open) {
      setOpen(false);
      return;
    }
    if (buttonRef.current) {
      const rect = buttonRef.current.getBoundingClientRect();
      // Position menu above button; in collapsed mode open to the right
      if (collapsed) {
        setMenuStyle({
          position: 'fixed',
          top: rect.top,
          left: rect.right + 6,
          zIndex: 70,
        });
      } else {
        setMenuStyle({
          position: 'fixed',
          bottom: window.innerHeight - rect.top + 4,
          left: rect.left,
          zIndex: 70,
        });
      }
    }
    setOpen(true);
  }

  async function handleSignOut() {
    if (pending) return;
    setPending(true);
    setOpen(false);
    try {
      await signOut();
    } catch (error) {
      const message =
        error instanceof Error && error.message ? error.message : 'Failed to sign out.';
      addErrorToast(message);
      setPending(false);
    }
  }

  if (!user) return null;

  const menuItemStyle: React.CSSProperties = {
    display: 'block',
    width: '100%',
    textAlign: 'left',
    padding: 'var(--space-2) var(--space-3)',
    fontSize: 'var(--font-size-sm)',
    color: 'var(--color-text)',
    backgroundColor: 'transparent',
    border: 'none',
    borderRadius: 'var(--radius-md)',
    cursor: 'pointer',
    textDecoration: 'none',
  };

  return (
    <div style={{ marginTop: 'auto' }} title={collapsed ? user.displayName : undefined}>
      <button
        ref={buttonRef}
        type="button"
        onClick={toggleMenu}
        aria-label="User menu"
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 'var(--space-2)',
          width: '100%',
          padding: collapsed ? 'var(--space-1) 0' : 'var(--space-2) var(--space-1)',
          backgroundColor: open ? 'var(--color-surface-hover)' : 'transparent',
          border: 'none',
          borderRadius: 'var(--radius-md)',
          cursor: 'pointer',
          justifyContent: collapsed ? 'center' : 'flex-start',
          transition: 'background-color var(--transition-fast)',
        }}
      >
        <AvatarCircle userId={user.id} displayName={user.displayName} version={version} />
        {!collapsed && (
          <div
            style={{
              display: 'flex',
              flexDirection: 'column',
              gap: '1px',
              minWidth: 0,
              overflow: 'hidden',
            }}
          >
            <span
              style={{
                fontSize: 'var(--font-size-sm)',
                fontWeight: 'var(--font-weight-medium)',
                color: 'var(--color-text)',
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
              }}
            >
              {user.displayName}
            </span>
            <span
              style={{
                fontSize: 'var(--font-size-xs)',
                color: 'var(--color-text-subtle)',
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
              }}
            >
              {user.login}
            </span>
          </div>
        )}
      </button>

      {open &&
        createPortal(
          <div
            ref={menuRef}
            style={{
              ...menuStyle,
              backgroundColor: 'var(--color-surface)',
              border: '1px solid var(--color-border)',
              borderRadius: 'var(--radius-lg)',
              boxShadow: 'var(--shadow-lg)',
              minWidth: 160,
              overflow: 'hidden',
              padding: 'var(--space-1)',
              display: 'flex',
              flexDirection: 'column',
              gap: '2px',
            }}
          >
            <Link
              to="/profile"
              onClick={() => setOpen(false)}
              style={menuItemStyle}
              onMouseEnter={(e) => {
                (e.currentTarget as HTMLElement).style.backgroundColor =
                  'var(--color-surface-hover)';
              }}
              onMouseLeave={(e) => {
                (e.currentTarget as HTMLElement).style.backgroundColor = 'transparent';
              }}
            >
              Profile
            </Link>
            <button
              type="button"
              disabled={pending}
              onClick={handleSignOut}
              style={{
                ...menuItemStyle,
                opacity: pending ? 0.6 : 1,
                cursor: pending ? 'wait' : 'pointer',
              }}
              onMouseEnter={(e) => {
                if (!pending)
                  (e.currentTarget as HTMLElement).style.backgroundColor =
                    'var(--color-surface-hover)';
              }}
              onMouseLeave={(e) => {
                (e.currentTarget as HTMLElement).style.backgroundColor = 'transparent';
              }}
            >
              {pending ? 'Signing out…' : 'Sign out'}
            </button>
          </div>,
          document.body
        )}
    </div>
  );
}

export function AppShell({ children }: { children: ReactNode }) {
  const [collapsed, setCollapsed] = useState(false);

  return (
    <div
      style={{
        display: 'flex',
        minHeight: '100vh',
        backgroundColor: 'var(--color-bg)',
      }}
    >
      <aside
        style={{
          width: collapsed ? SIDEBAR_COLLAPSED_WIDTH : SIDEBAR_EXPANDED_WIDTH,
          flexShrink: 0,
          backgroundColor: 'var(--color-surface)',
          borderRight: '1px solid var(--color-border)',
          display: 'flex',
          flexDirection: 'column',
          gap: 'var(--space-4)',
          padding: collapsed ? `var(--space-4) var(--space-2)` : 'var(--space-4)',
          transition: `width var(--transition-default), padding var(--transition-default)`,
          overflow: 'hidden',
        }}
      >
        {/* Header: logo (expanded) + collapse toggle */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: collapsed ? 'center' : 'space-between',
            minHeight: 28,
          }}
        >
          {!collapsed && (
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 'var(--space-2)',
                minWidth: 0,
                overflow: 'hidden',
              }}
            >
              <img
                src="/favicon.svg"
                alt=""
                aria-hidden="true"
                width={22}
                height={22}
                style={{ display: 'block', flexShrink: 0 }}
              />
              <span
                style={{
                  fontSize: 'var(--font-size-lg)',
                  fontWeight: 'var(--font-weight-semibold)',
                  color: 'var(--color-accent)',
                  letterSpacing: '-0.3px',
                  whiteSpace: 'nowrap',
                  overflow: 'hidden',
                }}
              >
                Trackify
              </span>
            </div>
          )}
          <button
            type="button"
            onClick={() => setCollapsed((c) => !c)}
            title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
            aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              width: 28,
              height: 28,
              padding: 0,
              backgroundColor: 'transparent',
              border: '1px solid var(--color-border)',
              borderRadius: 'var(--radius-md)',
              cursor: 'pointer',
              color: 'var(--color-text-subtle)',
              fontSize: '13px',
              flexShrink: 0,
              transition: 'background-color var(--transition-fast)',
            }}
            onMouseEnter={(e) => {
              (e.currentTarget as HTMLElement).style.backgroundColor =
                'var(--color-surface-hover)';
            }}
            onMouseLeave={(e) => {
              (e.currentTarget as HTMLElement).style.backgroundColor = 'transparent';
            }}
          >
            {collapsed ? '›' : '‹'}
          </button>
        </div>

        <WorkspaceSwitcher collapsed={collapsed} />
        <AvatarMenu collapsed={collapsed} />
      </aside>

      <main
        style={{
          flex: 1,
          minWidth: 0,
          backgroundColor: 'var(--color-bg)',
        }}
      >
        {children}
      </main>
      <ToastViewport />
    </div>
  );
}
