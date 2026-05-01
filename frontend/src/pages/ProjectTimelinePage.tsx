import 'gantt-task-react/dist/index.css';
import React, { createContext, useContext, useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Gantt, ViewMode } from 'gantt-task-react';
import type { Task as GanttTask } from 'gantt-task-react';
import { format } from 'date-fns';
import { useProjectTimeline } from '../modules/gantt/hooks/useProjectTimeline';
import { useScheduleTask } from '../modules/gantt/hooks/useScheduleTask';
import { mapTimelineToGantt } from '../modules/gantt/utils/mapTimelineToGantt';
import { ProjectNav } from '../modules/project/components/ProjectNav';

// Maximum chars for bar label display; longer names get a trailing ellipsis.
const BAR_LABEL_MAX = 18;

function truncateForBar(name: string): string {
  return name.length > BAR_LABEL_MAX ? name.slice(0, BAR_LABEL_MAX - 1) + '…' : name;
}

// Context that carries full (un-truncated) task titles to the custom table.
const FullTitleContext = createContext<Map<string, string>>(new Map());

type TaskListHeaderProps = {
  headerHeight: number;
  rowWidth: string;
  fontFamily: string;
  fontSize: string;
};

// Custom header that matches the 3-column layout of CustomTaskListTable.
const CustomTaskListHeader: React.FC<TaskListHeaderProps> = ({
  headerHeight,
  rowWidth,
  fontFamily,
  fontSize,
}) => (
  <div style={{ fontFamily, fontSize, borderLeft: '1px solid #e6e4e4', borderTop: '1px solid #e6e4e4', borderBottom: '1px solid #e6e4e4' }}>
    <div style={{ display: 'flex', height: headerHeight, fontWeight: 600, color: '#555' }}>
      <div style={{ ...cellStyle, minWidth: rowWidth, maxWidth: rowWidth }}>Name</div>
      <div style={{ ...cellStyle, minWidth: rowWidth, maxWidth: rowWidth }}>From</div>
      <div style={{ ...cellStyle, minWidth: rowWidth, maxWidth: rowWidth }}>To</div>
    </div>
  </div>
);

type TaskListTableProps = {
  rowHeight: number;
  rowWidth: string;
  fontFamily: string;
  fontSize: string;
  locale: string;
  tasks: GanttTask[];
  selectedTaskId: string;
  setSelectedTask: (taskId: string) => void;
  onExpanderClick: (task: GanttTask) => void;
};

// Custom list table: shows full names (from context) with CSS ellipsis truncation,
// plus From/To date columns to match the default library header layout.
const CustomTaskListTable: React.FC<TaskListTableProps> = ({
  rowHeight,
  rowWidth,
  fontFamily,
  fontSize,
  tasks,
  selectedTaskId,
  setSelectedTask,
}) => {
  const fullTitles = useContext(FullTitleContext);

  return (
    <div style={{ fontFamily, fontSize, borderBottom: '1px solid #e6e4e4', borderLeft: '1px solid #e6e4e4' }}>
      {tasks.map((task, idx) => {
        const fullName = fullTitles.get(task.id) ?? task.name;
        const isSelected = task.id === selectedTaskId;
        return (
          <div
            key={task.id + 'row'}
            style={{
              display: 'flex',
              height: rowHeight,
              backgroundColor: isSelected
                ? 'rgba(74,139,111,0.12)'
                : idx % 2 === 1
                  ? '#f5f5f5'
                  : undefined,
              cursor: 'pointer',
            }}
            onClick={() => setSelectedTask(task.id)}
          >
            {/* Name column */}
            <div style={{ ...cellStyle, minWidth: rowWidth, maxWidth: rowWidth }} title={fullName}>
              <span style={nameTextStyle}>{fullName}</span>
            </div>
            {/* From column */}
            <div style={{ ...cellStyle, minWidth: rowWidth, maxWidth: rowWidth }}>
              {format(task.start, 'MMM d, yyyy')}
            </div>
            {/* To column */}
            <div style={{ ...cellStyle, minWidth: rowWidth, maxWidth: rowWidth }}>
              {format(task.end, 'MMM d, yyyy')}
            </div>
          </div>
        );
      })}
    </div>
  );
};

const cellStyle: React.CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  padding: '0 8px',
  overflow: 'hidden',
  borderRight: '1px solid #e6e4e4',
  boxSizing: 'border-box',
};

const nameTextStyle: React.CSSProperties = {
  overflow: 'hidden',
  whiteSpace: 'nowrap',
  textOverflow: 'ellipsis',
  flex: 1,
  minWidth: 0,
};

export default function ProjectTimelinePage() {
  const { projectId } = useParams<{ projectId: string }>();
  const { data, isLoading, error } = useProjectTimeline(projectId);
  const scheduleTask = useScheduleTask(projectId);
  const [viewMode, setViewMode] = useState<ViewMode>(ViewMode.Day);

  const fullTitleMap = useMemo(
    () => new Map((data?.tasks ?? []).map(t => [t.id, t.title])),
    [data],
  );

  const { ganttTasks, datelessCount } = useMemo(() => {
    if (!data) return { ganttTasks: [] as GanttTask[], datelessCount: 0 };
    const result = mapTimelineToGantt(data.tasks, data.dependencies);
    return {
      datelessCount: result.datelessCount,
      ganttTasks: result.ganttTasks.map(t => ({
        ...t,
        name: truncateForBar(t.name),
      })),
    };
  }, [data]);

  // Called by gantt-task-react for both drag (move) and resize operations.
  // Returns false to undo the change when the range is inverted (end <= start).
  function handleDateChange(task: GanttTask): boolean | void {
    const { start, end } = task;

    // Guard: reject inverted or zero-length ranges — backend does not validate this.
    if (end <= start) {
      return false;
    }

    // date-fns format uses local time, avoiding UTC-midnight day-shift bugs that
    // toISOString().slice(0,10) causes in negative-UTC timezones.
    const startDate = format(start, 'yyyy-MM-dd');
    const dueDate = format(end, 'yyyy-MM-dd');

    scheduleTask.mutate({ taskId: task.id, startDate, dueDate });
  }

  if (isLoading) {
    return (
      <section style={sectionStyle}>
        <ProjectNav projectId={projectId} />
        <h1>Project timeline</h1>
        <p style={mutedStyle}>Loading…</p>
      </section>
    );
  }

  if (error) {
    return (
      <section style={sectionStyle}>
        <ProjectNav projectId={projectId} />
        <h1>Project timeline</h1>
        <p style={{ color: 'var(--color-danger)' }}>Failed to load timeline.</p>
      </section>
    );
  }

  if (!data || data.tasks.length === 0) {
    return (
      <section style={sectionStyle}>
        <ProjectNav projectId={projectId} />
        <h1>Project timeline</h1>
        <p style={mutedStyle}>No tasks yet.</p>
      </section>
    );
  }

  if (ganttTasks.length === 0) {
    return (
      <section style={sectionStyle}>
        <ProjectNav projectId={projectId} />
        <h1>Project timeline</h1>
        <p style={mutedStyle}>
          No tasks have start and due dates set ({datelessCount} task
          {datelessCount !== 1 ? 's' : ''} without dates).
        </p>
      </section>
    );
  }

  return (
    <FullTitleContext.Provider value={fullTitleMap}>
      <section style={sectionStyle}>
        <ProjectNav projectId={projectId} />
        <h1>Project timeline</h1>
        <div style={viewToggleBarStyle}>
          <div style={segmentedToggleStyle}>
            {([ViewMode.Day, ViewMode.Week] as const).map(mode => (
              <button
                key={mode}
                onClick={() => setViewMode(mode)}
                style={viewMode === mode ? activeSegmentStyle : inactiveSegmentStyle}
                aria-pressed={viewMode === mode}
              >
                {mode === ViewMode.Day ? 'Day' : 'Week'}
              </button>
            ))}
          </div>
        </div>
        <div className="ganttWrapper" style={ganttWrapperStyle}>
          <Gantt
            tasks={ganttTasks}
            viewMode={viewMode}
            listCellWidth="180px"
            barBackgroundColor="var(--color-accent-soft)"
            barBackgroundSelectedColor="var(--color-accent)"
            todayColor="rgba(74, 139, 111, 0.12)"
            onDateChange={handleDateChange}
            TaskListHeader={CustomTaskListHeader}
            TaskListTable={CustomTaskListTable}
          />
        </div>
        {datelessCount > 0 && (
          <p style={mutedStyle}>
            {datelessCount} task{datelessCount !== 1 ? 's' : ''} not shown — no
            start or due date set.
          </p>
        )}
      </section>
    </FullTitleContext.Provider>
  );
}

const sectionStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  gap: 'var(--space-4)',
  padding: 'var(--space-6)',
};

const ganttWrapperStyle: React.CSSProperties = {
  overflowX: 'auto',
  overflowY: 'visible',
  width: '100%',
};

const mutedStyle: React.CSSProperties = {
  color: 'var(--color-text-muted)',
  fontSize: 'var(--font-size-sm)',
};

const viewToggleBarStyle: React.CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  gap: 'var(--space-2)',
};

const segmentedToggleStyle: React.CSSProperties = {
  display: 'inline-flex',
  border: '1px solid var(--color-border)',
  borderRadius: 'var(--radius-md)',
  overflow: 'hidden',
  backgroundColor: 'var(--color-surface)',
};

const baseSegmentStyle: React.CSSProperties = {
  padding: '4px 14px',
  fontSize: 'var(--font-size-sm)',
  fontFamily: 'inherit',
  border: 'none',
  cursor: 'pointer',
  transition: 'background-color 0.15s, color 0.15s',
  lineHeight: 1.5,
};

const activeSegmentStyle: React.CSSProperties = {
  ...baseSegmentStyle,
  backgroundColor: 'var(--color-accent)',
  color: '#fff',
};

const inactiveSegmentStyle: React.CSSProperties = {
  ...baseSegmentStyle,
  backgroundColor: 'transparent',
  color: 'var(--color-text)',
};
