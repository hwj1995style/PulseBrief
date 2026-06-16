export type IngestionJobStatus = 'RUNNING' | 'SUCCESS' | 'PARTIAL_SUCCESS' | 'FAILED';

export interface AdminIngestionJob {
  id: number;
  sourceCode: string;
  triggerType: string;
  status: IngestionJobStatus;
  startedAt: string;
  finishedAt: string | null;
  fetchedCount: number;
  newCount: number;
  duplicateCount: number;
  candidateCount: number;
  errorMessage: string | null;
}

export interface AdminIngestionMetrics {
  fetchedCount: number;
  candidateCount: number;
  publishedCount: number;
  failedCount: number;
}

export interface AdminIngestionSource {
  id: number;
  code: string;
  name: string;
  providerType: string;
  defaultCategoryCode: string | null;
  enabled: boolean;
  contentAccessPolicy: string;
  maxAgeHours: number | null;
  allowPdfDownload: boolean;
  allowFullText: boolean;
}

export interface AdminIngestionRunInput {
  pageSize?: number;
  generateCandidates?: boolean;
}

export interface AdminIngestionRunResult {
  jobId: number;
  sourceCode: string;
  providerType: string;
  status: IngestionJobStatus;
  fetchedCount: number;
  newCount: number;
  duplicateCount: number;
  candidateCount: number;
  errorMessage: string | null;
}

export interface AdminIngestionAnomaly {
  id: number;
  rawNewsItemId: number;
  title: string;
  sourceCode: string;
  sourceName: string;
  originalUrl: string;
  publishedAt: string | null;
  fetchedAt: string | null;
  issueType: string;
  severity: 'HIGH' | 'MEDIUM' | string;
  description: string;
}

export type AdminOperationLogAction = 'PUBLISH_ARTICLE' | 'PUBLISH_DIGEST' | 'OFFLINE_DIGEST' | string;

export interface AdminOperationLog {
  id: number;
  module: string;
  actionType: AdminOperationLogAction;
  targetType: string;
  targetId: number;
  targetTitle: string;
  status: string;
  operatorName: string;
  detail: string;
  createdAt: string;
}
