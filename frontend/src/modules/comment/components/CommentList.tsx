import { formatDistanceToNow, parseISO } from 'date-fns';
import { useComments } from '../hooks/useComments';
import { getAvatarColors } from '../../../shared/utils/avatarColor';
import type React from 'react';

interface CommentListProps {
  taskId: string;
}

// ---------- styles ----------

const sectionLabelStyle: React.CSSProperties = {
  fontSize: 'var(--font-size-xs)',
  fontWeight: 'var(--font-weight-semibold)',
  color: 'var(--color-text-subtle)',
  textTransform: 'uppercase',
  letterSpacing: '0.05em',
  marginBottom: 'var(--space-2)',
};

const commentItemStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  gap: 'var(--space-1)',
  padding: 'var(--space-2) var(--space-3)',
  backgroundColor: 'var(--color-bg-muted)',
  borderRadius: 'var(--radius-md)',
  border: '1px solid var(--color-border)',
};

const commentMetaStyle: React.CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  gap: 'var(--space-2)',
};

const commentAuthorStyle: React.CSSProperties = {
  fontSize: 'var(--font-size-xs)',
  fontWeight: 'var(--font-weight-semibold)',
  color: 'var(--color-text)',
};

const commentTimeStyle: React.CSSProperties = {
  fontSize: 'var(--font-size-xs)',
  color: 'var(--color-text-subtle)',
};

const commentBodyStyle: React.CSSProperties = {
  fontSize: 'var(--font-size-sm)',
  color: 'var(--color-text)',
  margin: 0,
  whiteSpace: 'pre-wrap',
  lineHeight: 'var(--line-height-normal)',
};

const mutedTextStyle: React.CSSProperties = {
  fontSize: 'var(--font-size-sm)',
  color: 'var(--color-text-muted)',
  fontStyle: 'italic',
  margin: 0,
};

const errorTextStyle: React.CSSProperties = {
  fontSize: 'var(--font-size-sm)',
  color: 'var(--color-danger)',
  margin: 0,
};

const listStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  gap: 'var(--space-2)',
};

// ---------- component ----------

function fmtRelative(iso: string): string {
  try {
    return formatDistanceToNow(parseISO(iso), { addSuffix: true });
  } catch {
    return iso;
  }
}

export function CommentList({ taskId }: CommentListProps) {
  const { data: comments, isLoading, isError } = useComments(taskId);

  return (
    <div>
      <p style={sectionLabelStyle}>Comments</p>

      {isLoading && <p style={mutedTextStyle}>Loading comments…</p>}

      {isError && <p style={errorTextStyle}>Couldn't load comments.</p>}

      {!isLoading && !isError && comments && comments.length === 0 && (
        <p style={mutedTextStyle}>No comments yet.</p>
      )}

      {!isLoading && !isError && comments && comments.length > 0 && (
        <ul style={{ ...listStyle, listStyle: 'none', margin: 0, padding: 0 }}>
          {comments.map((c) => {
            const name = c.authorName ?? 'Deleted user';
            const { bg, fg } = c.authorId ? getAvatarColors(c.authorId) : { bg: 'var(--color-bg-muted)', fg: 'var(--color-text-subtle)' };
            const initials = name.trim().charAt(0).toUpperCase() || '?';
            return (
              <li key={c.id} style={commentItemStyle}>
                <div style={commentMetaStyle}>
                  <div
                    style={{
                      width: 24,
                      height: 24,
                      borderRadius: '50%',
                      flexShrink: 0,
                      backgroundColor: bg,
                      color: fg,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontSize: 'var(--font-size-xs)',
                      fontWeight: 'var(--font-weight-semibold)',
                      userSelect: 'none',
                    }}
                  >
                    {initials}
                  </div>
                  <span style={commentAuthorStyle}>{name}</span>
                  <span style={commentTimeStyle}>{fmtRelative(c.createdAt)}</span>
                </div>
                <p style={commentBodyStyle}>{c.body}</p>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
