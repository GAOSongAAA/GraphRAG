export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

export type RetrievalMode = 'vector' | 'graph' | 'hybrid';

export interface GraphRagRequest {
  question: string;
  maxDocuments?: number;
  maxEntities?: number;
  similarityThreshold?: number;
  retrievalMode?: RetrievalMode;
  parameters?: Record<string, any>;
}

export interface Segment {
  content: string;
  score: number;
  type: 'document' | 'entity';
  source?: string;
}

export interface GraphRagResponse {
  answer: string;
  segments: Segment[];
  processingTimeMs: number;
}

export interface QueryAnalysis {
  queryType: 'factual' | 'comparative' | 'summary' | 'unknown';
  expectedAnswerType: 'short' | 'list' | 'paragraph' | 'table';
  complexity: 'simple' | 'medium' | 'complex';
}

export interface GraphStats {
  nodeCount: number;
  edgeCount: number;
  labels: Record<string, number>;
  [key: string]: any;
}

export interface RelatedEntity {
  properties: Record<string, any>;
  labels: string[];
}
export interface Relationship {
  type: string;
  properties: Record<string, any>;
}
export interface PathSegment {
  start: RelatedEntity;
  relationship: Relationship;
  end: RelatedEntity;
}

export interface RelatedEntityInfo {
  entity: RelatedEntity;
  path: PathSegment[];
  hops: number;
}
