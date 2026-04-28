import { useState } from 'react';
import type { FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useRegister } from '../modules/auth/hooks/useRegister';
import { ApiError } from '../shared/api/httpClient';

const MIN_PASSWORD_LENGTH = 8;

export default function SignUpPage() {
  const navigate = useNavigate();
  const register = useRegister();

  const [login, setLogin] = useState('');
  const [email, setEmail] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [password, setPassword] = useState('');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const trimmedLogin = login.trim();
  const trimmedEmail = email.trim();
  const trimmedDisplayName = displayName.trim();
  const formValid =
    trimmedLogin.length > 0 &&
    trimmedEmail.length > 0 &&
    trimmedDisplayName.length > 0 &&
    password.length >= MIN_PASSWORD_LENGTH;
  const submitting = register.isPending;
  const canSubmit = formValid && !submitting;

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!canSubmit) return;

    setErrorMessage(null);
    try {
      await register.mutateAsync({
        login: trimmedLogin,
        email: trimmedEmail,
        displayName: trimmedDisplayName,
        password,
      });
      navigate('/', { replace: true });
    } catch (error) {
      if (error instanceof ApiError) {
        setErrorMessage(error.message || 'Sign-up failed. Please try again.');
      } else {
        setErrorMessage('Sign-up failed. Please try again.');
      }
    }
  }

  return (
    <form
      onSubmit={handleSubmit}
      noValidate
      style={{
        display: 'flex',
        flexDirection: 'column',
        gap: 'var(--space-4)',
        backgroundColor: 'var(--color-surface)',
        border: '1px solid var(--color-border)',
        borderRadius: 'var(--radius-lg)',
        padding: 'var(--space-6)',
        boxShadow: 'var(--shadow-sm)',
      }}
    >
      <header style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-1)' }}>
        <h1 style={{ fontSize: 'var(--font-size-xl)' }}>Create your account</h1>
        <p style={{ color: 'var(--color-text-muted)', fontSize: 'var(--font-size-sm)' }}>
          Pick a login and password to start using Trackify.
        </p>
      </header>

      <label
        htmlFor="signup-login"
        style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-1)' }}
      >
        <span style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-text-muted)' }}>
          Login
        </span>
        <input
          id="signup-login"
          name="login"
          type="text"
          autoComplete="username"
          autoFocus
          required
          value={login}
          onChange={(event) => setLogin(event.target.value)}
          disabled={submitting}
          style={{
            padding: 'var(--space-3)',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-md)',
            backgroundColor: 'var(--color-bg)',
            fontSize: 'var(--font-size-base)',
          }}
        />
      </label>

      <label
        htmlFor="signup-email"
        style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-1)' }}
      >
        <span style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-text-muted)' }}>
          Email
        </span>
        <input
          id="signup-email"
          name="email"
          type="email"
          autoComplete="email"
          required
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          disabled={submitting}
          style={{
            padding: 'var(--space-3)',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-md)',
            backgroundColor: 'var(--color-bg)',
            fontSize: 'var(--font-size-base)',
          }}
        />
      </label>

      <label
        htmlFor="signup-display-name"
        style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-1)' }}
      >
        <span style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-text-muted)' }}>
          Display name
        </span>
        <input
          id="signup-display-name"
          name="displayName"
          type="text"
          autoComplete="name"
          required
          value={displayName}
          onChange={(event) => setDisplayName(event.target.value)}
          disabled={submitting}
          style={{
            padding: 'var(--space-3)',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-md)',
            backgroundColor: 'var(--color-bg)',
            fontSize: 'var(--font-size-base)',
          }}
        />
      </label>

      <label
        htmlFor="signup-password"
        style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-1)' }}
      >
        <span style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-text-muted)' }}>
          Password
        </span>
        <input
          id="signup-password"
          name="password"
          type="password"
          autoComplete="new-password"
          required
          minLength={MIN_PASSWORD_LENGTH}
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          disabled={submitting}
          style={{
            padding: 'var(--space-3)',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-md)',
            backgroundColor: 'var(--color-bg)',
            fontSize: 'var(--font-size-base)',
          }}
        />
        <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-subtle)' }}>
          At least {MIN_PASSWORD_LENGTH} characters.
        </span>
      </label>

      {errorMessage && (
        <div
          role="alert"
          style={{
            backgroundColor: 'var(--color-danger-soft)',
            color: 'var(--color-danger)',
            border: '1px solid var(--color-danger)',
            borderRadius: 'var(--radius-md)',
            padding: 'var(--space-3)',
            fontSize: 'var(--font-size-sm)',
          }}
        >
          {errorMessage}
        </div>
      )}

      <button
        type="submit"
        disabled={!canSubmit}
        style={{
          padding: 'var(--space-3) var(--space-4)',
          backgroundColor: 'var(--color-accent)',
          color: 'var(--color-text-on-accent)',
          borderRadius: 'var(--radius-md)',
          fontSize: 'var(--font-size-base)',
          fontWeight: 'var(--font-weight-medium)',
          transition: 'background-color var(--transition-fast)',
        }}
      >
        {submitting ? 'Creating account…' : 'Create account'}
      </button>
    </form>
  );
}
