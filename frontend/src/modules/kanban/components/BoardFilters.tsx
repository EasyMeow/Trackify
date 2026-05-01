import { useState, useEffect, useRef } from 'react';
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

const dropdownPanelStyle: React.CSSProperties = {
  position: 'absolute',
  top: '100%',
  left: 0,
  marginTop: '4px',
  zIndex: 100,
  backgroundColor: 'var(--color-bg)',
  border: '1px solid var(--color-border)',
  borderRadius: 'var(--radius-md)',
  boxShadow: '0 4px 12px rgba(0,0,0,0.08)',
  minWidth: '160px',
  maxHeight: '240px',
  overflowY: 'auto',
  padding: 'var(--space-1) 0',
};

const checkboxItemStyle: React.CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  gap: 'var(--space-2)',
  padding: 'var(--space-1) var(--space-3)',
  cursor: 'pointer',
  fontSize: 'var(--font-size-sm)',
  color: 'var(--color-text)',
  userSelect: 'none',
};

function triggerStyle(active: boolean): React.CSSProperties {
  return {
    padding: 'var(--space-1) var(--space-2)',
    fontSize: 'var(--font-size-xs)',
    fontWeight: active ? 'var(--font-weight-semibold)' : 'var(--font-weight-normal)',
    color: active ? 'var(--color-text-on-accent)' : 'var(--color-text-subtle)',
    backgroundColor: active ? 'var(--color-accent)' : 'var(--color-bg)',
    border: `1px solid ${active ? 'var(--color-accent)' : 'var(--color-border)'}`,
    borderRadius: 'var(--radius-sm)',
    cursor: 'pointer',
    whiteSpace: 'nowrap',
    display: 'flex',
    alignItems: 'center',
    gap: '4px',
  };
}

export function BoardFilters({ columns, filters, onChange }: BoardFiltersProps) {
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  const hasFilters =
    filters.titleSearch !== '' ||
    filters.selectedColumnIds.length > 0 ||
    filters.dateFrom !== '' ||
    filters.dateTo !== '';

  const selectedCount = filters.selectedColumnIds.length;

  function toggleColumn(id: string) {
    const next = filters.selectedColumnIds.includes(id)
      ? filters.selectedColumnIds.filter((c) => c !== id)
      : [...filters.selectedColumnIds, id];
    onChange({ ...filters, selectedColumnIds: next });
  }

  function clear() {
    onChange({ titleSearch: '', selectedColumnIds: [], dateFrom: '', dateTo: '' });
  }

  useEffect(() => {
    if (!dropdownOpen) return;
    function handleClickOutside(e: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setDropdownOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [dropdownOpen]);

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
          <div ref={dropdownRef} style={{ position: 'relative' }}>
            <button
              type="button"
              style={triggerStyle(selectedCount > 0)}
              onClick={() => setDropdownOpen((o) => !o)}
              aria-haspopup="listbox"
              aria-expanded={dropdownOpen}
            >
              Status{selectedCount > 0 ? ` (${selectedCount})` : ''}
              <span style={{ fontSize: '9px' }}>{dropdownOpen ? '▲' : '▼'}</span>
            </button>
            {dropdownOpen && (
              <div style={dropdownPanelStyle} role="listbox" aria-multiselectable="true">
                {columns.map((col) => (
                  <label key={col.id} style={checkboxItemStyle}>
                    <input
                      type="checkbox"
                      checked={filters.selectedColumnIds.includes(col.id)}
                      onChange={() => toggleColumn(col.id)}
                      style={{ accentColor: 'var(--color-accent)', cursor: 'pointer' }}
                    />
                    {col.name}
                  </label>
                ))}
              </div>
            )}
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
