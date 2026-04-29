import { useState, useRef } from 'react';
import { useAuth } from '../modules/auth/hooks/useAuth';
import { useUpdateMe } from '../modules/auth/hooks/useUpdateMe';
import { useChangePassword } from '../modules/auth/hooks/useChangePassword';
import { useAvatarStore } from '../shared/state/avatarStore';
import { getAvatarColors } from '../shared/utils/avatarColor';

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? '/api';
const MIN_PASSWORD_LENGTH = 8;

// ─── styles ──────────────────────────────────────────────────────────────────

const pageStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  gap: 'var(--space-6)',
  padding: 'var(--space-6)',
  maxWidth: '600px',
};

const headingStyle: React.CSSProperties = {
  margin: 0,
  fontSize: 'var(--font-size-2xl)',
  fontWeight: 'var(--font-weight-semibold)',
  color: 'var(--color-text)',
};

const sectionStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  gap: 'var(--space-4)',
  padding: 'var(--space-5)',
  backgroundColor: 'var(--color-surface)',
  border: '1px solid var(--color-border)',
  borderRadius: 'var(--radius-lg)',
  boxShadow: 'var(--shadow-xs)',
};

const sectionHeadingStyle: React.CSSProperties = {
  margin: 0,
  fontSize: 'var(--font-size-md)',
  fontWeight: 'var(--font-weight-semibold)',
  color: 'var(--color-text)',
  paddingBottom: 'var(--space-2)',
  borderBottom: '1px solid var(--color-border)',
};

const rowStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  gap: 'var(--space-1)',
};

const labelStyle: React.CSSProperties = {
  fontSize: 'var(--font-size-xs)',
  color: 'var(--color-text-subtle)',
  textTransform: 'uppercase',
  letterSpacing: '0.05em',
};

const inputStyle: React.CSSProperties = {
  width: '100%',
  padding: 'var(--space-2) var(--space-3)',
  fontSize: 'var(--font-size-base)',
  color: 'var(--color-text)',
  backgroundColor: 'var(--color-bg)',
  border: '1px solid var(--color-border)',
  borderRadius: 'var(--radius-md)',
  outline: 'none',
  boxSizing: 'border-box',
};

const actionsRowStyle: React.CSSProperties = {
  display: 'flex',
  gap: 'var(--space-2)',
  justifyContent: 'flex-end',
  alignItems: 'center',
};

const primaryButtonStyle: React.CSSProperties = {
  padding: 'var(--space-2) var(--space-4)',
  fontSize: 'var(--font-size-sm)',
  fontWeight: 'var(--font-weight-medium)',
  color: '#fff',
  backgroundColor: 'var(--color-accent)',
  border: 'none',
  borderRadius: 'var(--radius-md)',
  cursor: 'pointer',
};

const primaryButtonDisabledStyle: React.CSSProperties = {
  ...primaryButtonStyle,
  opacity: 0.5,
  cursor: 'not-allowed',
};

const secondaryButtonStyle: React.CSSProperties = {
  padding: 'var(--space-2) var(--space-3)',
  fontSize: 'var(--font-size-sm)',
  color: 'var(--color-text-muted)',
  backgroundColor: 'transparent',
  border: '1px solid var(--color-border)',
  borderRadius: 'var(--radius-md)',
  cursor: 'pointer',
};

const errorTextStyle: React.CSSProperties = {
  fontSize: 'var(--font-size-sm)',
  color: 'var(--color-danger)',
  margin: 0,
};

const successTextStyle: React.CSSProperties = {
  fontSize: 'var(--font-size-sm)',
  color: 'var(--color-accent)',
  margin: 0,
};

// ─── avatar section ───────────────────────────────────────────────────────────

function AvatarSection({ userId, displayName }: { userId: string; displayName: string }) {
  const version = useAvatarStore((s) => s.version);
  const updateMe = useUpdateMe();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [preview, setPreview] = useState<string | null>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [imgError, setImgError] = useState(false);
  const [successMsg, setSuccessMsg] = useState('');

  const avatarSrc = `${API_BASE}/users/${userId}/avatar?v=${version}`;
  const letter = displayName.trim().charAt(0).toUpperCase() || '?';
  const { bg: avatarBg, fg: avatarFg } = getAvatarColors(userId);

  function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    setSelectedFile(file);
    setSuccessMsg('');
    const url = URL.createObjectURL(file);
    setPreview(url);
  }

  function handleCancel() {
    setSelectedFile(null);
    if (preview) URL.revokeObjectURL(preview);
    setPreview(null);
    if (fileInputRef.current) fileInputRef.current.value = '';
    setSuccessMsg('');
  }

  function handleUpload() {
    if (!selectedFile) return;
    const formData = new FormData();
    formData.append('avatar', selectedFile);
    updateMe.mutate(formData, {
      onSuccess: () => {
        setSelectedFile(null);
        if (preview) URL.revokeObjectURL(preview);
        setPreview(null);
        if (fileInputRef.current) fileInputRef.current.value = '';
        setImgError(false);
        setSuccessMsg('Avatar updated.');
      },
    });
  }

  const currentSrc = preview ?? (!imgError ? avatarSrc : null);

  return (
    <div style={sectionStyle}>
      <h2 style={sectionHeadingStyle}>Avatar</h2>

      <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-5)' }}>
        <div
          style={{
            width: 72,
            height: 72,
            borderRadius: '50%',
            overflow: 'hidden',
            flexShrink: 0,
            backgroundColor: avatarBg,
            color: avatarFg,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: 'var(--font-size-xl)',
            fontWeight: 'var(--font-weight-semibold)',
            border: '2px solid var(--color-border)',
          }}
        >
          {currentSrc ? (
            <img
              src={currentSrc}
              alt={displayName}
              width={72}
              height={72}
              style={{ width: 72, height: 72, objectFit: 'cover', display: 'block' }}
              onError={() => {
                if (!preview) setImgError(true);
              }}
            />
          ) : (
            letter
          )}
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-2)' }}>
          <input
            ref={fileInputRef}
            type="file"
            accept="image/jpeg,image/png,image/webp"
            style={{ display: 'none' }}
            onChange={handleFileChange}
            disabled={updateMe.isPending}
          />
          <button
            type="button"
            style={secondaryButtonStyle}
            onClick={() => fileInputRef.current?.click()}
            disabled={updateMe.isPending}
          >
            Choose image…
          </button>
          <p
            style={{
              margin: 0,
              fontSize: 'var(--font-size-xs)',
              color: 'var(--color-text-subtle)',
            }}
          >
            JPEG, PNG, or WebP · max 1 MB
          </p>
        </div>
      </div>

      {selectedFile && (
        <p style={{ margin: 0, fontSize: 'var(--font-size-sm)', color: 'var(--color-text-muted)' }}>
          Selected: {selectedFile.name}
        </p>
      )}

      {updateMe.isError && (
        <p style={errorTextStyle}>{updateMe.error?.message ?? 'Failed to upload avatar.'}</p>
      )}
      {successMsg && <p style={successTextStyle}>{successMsg}</p>}

      {selectedFile && (
        <div style={actionsRowStyle}>
          <button
            type="button"
            style={secondaryButtonStyle}
            onClick={handleCancel}
            disabled={updateMe.isPending}
          >
            Cancel
          </button>
          <button
            type="button"
            style={updateMe.isPending ? primaryButtonDisabledStyle : primaryButtonStyle}
            onClick={handleUpload}
            disabled={updateMe.isPending}
          >
            {updateMe.isPending ? 'Uploading…' : 'Upload avatar'}
          </button>
        </div>
      )}
    </div>
  );
}

// ─── display name section ─────────────────────────────────────────────────────

function DisplayNameSection({ currentDisplayName }: { currentDisplayName: string }) {
  const updateMe = useUpdateMe();
  const [displayName, setDisplayName] = useState(currentDisplayName);
  const [successMsg, setSuccessMsg] = useState('');

  const isDirty = displayName.trim() !== currentDisplayName;

  function handleSave() {
    if (!isDirty || !displayName.trim()) return;
    const formData = new FormData();
    formData.append('displayName', displayName.trim());
    updateMe.mutate(formData, {
      onSuccess: () => {
        setSuccessMsg('Display name updated.');
      },
    });
  }

  return (
    <div style={sectionStyle}>
      <h2 style={sectionHeadingStyle}>Display name</h2>

      <div style={rowStyle}>
        <label style={labelStyle} htmlFor="display-name">
          Name
        </label>
        <input
          id="display-name"
          style={inputStyle}
          value={displayName}
          onChange={(e) => {
            setDisplayName(e.target.value);
            setSuccessMsg('');
          }}
          maxLength={120}
          disabled={updateMe.isPending}
        />
      </div>

      {updateMe.isError && (
        <p style={errorTextStyle}>{updateMe.error?.message ?? 'Failed to update display name.'}</p>
      )}
      {successMsg && <p style={successTextStyle}>{successMsg}</p>}

      <div style={actionsRowStyle}>
        <button
          type="button"
          style={isDirty && displayName.trim() && !updateMe.isPending ? primaryButtonStyle : primaryButtonDisabledStyle}
          onClick={handleSave}
          disabled={!isDirty || !displayName.trim() || updateMe.isPending}
        >
          {updateMe.isPending ? 'Saving…' : 'Save changes'}
        </button>
      </div>
    </div>
  );
}

// ─── password section ─────────────────────────────────────────────────────────

function PasswordSection() {
  const changePassword = useChangePassword();
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  const mismatch = newPassword.length > 0 && confirmPassword.length > 0 && newPassword !== confirmPassword;
  const tooShort = newPassword.length > 0 && newPassword.length < MIN_PASSWORD_LENGTH;
  const canSubmit =
    currentPassword.length > 0 &&
    newPassword.length >= MIN_PASSWORD_LENGTH &&
    newPassword === confirmPassword &&
    !changePassword.isPending;

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!canSubmit) return;
    changePassword.mutate(
      { currentPassword, newPassword },
      {
        onSuccess: () => {
          setCurrentPassword('');
          setNewPassword('');
          setConfirmPassword('');
          setSuccessMsg('Password changed successfully.');
        },
      },
    );
  }

  return (
    <div style={sectionStyle}>
      <h2 style={sectionHeadingStyle}>Change password</h2>

      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
        <div style={rowStyle}>
          <label style={labelStyle} htmlFor="current-password">
            Current password
          </label>
          <input
            id="current-password"
            type="password"
            style={inputStyle}
            value={currentPassword}
            onChange={(e) => { setCurrentPassword(e.target.value); setSuccessMsg(''); }}
            autoComplete="current-password"
            disabled={changePassword.isPending}
          />
        </div>

        <div style={rowStyle}>
          <label style={labelStyle} htmlFor="new-password">
            New password
          </label>
          <input
            id="new-password"
            type="password"
            style={inputStyle}
            value={newPassword}
            onChange={(e) => { setNewPassword(e.target.value); setSuccessMsg(''); }}
            autoComplete="new-password"
            disabled={changePassword.isPending}
          />
          {tooShort && (
            <p style={errorTextStyle}>
              Password must be at least {MIN_PASSWORD_LENGTH} characters.
            </p>
          )}
        </div>

        <div style={rowStyle}>
          <label style={labelStyle} htmlFor="confirm-password">
            Confirm new password
          </label>
          <input
            id="confirm-password"
            type="password"
            style={inputStyle}
            value={confirmPassword}
            onChange={(e) => { setConfirmPassword(e.target.value); setSuccessMsg(''); }}
            autoComplete="new-password"
            disabled={changePassword.isPending}
          />
          {mismatch && <p style={errorTextStyle}>Passwords do not match.</p>}
        </div>

        {changePassword.isError && (
          <p style={errorTextStyle}>
            {changePassword.error?.message ?? 'Failed to change password.'}
          </p>
        )}
        {successMsg && <p style={successTextStyle}>{successMsg}</p>}

        <div style={actionsRowStyle}>
          <button
            type="submit"
            style={canSubmit ? primaryButtonStyle : primaryButtonDisabledStyle}
            disabled={!canSubmit}
          >
            {changePassword.isPending ? 'Changing…' : 'Change password'}
          </button>
        </div>
      </form>
    </div>
  );
}

// ─── account info section ─────────────────────────────────────────────────────

function AccountInfoSection({ login, email }: { login: string; email: string }) {
  const readonlyInputStyle: React.CSSProperties = {
    ...inputStyle,
    backgroundColor: 'var(--color-bg)',
    color: 'var(--color-text-muted)',
    cursor: 'default',
  };

  return (
    <div style={sectionStyle}>
      <h2 style={sectionHeadingStyle}>Account</h2>

      <div style={rowStyle}>
        <span style={labelStyle}>Login</span>
        <input
          style={readonlyInputStyle}
          value={login}
          readOnly
          tabIndex={-1}
        />
      </div>

      <div style={rowStyle}>
        <span style={labelStyle}>Email</span>
        <input
          style={readonlyInputStyle}
          value={email}
          readOnly
          tabIndex={-1}
        />
      </div>
    </div>
  );
}

// ─── page ─────────────────────────────────────────────────────────────────────

export default function ProfilePage() {
  const { user } = useAuth();

  if (!user) return null;

  return (
    <section style={pageStyle}>
      <h1 style={headingStyle}>Profile</h1>
      <AvatarSection userId={user.id} displayName={user.displayName} />
      <DisplayNameSection currentDisplayName={user.displayName} />
      <AccountInfoSection login={user.login} email={user.email} />
      <PasswordSection />
    </section>
  );
}
