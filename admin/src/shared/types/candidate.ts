export type CandidateStatus = 'PENDING_REVIEW' | 'PUBLISHED' | 'REJECTED';
export type CandidateContentFetchMode = 'SNIPPET' | 'FULLTEXT';

export interface ReportAsset {
  id: number;
  title: string;
  originalUrl: string;
  fileName: string;
  fileSizeBytes: number | null;
  fileHash: string | null;
  licensePolicy: string;
  status: string;
  licenseNote: string | null;
  cacheStatus: string;
  cacheErrorMessage: string | null;
  mimeType: string | null;
  cachedAt: string | null;
  reviewNote: string | null;
  reviewedAt: string | null;
  reviewedBy: string | null;
}

export interface CandidateContent {
  candidateId: number;
  rawNewsItemId: number;
  captureMode: CandidateContentFetchMode;
  fetchStatus: string;
  preview: string | null;
  licensePolicy: string | null;
  licenseNote: string | null;
  fetchedAt: string | null;
  errorMessage: string | null;
}

export interface AdminCandidate {
  id: number;
  rawNewsItemId: number;
  title: string;
  summary: string;
  aiSummary: string;
  categoryCode: string;
  categoryName: string;
  sourceName: string;
  tagNames: string[];
  originalUrl: string;
  publishedAt: string;
  fetchedAt: string;
  status: CandidateStatus;
  content: CandidateContent | null;
  reportAssets: ReportAsset[];
}

export interface AdminCandidateUpdateInput {
  title: string;
  summary: string;
  categoryCode: string;
  sourceName: string;
  tagNames: string[];
}
