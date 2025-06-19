export interface ApiResponse<T> {
  /** 接口调用是否成功 */
  success: boolean;
  /** 返回代码，如 SUCCESS/ERROR */
  code: string;
  /** 提示信息 */
  message: string;
  /** 实际数据 */
  data: T;
  /** 服务器返回时间 */
  timestamp?: string;
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
  question: string;
  answer: string;
  /** 相关文档列表 */
  relevantDocuments: Record<string, any>[];
  /** 相关实体列表 */
  relevantEntities: Record<string, any>[];
  /** 实体关系路径 */
  relationshipPaths?: Record<string, any>[];
  /** 模型信心度 */
  confidence?: number;
  /** 处理耗时 */
  processingTimeMs: number;
  /** 兼容旧版字段 */
  segments?: Segment[];
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
