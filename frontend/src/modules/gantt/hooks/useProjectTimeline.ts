import { useQuery } from '@tanstack/react-query';
import { timelineApi } from '../api/timelineApi';
import type { TimelineResponse } from '../types/timeline';

export function useProjectTimeline(projectId: string | undefined) {
  return useQuery<TimelineResponse>({
    queryKey: ['projects', projectId, 'timeline'],
    queryFn: () => timelineApi.getTimeline(projectId as string),
    enabled: Boolean(projectId),
  });
}
