export type CandidateStatus = 'PENDING_REVIEW' | 'PUBLISHED' | 'REJECTED';
export type CandidateContentFetchMode = 'SNIPPET' | 'FULLTEXT';

export interface ReportAsset {
  id: number;
  title: string;
  fileName: string;
  licensePolicy: string;
  status: string;
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
