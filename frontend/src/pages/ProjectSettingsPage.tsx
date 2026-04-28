import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { format } from 'date-fns';
import { useProject } from '../modules/project/hooks/useProject';
import { useUpdateProject } from '../modules/project/hooks/useUpdateProject';
import { useBoardQuery } from '../modules/kanban/hooks/useBoardQuery';
import { ProjectNav } from '../modules/project/components/ProjectNav';

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

const textareaStyle: React.CSSProperties = {
  ...inputStyle,
  resize: 'vertical',
  minHeight: '96px',
  fontFamily: 'inherit',
  lineHeight: '1.5',
};

const actionsRowStyle: React.CSSProperties = {
  display: 'flex',
  gap: 'var(--space-2)',
  justifyContent: 'flex-end',
  paddingTop: 'var(--space-2)',
};

const saveButtonStyle: React.CSSProperties = {
  padding: 'var(--space-2) var(--space-4)',
  fontSize: 'var(--font-size-sm)',
  fontWeight: 'var(--font-weight-medium)',
  color: '#fff',
  backgroundColor: 'var(--color-accent)',
  border: 'none',
  borderRadius: 'var(--radius-md)',
  cursor: 'pointer',
};

const saveButtonDisabledStyle: React.CSSProperties = {
  ...saveButtonStyle,
  opacity: 0.6,
  cursor: 'not-allowed',
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

  const updateProject = useUpdateProject(projectId!);

  const {
    data: board,
    isLoading: boardLoading,
    isError: boardError,
  } = useBoardQuery(projectId);

  const [name, setName] = useState('');
  const [description, setDescription] = useState('');

  useEffect(() => {
    if (project) {
      setName(project.name);
      setDescription(project.description ?? '');
    }
  }, [project]);

  const isDirty =
    project !== undefined &&
    (name.trim() !== project.name || (description.trim() || null) !== project.description);

  function handleSave() {
    if (!project || !isDirty) return;
    updateProject.mutate({
      name: name.trim(),
      description: description.trim() || null,
    });
  }

  return (
    <section style={pageStyle}>
      <ProjectNav projectId={projectId} />
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
              <label style={labelStyle} htmlFor="project-name">Name</label>
              <input
                id="project-name"
                style={inputStyle}
                value={name}
                onChange={(e) => setName(e.target.value)}
                maxLength={200}
                disabled={updateProject.isPending}
              />
            </div>

            <div style={rowStyle}>
              <span style={labelStyle}>Slug</span>
              <p style={monoValueStyle}>{project.slug}</p>
            </div>

            <div style={rowStyle}>
              <label style={labelStyle} htmlFor="project-description">Description</label>
              <textarea
                id="project-description"
                style={textareaStyle}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="No description"
                disabled={updateProject.isPending}
              />
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

            <div style={actionsRowStyle}>
              <button
                style={isDirty && !updateProject.isPending ? saveButtonStyle : saveButtonDisabledStyle}
                onClick={handleSave}
                disabled={!isDirty || updateProject.isPending}
              >
                {updateProject.isPending ? 'Saving…' : 'Save changes'}
              </button>
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
