import { useState } from 'react';
import { useCreateComment } from '../hooks/useCreateComment';

interface CommentComposerProps {
  taskId: string;
}

// ---------- styles ----------

const composerStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  gap: 'var(--space-2)',
};

const textareaStyle: React.CSSProperties = {
  width: '100%',
  boxSizing: 'border-box',
  padding: 'var(--space-2)',
  fontSize: 'var(--font-size-sm)',
  color: 'var(--color-text)',
  backgroundColor: 'var(--color-bg)',
  border: '1px solid var(--color-border)',
  borderRadius: 'var(--radius-md)',
  outline: 'none',
  fontFamily: 'inherit',
  resize: 'vertical',
  minHeight: '80px',
  lineHeight: 'var(--line-height-normal)',
};

const rowStyle: React.CSSProperties = {
  display: 'flex',
  justifyContent: 'flex-end',
};

const submitBtnStyle: React.CSSProperties = {
  padding: 'var(--space-1) var(--space-3)',
  fontSize: 'var(--font-size-sm)',
  fontWeight: 'var(--font-weight-medium)',
  color: 'var(--color-text-on-accent)',
  backgroundColor: 'var(--color-success)',
  border: 'none',
  borderRadius: 'var(--radius-md)',
  cursor: 'pointer',
  transition: 'background-color var(--transition-fast)',
};

const submitBtnDisabledStyle: React.CSSProperties = {
  ...submitBtnStyle,
  backgroundColor: 'var(--color-border-strong)',
  color: 'var(--color-text-subtle)',
  cursor: 'not-allowed',
};

const inlineErrorStyle: React.CSSProperties = {
  fontSize: 'var(--font-size-xs)',
  color: 'var(--color-danger)',
  marginTop: 'var(--space-1)',
};

// ---------- component ----------

export function CommentComposer({ taskId }: CommentComposerProps) {
  const [draft, setDraft] = useState('');
  const [error, setError] = useState<string | null>(null);

  const { mutate, isPending } = useCreateComment(taskId);

  const trimmed = draft.trim();
  const isDisabled = trimmed.length === 0 || isPending;

  function handleSubmit() {
    if (!trimmed) return;
    setError(null);
    mutate(
      { body: trimmed },
      {
        onSuccess: () => {
          setDraft('');
        },
        onError: () => {
          setError("Couldn't post comment.");
        },
      },
    );
  }

  function handleKeyDown(e: React.KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === 'Enter' && (e.metaKey || e.ctrlKey)) {
      e.preventDefault();
      if (!isDisabled) handleSubmit();
    }
  }

  return (
    <div style={composerStyle}>
      <textarea
        style={textareaStyle}
        placeholder="Add a comment…"
        value={draft}
        disabled={isPending}
        onChange={(e) => {
          setDraft(e.target.value);
          if (error) setError(null);
        }}
        onKeyDown={handleKeyDown}
        aria-label="Comment body"
      />
      {error && <p style={inlineErrorStyle}>{error}</p>}
      <div style={rowStyle}>
        <button
          style={isDisabled ? submitBtnDisabledStyle : submitBtnStyle}
          disabled={isDisabled}
          onClick={handleSubmit}
        >
          Add comment
        </button>
      </div>
    </div>
  );
}
