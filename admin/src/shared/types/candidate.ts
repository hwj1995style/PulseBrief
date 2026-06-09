export type CandidateStatus = 'PENDING_REVIEW' | 'PUBLISHED' | 'REJECTED';

export interface ReportAsset {
  id: number;
  title: string;
  fileName: string;
  licensePolicy: string;
  status: string;
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
  originalUrl: string;
  publishedAt: string;
  fetchedAt: string;
  status: CandidateStatus;
  reportAssets: ReportAsset[];
}
