import axiosClient from './axiosClient';
import {
  GraphRagRequest,
  GraphRagResponse,
  QueryAnalysis,
  GraphStats,
  RelatedEntityInfo,
  ApiResponse,
} from './types';

export const graphRagApi = {
  query: (
    request: GraphRagRequest
  ): Promise<ApiResponse<GraphRagResponse>> =>
    axiosClient.post('/query', request),

  queryStream: (
    request: GraphRagRequest,
    onMessage: (data: ApiResponse<GraphRagResponse>) => void,
    onError?: (err: any) => void
  ): EventSource => {
    const params = new URLSearchParams({
      question: request.question,
      retrievalMode: request.retrievalMode || 'hybrid',
    });
    const es = new EventSource(`/query/stream?${params.toString()}`);
    es.onmessage = (ev) => {
      onMessage(JSON.parse(ev.data));
    };
    es.onerror = (err) => {
      onError?.(err);
      es.close();
    };
    return es;
  },

  submitAsyncQuery: (
    request: GraphRagRequest
  ): Promise<ApiResponse<string>> =>
    axiosClient.post('/query/async', request),

  getAsyncResult: (
    taskId: string
  ): Promise<ApiResponse<'running' | 'task not found' | GraphRagResponse>> =>
    axiosClient.get(`/query/async/${taskId}`),

  analyzeQuery: (query: string): Promise<ApiResponse<QueryAnalysis>> =>
    axiosClient.post('/analyze', null, { params: { query } }),

  uploadDocument: (
    file: File,
    source?: string
  ): Promise<ApiResponse<string>> => {
    const formData = new FormData();
    formData.append('file', file);
    if (source) formData.append('source', source);
    return axiosClient.post('/documents/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },

  getStats: (): Promise<ApiResponse<GraphStats>> => axiosClient.get('/stats'),

  checkHealth: (): Promise<ApiResponse<string>> => axiosClient.get('/health'),

  clearGraph: (): Promise<ApiResponse<string>> => axiosClient.delete('/clear'),

  getRelatedEntities: (
    entityName: string,
    maxHops = 2,
    maxResults = 20
  ): Promise<ApiResponse<RelatedEntityInfo[]>> =>
    axiosClient
      .get<ApiResponse<any[]>>(`/entities/${entityName}/related`, {
        params: { maxHops, maxResults },
      })
      .then((resp) => {
        const apiResp = resp as unknown as ApiResponse<any[]>;
        const mapped = (apiResp.data || []).map((item) => {
          const entity = {
            properties: {
              name: item.entityName,
              type: item.entityType,
              description: item.description,
            },
            labels: [],
          } as RelatedEntityInfo['entity'];

          const path: RelatedEntityInfo['path'] = [];
          const nodes: string[] = item.pathNodes || [];
          const rels: string[] = item.relationshipTypes || [];

          for (let i = 0; i < rels.length; i++) {
            path.push({
              start: {
                properties: { name: nodes[i] },
                labels: [],
              },
              relationship: {
                type: rels[i],
                properties: {},
              },
              end: {
                properties: { name: nodes[i + 1] },
                labels: [],
              },
            });
          }

          return {
            entity,
            path,
            hops: item.pathLength ?? rels.length,
          } as RelatedEntityInfo;
        });

        return { ...apiResp, data: mapped } as ApiResponse<RelatedEntityInfo[]>;
      }),
};

