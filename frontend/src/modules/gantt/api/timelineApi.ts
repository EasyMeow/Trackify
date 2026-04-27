import { httpClient } from '../../../shared/api/httpClient';
import type { TimelineResponse } from '../types/timeline';

export const timelineApi = {
  getTimeline(projectId: string): Promise<TimelineResponse> {
    return httpClient.get<TimelineResponse>(`/projects/${projectId}/timeline`);
  },
};
