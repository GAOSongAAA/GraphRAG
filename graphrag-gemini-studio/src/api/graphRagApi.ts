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
    axiosClient.get(`/entities/${entityName}/related`, {
      params: { maxHops, maxResults },
    }),
};
