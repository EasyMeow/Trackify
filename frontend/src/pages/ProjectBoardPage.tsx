import { useParams } from 'react-router-dom';
import { useBoardQuery } from '../modules/kanban/hooks/useBoardQuery';
import { BoardColumnView } from '../modules/kanban/components/BoardColumnView';

const pageStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  gap: 'var(--space-4)',
  padding: 'var(--space-5)',
  height: '100%',
  boxSizing: 'border-box',
};

const boardStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'row',
  gap: 'var(--space-4)',
  alignItems: 'flex-start',
  overflowX: 'auto',
  paddingBottom: 'var(--space-4)',
  flex: 1,
};

const mutedStyle: React.CSSProperties = {
  margin: 0,
  color: 'var(--color-text-muted)',
  fontSize: 'var(--font-size-sm)',
};

const headingStyle: React.CSSProperties = {
  margin: 0,
  fontSize: 'var(--font-size-xl)',
  fontWeight: 'var(--font-weight-semibold)',
  color: 'var(--color-text)',
};

export default function ProjectBoardPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const { data, isLoading, isError } = useBoardQuery(projectId);

  return (
    <section style={pageStyle}>
      <h1 style={headingStyle}>Board</h1>
      {isLoading && <p style={mutedStyle}>Loading board…</p>}
      {isError && <p style={{ ...mutedStyle, color: 'var(--color-danger)' }}>Couldn't load board.</p>}
      {data && (
        <div style={boardStyle}>
          {data.columns.map((column) => (
            <BoardColumnView key={column.id} column={column} />
          ))}
        </div>
      )}
    </section>
  );
}
