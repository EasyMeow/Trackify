import { useState, useEffect } from 'react';
import { getAvatarColors } from '../utils/avatarColor';

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? '/api';

interface AvatarCircleProps {
  userId: string;
  displayName: string;
  /** Increment to bust the avatar image cache after an upload. */
  version?: number;
  size?: number;
}

export function AvatarCircle({ userId, displayName, version = 0, size = 32 }: AvatarCircleProps) {
  const [imgError, setImgError] = useState(false);
  useEffect(() => { setImgError(false); }, [version]);

  const { bg, fg } = getAvatarColors(userId);
  const letter = displayName.trim().charAt(0).toUpperCase() || '?';
  const avatarSrc = `${API_BASE}/users/${userId}/avatar?v=${version}`;

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
    backgroundColor: bg,
    color: fg,
    fontSize: size <= 28 ? 'var(--font-size-xs)' : size <= 40 ? 'var(--font-size-sm)' : 'var(--font-size-lg)',
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
