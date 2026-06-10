import { mockCandidates } from '../../mock/candidates';
import type { AdminCandidate, CandidateStatus, ReportAsset } from '../types/candidate';

export interface AdminApiClientConfig {
  apiBaseUrl?: string;
  adminToken?: string;
}

export interface AdminApiClient {
  getInitialCandidates: () => AdminCandidate[];
  listCandidates: (status?: CandidateStatus | 'ALL') => Promise<AdminCandidate[]>;
  getCandidate: (id: number) => Promise<AdminCandidate>;
  publishCandidate: (id: number) => Promise<AdminCandidate>;
  rejectCandidate: (id: number, reviewNote?: string) => Promise<AdminCandidate>;
}

interface BackendApiResponse<T> {
  code: string;
  message?: string;
  data: T;
  traceId?: string;
}

interface BackendPageResponse<T> {
  items: T[];
  page: number;
  pageSize: number;
  total: number;
  hasMore: boolean;
}

interface BackendCandidateResponse {
  id: number;
  rawNewsItemId: number;
  title: string;
  summary: string;
  categoryCode: string;
  sourceName: string;
  originalUrl: string;
  publishedAt: string | null;
  status: CandidateStatus;
  createdAt: string;
  publishedArticleId: number | null;
  reviewNote: string | null;
}

interface BackendRawNewsItemResponse {
  id: number;
  sourceCode: string;
  providerItemId: string;
  title: string;
  summary: string;
  sourceName: string;
  originalUrl: string;
  publishedAt: string | null;
  fetchedAt: string;
  language: string;
  country: string;
  status: string;
}

interface BackendReportAssetResponse {
  id: number;
  title: string;
  originalUrl: string;
  fileName: string;
  fileSizeBytes: number | null;
  fileHash: string | null;
  licensePolicy: string;
  status: string;
}

interface BackendCandidateDetailResponse {
  candidate: BackendCandidateResponse;
  rawItem: BackendRawNewsItemResponse | null;
  reportAssets: BackendReportAssetResponse[];
  duplicateHints: string[];
  availableActions: string[];
}

interface MutableAdminApiClient extends AdminApiClient {
  resetMockData?: () => void;
}

const categoryNameByCode: Record<string, string> = {
  ai: 'AI 前沿',
  macro: '宏观政策',
  finance: '财经市场',
  technology: '科技趋势',
  investment_view: '投行观点',
  industry: '产业观察',
  company: '公司动态',
  global: '全球热点'
};

const defaultApiBaseUrl = import.meta.env.VITE_ADMIN_API_BASE_URL;
const defaultAdminToken = import.meta.env.VITE_ADMIN_TOKEN ?? 'dev-admin-token';

export const adminApiConfig = {
  apiBaseUrl: defaultApiBaseUrl,
  adminToken: defaultAdminToken,
  mode: defaultApiBaseUrl ? 'api' : 'mock'
};

const defaultClient = createAdminApiClient({
  apiBaseUrl: adminApiConfig.apiBaseUrl,
  adminToken: adminApiConfig.adminToken
}) as MutableAdminApiClient;

export function createAdminApiClient(config: AdminApiClientConfig = {}): AdminApiClient {
  const apiBaseUrl = config.apiBaseUrl?.replace(/\/$/, '');
  if (!apiBaseUrl) {
    return createMockAdminApiClient();
  }
  return createHttpAdminApiClient({
    apiBaseUrl,
    adminToken: config.adminToken ?? 'dev-admin-token'
  });
}

export function getInitialCandidates(): AdminCandidate[] {
  return defaultClient.getInitialCandidates();
}

export function listCandidates(status?: CandidateStatus | 'ALL'): Promise<AdminCandidate[]> {
  return defaultClient.listCandidates(status);
}

export function getCandidate(id: number): Promise<AdminCandidate> {
  return defaultClient.getCandidate(id);
}

export function publishCandidate(id: number): Promise<AdminCandidate> {
  return defaultClient.publishCandidate(id);
}

export function rejectCandidate(id: number, reviewNote?: string): Promise<AdminCandidate> {
  return defaultClient.rejectCandidate(id, reviewNote);
}

export function resetAdminApiMock() {
  defaultClient.resetMockData?.();
}

function createMockAdminApiClient(): MutableAdminApiClient {
  let localCandidates = mockCandidates.map(cloneCandidate);

  function resetMockData() {
    localCandidates = mockCandidates.map(cloneCandidate);
  }

  function findCandidate(id: number) {
    const candidate = localCandidates.find((item) => item.id === id);
    if (!candidate) {
      throw new Error('Candidate not found');
    }
    return cloneCandidate(candidate);
  }

  function updateStatus(id: number, status: CandidateStatus) {
    localCandidates = localCandidates.map((candidate) =>
      candidate.id === id ? { ...candidate, status } : candidate
    );
    return findCandidate(id);
  }

  return {
    getInitialCandidates: () => localCandidates.map(cloneCandidate),
    listCandidates: async (status) => {
      if (!status || status === 'ALL') {
        return localCandidates.map(cloneCandidate);
      }
      return localCandidates.filter((candidate) => candidate.status === status).map(cloneCandidate);
    },
    getCandidate: async (id) => findCandidate(id),
    publishCandidate: async (id) => updateStatus(id, 'PUBLISHED'),
    rejectCandidate: async (id) => updateStatus(id, 'REJECTED'),
    resetMockData
  };
}

function createHttpAdminApiClient(config: Required<AdminApiClientConfig>): AdminApiClient {
  const headers = {
    Authorization: `Bearer ${config.adminToken}`,
    'Content-Type': 'application/json'
  };

  async function request<T>(path: string, init?: RequestInit): Promise<T> {
    const response = await fetch(`${config.apiBaseUrl}${path}`, {
      ...init,
      headers: {
        ...headers,
        ...init?.headers
      }
    });
    const payload = (await response.json()) as BackendApiResponse<T>;
    if (!response.ok || payload.code !== 'OK') {
      throw new Error(payload.message || `Admin API request failed: ${response.status}`);
    }
    return payload.data;
  }

  async function fetchCandidatePage(status: CandidateStatus): Promise<AdminCandidate[]> {
    const query = new URLSearchParams({
      status,
      page: '1',
      pageSize: '50'
    });
    const data = await request<BackendPageResponse<BackendCandidateResponse>>(
      `/api/admin/candidates?${query.toString()}`
    );
    return data.items.map(mapCandidate);
  }

  return {
    getInitialCandidates: () => [],
    listCandidates: async (status) => {
      if (!status || status === 'ALL') {
        const groups = await Promise.all([
          fetchCandidatePage('PENDING_REVIEW'),
          fetchCandidatePage('PUBLISHED'),
          fetchCandidatePage('REJECTED')
        ]);
        return groups.flat();
      }
      return fetchCandidatePage(status);
    },
    getCandidate: async (id) => {
      const detail = await request<BackendCandidateDetailResponse>(`/api/admin/candidates/${id}`);
      return mapCandidateDetail(detail);
    },
    publishCandidate: async (id) => {
      const candidate = await request<BackendCandidateResponse>(`/api/admin/candidates/${id}/publish`, {
        method: 'POST',
        body: JSON.stringify({ publishNow: true })
      });
      return mapCandidate(candidate);
    },
    rejectCandidate: async (id, reviewNote) => {
      const candidate = await request<BackendCandidateResponse>(`/api/admin/candidates/${id}/reject`, {
        method: 'POST',
        body: JSON.stringify(reviewNote ? { reviewNote } : {})
      });
      return mapCandidate(candidate);
    }
  };
}

function mapCandidate(candidate: BackendCandidateResponse): AdminCandidate {
  return {
    id: candidate.id,
    rawNewsItemId: candidate.rawNewsItemId,
    title: candidate.title,
    summary: candidate.summary,
    aiSummary: candidate.summary,
    categoryCode: candidate.categoryCode,
    categoryName: categoryNameByCode[candidate.categoryCode] ?? candidate.categoryCode,
    sourceName: candidate.sourceName,
    originalUrl: candidate.originalUrl,
    publishedAt: candidate.publishedAt ?? '',
    fetchedAt: candidate.createdAt,
    status: candidate.status,
    reportAssets: []
  };
}

function mapCandidateDetail(detail: BackendCandidateDetailResponse): AdminCandidate {
  const candidate = mapCandidate(detail.candidate);
  const rawItem = detail.rawItem;
  return {
    ...candidate,
    summary: rawItem?.summary || candidate.summary,
    aiSummary: rawItem?.summary || candidate.aiSummary,
    sourceName: rawItem?.sourceName || candidate.sourceName,
    originalUrl: rawItem?.originalUrl || candidate.originalUrl,
    publishedAt: rawItem?.publishedAt ?? candidate.publishedAt,
    fetchedAt: rawItem?.fetchedAt || candidate.fetchedAt,
    reportAssets: detail.reportAssets.map(mapReportAsset)
  };
}

function mapReportAsset(asset: BackendReportAssetResponse): ReportAsset {
  return {
    id: asset.id,
    title: asset.title,
    fileName: asset.fileName,
    licensePolicy: asset.licensePolicy,
    status: asset.status
  };
}

function cloneCandidate(candidate: AdminCandidate): AdminCandidate {
  return {
    ...candidate,
    reportAssets: candidate.reportAssets.map((asset) => ({ ...asset }))
  };
}
