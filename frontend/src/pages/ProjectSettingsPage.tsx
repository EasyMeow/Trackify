import { useParams } from 'react-router-dom';
import { format } from 'date-fns';
import { useProject } from '../modules/project/hooks/useProject';
import { useBoardQuery } from '../modules/kanban/hooks/useBoardQuery';

// ─── styles ──────────────────────────────────────────────────────────────────

const pageStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  gap: 'var(--space-6)',
  padding: 'var(--space-6)',
  maxWidth: '720px',
};

const sectionStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  gap: 'var(--space-3)',
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

const valueStyle: React.CSSProperties = {
  fontSize: 'var(--font-size-base)',
  color: 'var(--color-text)',
  margin: 0,
};

const monoValueStyle: React.CSSProperties = {
  ...valueStyle,
  fontFamily: 'var(--font-mono)',
  fontSize: 'var(--font-size-sm)',
  color: 'var(--color-text-muted)',
};

const mutedValueStyle: React.CSSProperties = {
  ...valueStyle,
  color: 'var(--color-text-subtle)',
  fontStyle: 'italic',
};

const mutedTextStyle: React.CSSProperties = {
  color: 'var(--color-text-muted)',
  fontSize: 'var(--font-size-sm)',
  margin: 0,
};

const errorTextStyle: React.CSSProperties = {
  fontSize: 'var(--font-size-sm)',
  color: 'var(--color-danger)',
  margin: 0,
};

const columnTableStyle: React.CSSProperties = {
  width: '100%',
  borderCollapse: 'collapse',
};

const thStyle: React.CSSProperties = {
  textAlign: 'left',
  fontSize: 'var(--font-size-xs)',
  color: 'var(--color-text-subtle)',
  textTransform: 'uppercase',
  letterSpacing: '0.05em',
  padding: 'var(--space-1) var(--space-2)',
  borderBottom: '1px solid var(--color-border)',
};

const tdStyle: React.CSSProperties = {
  fontSize: 'var(--font-size-sm)',
  color: 'var(--color-text)',
  padding: 'var(--space-2)',
  borderBottom: '1px solid var(--color-border)',
};

const tdMutedStyle: React.CSSProperties = {
  ...tdStyle,
  color: 'var(--color-text-subtle)',
};

// ─── component ───────────────────────────────────────────────────────────────

export default function ProjectSettingsPage() {
  const { projectId } = useParams<{ projectId: string }>();

  const {
    data: project,
    isLoading: projectLoading,
    isError: projectError,
  } = useProject(projectId);

  const {
    data: board,
    isLoading: boardLoading,
    isError: boardError,
  } = useBoardQuery(projectId);

  return (
    <section style={pageStyle}>
      <h1 style={{ margin: 0, fontSize: 'var(--font-size-2xl)', fontWeight: 'var(--font-weight-semibold)', color: 'var(--color-text)' }}>
        Project settings
      </h1>

      {/* ── metadata section ── */}
      <div style={sectionStyle}>
        <h2 style={sectionHeadingStyle}>Project details</h2>

        {projectLoading && <p style={mutedTextStyle}>Loading project…</p>}
        {!projectLoading && projectError && (
          <p style={errorTextStyle}>Couldn't load project details.</p>
        )}
        {!projectLoading && !projectError && project && (
          <>
            <div style={rowStyle}>
              <span style={labelStyle}>Name</span>
              <p style={valueStyle}>{project.name}</p>
            </div>
            <div style={rowStyle}>
              <span style={labelStyle}>Slug</span>
              <p style={monoValueStyle}>{project.slug}</p>
            </div>
            <div style={rowStyle}>
              <span style={labelStyle}>Description</span>
              {project.description ? (
                <p style={valueStyle}>{project.description}</p>
              ) : (
                <p style={mutedValueStyle}>No description</p>
              )}
            </div>
            <div style={{ display: 'flex', gap: 'var(--space-6)', flexWrap: 'wrap' }}>
              <div style={rowStyle}>
                <span style={labelStyle}>Created</span>
                <p style={valueStyle}>
                  {format(new Date(project.createdAt), 'PPP')}
                </p>
              </div>
              <div style={rowStyle}>
                <span style={labelStyle}>Last updated</span>
                <p style={valueStyle}>
                  {format(new Date(project.updatedAt), 'PPP')}
                </p>
              </div>
            </div>
          </>
        )}
      </div>

      {/* ── board columns section ── */}
      <div style={sectionStyle}>
        <h2 style={sectionHeadingStyle}>Board columns</h2>

        {boardLoading && <p style={mutedTextStyle}>Loading columns…</p>}
        {!boardLoading && boardError && (
          <p style={errorTextStyle}>Couldn't load board columns.</p>
        )}
        {!boardLoading && !boardError && board && board.columns.length === 0 && (
          <p style={mutedTextStyle}>No columns configured for this board.</p>
        )}
        {!boardLoading && !boardError && board && board.columns.length > 0 && (
          <table style={columnTableStyle}>
            <thead>
              <tr>
                <th style={thStyle}>Position</th>
                <th style={thStyle}>Name</th>
              </tr>
            </thead>
            <tbody>
              {[...board.columns]
                .sort((a, b) => a.position - b.position)
                .map((col) => (
                  <tr key={col.id}>
                    <td style={tdMutedStyle}>{col.position + 1}</td>
                    <td style={tdStyle}>{col.name}</td>
                  </tr>
                ))}
            </tbody>
          </table>
        )}
      </div>
    </section>
  );
}
