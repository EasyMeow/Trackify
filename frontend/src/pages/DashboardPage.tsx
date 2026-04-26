import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useWorkspaces } from '../modules/workspace/hooks/useWorkspaces';
import { useProjects } from '../modules/project/hooks/useProjects';
import { useCreateProject } from '../modules/project/hooks/useCreateProject';
import { ApiError } from '../shared/api/httpClient';
import type { Project } from '../modules/project/types/project';

const pageStyle = {
  display: 'flex',
  flexDirection: 'column' as const,
  gap: 'var(--space-4)',
  padding: 'var(--space-6)',
};

const mutedTextStyle = {
  color: 'var(--color-text-muted)',
  margin: 0,
};

const gridStyle = {
  display: 'grid',
  gap: 'var(--space-3)',
  gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))',
};

const cardStyle = {
  display: 'flex',
  flexDirection: 'column' as const,
  gap: 'var(--space-2)',
  padding: 'var(--space-4)',
  backgroundColor: 'var(--color-surface)',
  border: '1px solid var(--color-border)',
  borderRadius: 'var(--radius-lg)',
  boxShadow: 'var(--shadow-xs)',
};

const cardTitleStyle = {
  fontSize: 'var(--font-size-md)',
  fontWeight: 'var(--font-weight-semibold)',
  color: 'var(--color-text)',
  margin: 0,
};

const cardSlugStyle = {
  fontSize: 'var(--font-size-xs)',
  color: 'var(--color-text-subtle)',
  margin: 0,
};

const cardDescriptionStyle = {
  fontSize: 'var(--font-size-sm)',
  color: 'var(--color-text-muted)',
  margin: 0,
};

const formCardStyle = {
  display: 'flex',
  flexDirection: 'column' as const,
  gap: 'var(--space-3)',
  padding: 'var(--space-4)',
  backgroundColor: 'var(--color-surface)',
  border: '1px solid var(--color-border)',
  borderRadius: 'var(--radius-lg)',
  boxShadow: 'var(--shadow-xs)',
  maxWidth: '480px',
};

const formTitleStyle = {
  fontSize: 'var(--font-size-md)',
  fontWeight: 'var(--font-weight-semibold)',
  color: 'var(--color-text)',
  margin: 0,
};

const labelStyle = {
  display: 'flex',
  flexDirection: 'column' as const,
  gap: 'var(--space-1)',
  fontSize: 'var(--font-size-sm)',
  color: 'var(--color-text-muted)',
};

const inputStyle = {
  padding: 'var(--space-2) var(--space-3)',
  fontSize: 'var(--font-size-sm)',
  color: 'var(--color-text)',
  backgroundColor: 'var(--color-bg)',
  border: '1px solid var(--color-border)',
  borderRadius: 'var(--radius-md)',
  outline: 'none',
};

const submitButtonStyle = {
  alignSelf: 'flex-start' as const,
  padding: 'var(--space-2) var(--space-4)',
  fontSize: 'var(--font-size-sm)',
  fontWeight: 'var(--font-weight-semibold)',
  color: 'var(--color-text-on-accent)',
  backgroundColor: 'var(--color-accent)',
  border: 'none',
  borderRadius: 'var(--radius-md)',
  cursor: 'pointer',
};

const submitButtonDisabledStyle = {
  ...submitButtonStyle,
  opacity: 0.5,
  cursor: 'not-allowed' as const,
};

const errorTextStyle = {
  fontSize: 'var(--font-size-sm)',
  color: 'var(--color-danger)',
  margin: 0,
};

export default function DashboardPage() {
  const [searchParams] = useSearchParams();
  const { data: workspaces, isLoading: workspacesLoading } = useWorkspaces();

  const urlWorkspaceId = searchParams.get('workspace');
  const selectedWorkspaceId = workspaces?.some((workspace) => workspace.id === urlWorkspaceId)
    ? (urlWorkspaceId as string)
    : workspaces?.[0]?.id;

  const {
    data: projects,
    isLoading: projectsLoading,
    isError: projectsError,
  } = useProjects(selectedWorkspaceId);

  return (
    <section style={pageStyle}>
      <h1>Dashboard</h1>
      {workspacesLoading && <p style={mutedTextStyle}>Loading workspaces…</p>}
      {!workspacesLoading && !selectedWorkspaceId && (
        <p style={mutedTextStyle}>No workspace selected.</p>
      )}
      {selectedWorkspaceId && (
        <CreateProjectForm workspaceId={selectedWorkspaceId} />
      )}
      {selectedWorkspaceId && projectsLoading && (
        <p style={mutedTextStyle}>Loading projects…</p>
      )}
      {selectedWorkspaceId && projectsError && (
        <p style={mutedTextStyle}>Could not load projects.</p>
      )}
      {selectedWorkspaceId && projects && projects.length === 0 && (
        <p style={mutedTextStyle}>No projects yet.</p>
      )}
      {selectedWorkspaceId && projects && projects.length > 0 && (
        <ul style={{ ...gridStyle, listStyle: 'none', margin: 0, padding: 0 }}>
          {projects.map((project) => (
            <ProjectCard key={project.id} project={project} />
          ))}
        </ul>
      )}
    </section>
  );
}

function CreateProjectForm({ workspaceId }: { workspaceId: string }) {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const { mutate, isPending, error, reset } = useCreateProject(workspaceId);

  const errorMessage =
    error instanceof ApiError ? error.message : error ? String(error) : null;

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!name.trim()) return;
    mutate(
      { name: name.trim(), description: description.trim() || undefined },
      {
        onSuccess: () => {
          setName('');
          setDescription('');
          reset();
        },
      }
    );
  }

  return (
    <form style={formCardStyle} onSubmit={handleSubmit}>
      <h2 style={formTitleStyle}>New project</h2>
      <label style={labelStyle}>
        Name
        <input
          style={inputStyle}
          type="text"
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="Project name"
          maxLength={255}
          disabled={isPending}
          required
        />
      </label>
      <label style={labelStyle}>
        Description (optional)
        <input
          style={inputStyle}
          type="text"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="Short description"
          maxLength={2000}
          disabled={isPending}
        />
      </label>
      {errorMessage && <p style={errorTextStyle}>{errorMessage}</p>}
      <button
        type="submit"
        style={isPending || !name.trim() ? submitButtonDisabledStyle : submitButtonStyle}
        disabled={isPending || !name.trim()}
      >
        {isPending ? 'Creating…' : 'Create project'}
      </button>
    </form>
  );
}

function ProjectCard({ project }: { project: Project }) {
  return (
    <li style={cardStyle}>
      <h2 style={cardTitleStyle}>{project.name}</h2>
      <p style={cardSlugStyle}>{project.slug}</p>
      {project.description && <p style={cardDescriptionStyle}>{project.description}</p>}
    </li>
  );
}
