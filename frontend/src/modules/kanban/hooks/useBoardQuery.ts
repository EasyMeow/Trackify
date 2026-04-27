import { useQuery } from '@tanstack/react-query';
import { boardApi } from '../api/boardApi';
import type { BoardResponse } from '../types/board';

export function useBoardQuery(projectId: string | undefined) {
  return useQuery<BoardResponse>({
    queryKey: ['projects', projectId, 'board'],
    queryFn: () => boardApi.getBoard(projectId as string),
    enabled: Boolean(projectId),
  });
}
