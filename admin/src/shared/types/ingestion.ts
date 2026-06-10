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
