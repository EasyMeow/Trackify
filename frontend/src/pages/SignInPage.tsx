import { useState } from 'react';
import type { FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../modules/auth/hooks/useAuth';
import { ApiError } from '../shared/api/httpClient';

export default function SignInPage() {
  const navigate = useNavigate();
  const { signIn } = useAuth();

  const [login, setLogin] = useState('');
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting) return;

    setErrorMessage(null);
    setSubmitting(true);
    try {
      await signIn({ login: login.trim(), password });
      navigate('/', { replace: true });
    } catch (error) {
      if (error instanceof ApiError && error.code === 'INVALID_CREDENTIALS') {
        setErrorMessage('Invalid login or password.');
      } else if (error instanceof ApiError) {
        setErrorMessage(error.message || 'Sign-in failed. Please try again.');
      } else {
        setErrorMessage('Sign-in failed. Please try again.');
      }
      setSubmitting(false);
    }
  }

  const canSubmit = login.trim().length > 0 && password.length > 0 && !submitting;

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
        <h1 style={{ fontSize: 'var(--font-size-xl)' }}>Sign in</h1>
        <p style={{ color: 'var(--color-text-muted)', fontSize: 'var(--font-size-sm)' }}>
          Use your Trackify login and password.
        </p>
      </header>

      <label
        htmlFor="signin-login"
        style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-1)' }}
      >
        <span style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-text-muted)' }}>
          Login
        </span>
        <input
          id="signin-login"
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
        htmlFor="signin-password"
        style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-1)' }}
      >
        <span style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-text-muted)' }}>
          Password
        </span>
        <input
          id="signin-password"
          name="password"
          type="password"
          autoComplete="current-password"
          required
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
        {submitting ? 'Signing in…' : 'Sign in'}
      </button>
    </form>
  );
}
