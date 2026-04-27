import { useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';

const PARAM = 'taskId';

/**
 * Returns [selectedTaskId, setSelectedTaskId].
 * setSelectedTaskId(id) sets ?taskId=<id> preserving other params.
 * setSelectedTaskId(null) removes the param preserving other params.
 */
export function useSelectedTaskId(): [string | null, (id: string | null) => void] {
  const [searchParams, setSearchParams] = useSearchParams();
  const taskId = searchParams.get(PARAM);

  const setTaskId = useCallback(
    (id: string | null) => {
      setSearchParams(
        (prev) => {
          const next = new URLSearchParams(prev);
          if (id === null) {
            next.delete(PARAM);
          } else {
            next.set(PARAM, id);
          }
          return next;
        },
        { replace: true }
      );
    },
    [setSearchParams]
  );

  return [taskId, setTaskId];
}
