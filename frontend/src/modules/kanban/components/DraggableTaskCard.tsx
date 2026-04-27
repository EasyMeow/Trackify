import { useDraggable } from '@dnd-kit/core';
import { CSS } from '@dnd-kit/utilities';
import { TaskCard } from './TaskCard';
import type { BoardTaskCard } from '../types/board';

interface DraggableTaskCardProps {
  task: BoardTaskCard;
  columnId: string;
  isActive?: boolean;
  onSelect?: (id: string) => void;
}

export function DraggableTaskCard({ task, columnId, isActive, onSelect }: DraggableTaskCardProps) {
  const { attributes, listeners, setNodeRef, transform, isDragging } = useDraggable({
    id: task.id,
    data: { taskId: task.id, columnId },
  });

  const style: React.CSSProperties = {
    transform: CSS.Translate.toString(transform),
    opacity: isDragging ? 0.4 : 1,
    touchAction: 'none',
  };

  return (
    <div ref={setNodeRef} style={style} {...listeners} {...attributes}>
      <TaskCard task={task} isActive={isActive} onSelect={onSelect} />
    </div>
  );
}
