import type { BoardColumn } from '../types/board';

export interface BoardFilterValues {
  titleSearch: string;
  selectedColumnIds: string[];
  dateFrom: string;
  dateTo: string;
}

interface BoardFiltersProps {
  columns: BoardColumn[];
  filters: BoardFilterValues;
  onChange: (filters: BoardFilterValues) => void;
}

const barStyle: React.CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  gap: 'var(--space-3)',
  flexWrap: 'wrap',
  padding: 'var(--space-2) var(--space-3)',
  backgroundColor: 'var(--color-bg-muted)',
  border: '1px solid var(--color-border)',
  borderRadius: 'var(--radius-md)',
};

const inputStyle: React.CSSProperties = {
  padding: 'var(--space-1) var(--space-2)',
  fontSize: 'var(--font-size-sm)',
  color: 'var(--color-text)',
  backgroundColor: 'var(--color-bg)',
  border: '1px solid var(--color-border)',
  borderRadius: 'var(--radius-sm)',
  outline: 'none',
  minWidth: '160px',
};

const labelStyle: React.CSSProperties = {
  fontSize: 'var(--font-size-xs)',
  color: 'var(--color-text-muted)',
  fontWeight: 'var(--font-weight-medium)',
  textTransform: 'uppercase',
  letterSpacing: '0.04em',
  whiteSpace: 'nowrap',
};

const groupStyle: React.CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  gap: 'var(--space-2)',
};

const chipRowStyle: React.CSSProperties = {
  display: 'flex',
  flexWrap: 'wrap',
  gap: 'var(--space-1)',
};

const clearBtnStyle: React.CSSProperties = {
  marginLeft: 'auto',
  padding: 'var(--space-1) var(--space-2)',
  fontSize: 'var(--font-size-xs)',
  color: 'var(--color-text-subtle)',
  backgroundColor: 'transparent',
  border: '1px solid var(--color-border)',
  borderRadius: 'var(--radius-sm)',
  cursor: 'pointer',
};

function chipStyle(active: boolean): React.CSSProperties {
  return {
    padding: '2px var(--space-2)',
    fontSize: 'var(--font-size-xs)',
    fontWeight: active ? 'var(--font-weight-semibold)' : 'var(--font-weight-normal)',
    color: active ? 'var(--color-text-on-accent)' : 'var(--color-text-subtle)',
    backgroundColor: active ? 'var(--color-accent)' : 'var(--color-bg)',
    border: `1px solid ${active ? 'var(--color-accent)' : 'var(--color-border)'}`,
    borderRadius: 'var(--radius-sm)',
    cursor: 'pointer',
    whiteSpace: 'nowrap',
  };
}

export function BoardFilters({ columns, filters, onChange }: BoardFiltersProps) {
  const hasFilters =
    filters.titleSearch !== '' ||
    filters.selectedColumnIds.length > 0 ||
    filters.dateFrom !== '' ||
    filters.dateTo !== '';

  function toggleColumn(id: string) {
    const next = filters.selectedColumnIds.includes(id)
      ? filters.selectedColumnIds.filter((c) => c !== id)
      : [...filters.selectedColumnIds, id];
    onChange({ ...filters, selectedColumnIds: next });
  }

  function clear() {
    onChange({ titleSearch: '', selectedColumnIds: [], dateFrom: '', dateTo: '' });
  }

  return (
    <div style={barStyle}>
      <div style={groupStyle}>
        <span style={labelStyle}>Search</span>
        <input
          style={inputStyle}
          type="text"
          placeholder="Filter by title…"
          value={filters.titleSearch}
          onChange={(e) => onChange({ ...filters, titleSearch: e.target.value })}
        />
      </div>

      {columns.length > 0 && (
        <div style={groupStyle}>
          <span style={labelStyle}>Status</span>
          <div style={chipRowStyle}>
            {columns.map((col) => (
              <button
                key={col.id}
                type="button"
                style={chipStyle(filters.selectedColumnIds.includes(col.id))}
                onClick={() => toggleColumn(col.id)}
              >
                {col.name}
              </button>
            ))}
          </div>
        </div>
      )}

      <div style={groupStyle}>
        <span style={labelStyle}>Created</span>
        <input
          style={{ ...inputStyle, minWidth: '130px' }}
          type="date"
          value={filters.dateFrom}
          onChange={(e) => onChange({ ...filters, dateFrom: e.target.value })}
        />
        <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)' }}>–</span>
        <input
          style={{ ...inputStyle, minWidth: '130px' }}
          type="date"
          value={filters.dateTo}
          onChange={(e) => onChange({ ...filters, dateTo: e.target.value })}
        />
      </div>

      {hasFilters && (
        <button type="button" style={clearBtnStyle} onClick={clear}>
          Clear
        </button>
      )}
    </div>
  );
}
