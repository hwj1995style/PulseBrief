import { mockCandidates } from '../../mock/candidates';
import { mockDigestArticleCandidates, mockDigests } from '../../mock/digests';
import { mockIngestionJobs, mockIngestionMetrics, mockIngestionSources } from '../../mock/ingestion';
import type { AdminCandidate, AdminCandidateUpdateInput, CandidateStatus, ReportAsset } from '../types/candidate';
import type {
  AdminDigest,
  AdminDigestArticleCandidate,
  AdminDigestCreateInput,
  AdminDigestStatus
} from '../types/digest';
import type {
  AdminIngestionJob,
  AdminIngestionMetrics,
  AdminIngestionSource,
  IngestionJobStatus
} from '../types/ingestion';

export interface AdminApiClientConfig {
  apiBaseUrl?: string;
  adminToken?: string;
}

export interface AdminApiClient {
  getInitialCandidates: () => AdminCandidate[];
  listCandidates: (status?: CandidateStatus | 'ALL') => Promise<AdminCandidate[]>;
  getCandidate: (id: number) => Promise<AdminCandidate>;
  updateCandidate: (id: number, input: AdminCandidateUpdateInput) => Promise<AdminCandidate>;
  publishCandidate: (id: number) => Promise<AdminCandidate>;
  rejectCandidate: (id: number, reviewNote?: string) => Promise<AdminCandidate>;
  getInitialDigests: () => AdminDigest[];
  listDigests: (status?: AdminDigestStatus | 'ALL') => Promise<AdminDigest[]>;
  listDigestArticleCandidates: (keyword?: string) => Promise<AdminDigestArticleCandidate[]>;
  createDigest: (input: AdminDigestCreateInput) => Promise<AdminDigest>;
  updateDigest: (id: number, input: AdminDigestCreateInput) => Promise<AdminDigest>;
  publishDigest: (id: number) => Promise<AdminDigest>;
  offlineDigest: (id: number) => Promise<AdminDigest>;
  listIngestionJobs: (status?: IngestionJobStatus | 'ALL') => Promise<AdminIngestionJob[]>;
  getTodayIngestionMetrics: () => Promise<AdminIngestionMetrics>;
  listIngestionSources: () => Promise<AdminIngestionSource[]>;
  updateIngestionSourceEnabled: (id: number, enabled: boolean) => Promise<AdminIngestionSource>;
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
  tagNames?: string[];
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

interface BackendDigestArticleResponse {
  articleId: number;
  sortNo: number;
  highlightText: string;
  title: string;
  sourceName: string;
}

interface BackendDigestResponse {
  id: number;
  digestDate: string;
  digestType: string;
  categoryCode: string;
  title: string;
  summary: string;
  content: string;
  audioText: string;
  status: AdminDigestStatus;
  publishTime: string | null;
  articleCount: number;
  articles: BackendDigestArticleResponse[];
  availableActions: string[];
}

interface BackendDigestArticleCandidateResponse {
  id: number;
  title: string;
  sourceName: string;
  publishTime: string;
  categoryName: string;
  summary: string;
}

interface BackendIngestionJobResponse {
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

interface BackendIngestionMetricsResponse {
  fetchedCount: number;
  candidateCount: number;
  publishedCount: number;
  failedCount: number;
}

interface BackendIngestionSourceResponse {
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

export function updateCandidate(id: number, input: AdminCandidateUpdateInput): Promise<AdminCandidate> {
  return defaultClient.updateCandidate(id, input);
}

export function publishCandidate(id: number): Promise<AdminCandidate> {
  return defaultClient.publishCandidate(id);
}

export function rejectCandidate(id: number, reviewNote?: string): Promise<AdminCandidate> {
  return defaultClient.rejectCandidate(id, reviewNote);
}

export function getInitialDigests(): AdminDigest[] {
  return defaultClient.getInitialDigests();
}

export function listDigests(status?: AdminDigestStatus | 'ALL'): Promise<AdminDigest[]> {
  return defaultClient.listDigests(status);
}

export function listDigestArticleCandidates(keyword?: string): Promise<AdminDigestArticleCandidate[]> {
  return defaultClient.listDigestArticleCandidates(keyword);
}

export function createDigest(input: AdminDigestCreateInput): Promise<AdminDigest> {
  return defaultClient.createDigest(input);
}

export function updateDigest(id: number, input: AdminDigestCreateInput): Promise<AdminDigest> {
  return defaultClient.updateDigest(id, input);
}

export function publishDigest(id: number): Promise<AdminDigest> {
  return defaultClient.publishDigest(id);
}

export function offlineDigest(id: number): Promise<AdminDigest> {
  return defaultClient.offlineDigest(id);
}

export function listIngestionJobs(status?: IngestionJobStatus | 'ALL'): Promise<AdminIngestionJob[]> {
  return defaultClient.listIngestionJobs(status);
}

export function getTodayIngestionMetrics(): Promise<AdminIngestionMetrics> {
  return defaultClient.getTodayIngestionMetrics();
}

export function listIngestionSources(): Promise<AdminIngestionSource[]> {
  return defaultClient.listIngestionSources();
}

export function updateIngestionSourceEnabled(id: number, enabled: boolean): Promise<AdminIngestionSource> {
  return defaultClient.updateIngestionSourceEnabled(id, enabled);
}

export function resetAdminApiMock() {
  defaultClient.resetMockData?.();
}

function createMockAdminApiClient(): MutableAdminApiClient {
  let localCandidates = mockCandidates.map(cloneCandidate);
  let localDigests = mockDigests.map(cloneDigest);
  let localIngestionJobs = mockIngestionJobs.map(cloneIngestionJob);
  let localIngestionSources = mockIngestionSources.map(cloneIngestionSource);
  let nextDigestId = 900;

  function resetMockData() {
    localCandidates = mockCandidates.map(cloneCandidate);
    localDigests = mockDigests.map(cloneDigest);
    localIngestionJobs = mockIngestionJobs.map(cloneIngestionJob);
    localIngestionSources = mockIngestionSources.map(cloneIngestionSource);
    nextDigestId = 900;
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

  function updateCandidate(id: number, input: AdminCandidateUpdateInput) {
    localCandidates = localCandidates.map((candidate) =>
      candidate.id === id
        ? {
            ...candidate,
            ...input,
            categoryName: categoryNameByCode[input.categoryCode] ?? input.categoryCode
          }
        : candidate
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
    updateCandidate: async (id, input) => updateCandidate(id, input),
    publishCandidate: async (id) => updateStatus(id, 'PUBLISHED'),
    rejectCandidate: async (id) => updateStatus(id, 'REJECTED'),
    getInitialDigests: () => localDigests.map(cloneDigest),
    listDigests: async (status) => {
      if (!status || status === 'ALL') {
        return localDigests.map(cloneDigest);
      }
      return localDigests.filter((digest) => digest.status === status).map(cloneDigest);
    },
    listDigestArticleCandidates: async (keyword) => {
      const safeKeyword = keyword?.trim();
      if (!safeKeyword) {
        return mockDigestArticleCandidates.map((article) => ({ ...article }));
      }
      return mockDigestArticleCandidates
        .filter((article) => `${article.title}${article.summary}${article.sourceName}`.includes(safeKeyword))
        .map((article) => ({ ...article }));
    },
    createDigest: async (input) => {
      const articles = input.articles.map((article) => {
        const source = mockDigestArticleCandidates.find((candidate) => candidate.id === article.articleId);
        return {
          articleId: article.articleId,
          sortNo: article.sortNo,
          highlightText: article.highlightText,
          title: source?.title ?? '',
          sourceName: source?.sourceName ?? ''
        };
      });
      const digest: AdminDigest = {
        id: nextDigestId++,
        ...input,
        status: 'DRAFT',
        publishTime: null,
        articleCount: articles.length,
        articles,
        availableActions: ['EDIT', 'PUBLISH']
      };
      localDigests = [digest, ...localDigests];
      return cloneDigest(digest);
    },
    updateDigest: async (id, input) => {
      const articles = input.articles.map((article) => {
        const source = mockDigestArticleCandidates.find((candidate) => candidate.id === article.articleId);
        return {
          articleId: article.articleId,
          sortNo: article.sortNo,
          highlightText: article.highlightText,
          title: source?.title ?? '',
          sourceName: source?.sourceName ?? ''
        };
      });
      localDigests = localDigests.map((digest) =>
        digest.id === id
          ? {
              ...digest,
              ...input,
              status: 'DRAFT',
              publishTime: null,
              articleCount: articles.length,
              articles,
              availableActions: ['EDIT', 'PUBLISH']
            }
          : digest
      );
      const digest = localDigests.find((item) => item.id === id);
      if (!digest) {
        throw new Error('Digest not found');
      }
      return cloneDigest(digest);
    },
    publishDigest: async (id) => {
      localDigests = localDigests.map((digest) =>
        digest.id === id
          ? {
              ...digest,
              status: 'PUBLISHED',
              publishTime: new Date().toISOString(),
              availableActions: ['OFFLINE']
            }
          : digest
      );
      const digest = localDigests.find((item) => item.id === id);
      if (!digest) {
        throw new Error('Digest not found');
      }
      return cloneDigest(digest);
    },
    offlineDigest: async (id) => {
      localDigests = localDigests.map((digest) =>
        digest.id === id
          ? {
              ...digest,
              status: 'OFFLINE',
              availableActions: []
            }
          : digest
      );
      const digest = localDigests.find((item) => item.id === id);
      if (!digest) {
        throw new Error('Digest not found');
      }
      return cloneDigest(digest);
    },
    listIngestionJobs: async (status) => {
      if (!status || status === 'ALL') {
        return localIngestionJobs.map(cloneIngestionJob);
      }
      return localIngestionJobs.filter((job) => job.status === status).map(cloneIngestionJob);
    },
    getTodayIngestionMetrics: async () => ({ ...mockIngestionMetrics }),
    listIngestionSources: async () => localIngestionSources.map(cloneIngestionSource),
    updateIngestionSourceEnabled: async (id, enabled) => {
      localIngestionSources = localIngestionSources.map((source) =>
        source.id === id ? { ...source, enabled } : source
      );
      const source = localIngestionSources.find((item) => item.id === id);
      if (!source) {
        throw new Error('Ingestion source not found');
      }
      return cloneIngestionSource(source);
    },
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
    updateCandidate: async (id, input) => {
      const candidate = await request<BackendCandidateResponse>(`/api/admin/candidates/${id}`, {
        method: 'PUT',
        body: JSON.stringify(input)
      });
      return mapCandidate(candidate);
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
    },
    getInitialDigests: () => [],
    listDigests: async (status) => {
      if (!status || status === 'ALL') {
        const groups = await Promise.all([fetchDigestPage('DRAFT'), fetchDigestPage('PUBLISHED')]);
        return groups.flat();
      }
      return fetchDigestPage(status);
    },
    listDigestArticleCandidates: async (keyword) => {
      const query = new URLSearchParams({
        page: '1',
        pageSize: '50'
      });
      if (keyword?.trim()) {
        query.set('keyword', keyword.trim());
      }
      const data = await request<BackendPageResponse<BackendDigestArticleCandidateResponse>>(
        `/api/admin/digests/article-candidates?${query.toString()}`
      );
      return data.items.map(mapDigestArticleCandidate);
    },
    createDigest: async (input) => {
      const digest = await request<BackendDigestResponse>('/api/admin/digests', {
        method: 'POST',
        body: JSON.stringify(input)
      });
      return mapDigest(digest);
    },
    updateDigest: async (id, input) => {
      const digest = await request<BackendDigestResponse>(`/api/admin/digests/${id}`, {
        method: 'PUT',
        body: JSON.stringify(input)
      });
      return mapDigest(digest);
    },
    publishDigest: async (id) => {
      const digest = await request<BackendDigestResponse>(`/api/admin/digests/${id}/publish`, {
        method: 'POST',
        body: JSON.stringify({ publishNow: true })
      });
      return mapDigest(digest);
    },
    offlineDigest: async (id) => {
      const digest = await request<BackendDigestResponse>(`/api/admin/digests/${id}/offline`, {
        method: 'POST'
      });
      return mapDigest(digest);
    },
    listIngestionJobs: async (status) => {
      const query = new URLSearchParams();
      if (status && status !== 'ALL') {
        query.set('status', status);
      }
      query.set('page', '1');
      query.set('pageSize', '20');
      const data = await request<BackendPageResponse<BackendIngestionJobResponse>>(
        `/api/admin/ingestion/jobs?${query.toString()}`
      );
      return data.items.map(mapIngestionJob);
    },
    getTodayIngestionMetrics: async () => {
      const data = await request<BackendIngestionMetricsResponse>('/api/admin/ingestion/metrics/today');
      return { ...data };
    },
    listIngestionSources: async () => {
      const data = await request<BackendIngestionSourceResponse[]>('/api/admin/ingestion/sources');
      return data.map(mapIngestionSource);
    },
    updateIngestionSourceEnabled: async (id, enabled) => {
      const source = await request<BackendIngestionSourceResponse>(
        `/api/admin/ingestion/sources/${id}/enabled`,
        {
          method: 'PUT',
          body: JSON.stringify({ enabled })
        }
      );
      return mapIngestionSource(source);
    }
  };

  async function fetchDigestPage(status: AdminDigestStatus): Promise<AdminDigest[]> {
    const query = new URLSearchParams({
      status,
      page: '1',
      pageSize: '50'
    });
    const data = await request<BackendPageResponse<BackendDigestResponse>>(
      `/api/admin/digests?${query.toString()}`
    );
    return data.items.map(mapDigest);
  }
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
    tagNames: candidate.tagNames ?? [],
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
    tagNames: [...(candidate.tagNames ?? [])],
    reportAssets: candidate.reportAssets.map((asset) => ({ ...asset }))
  };
}

function mapDigest(digest: BackendDigestResponse): AdminDigest {
  return {
    id: digest.id,
    digestDate: digest.digestDate,
    digestType: digest.digestType,
    categoryCode: digest.categoryCode,
    title: digest.title,
    summary: digest.summary,
    content: digest.content,
    audioText: digest.audioText,
    status: digest.status,
    publishTime: digest.publishTime,
    articleCount: digest.articleCount,
    articles: digest.articles.map((article) => ({ ...article })),
    availableActions: digest.availableActions
  };
}

function mapDigestArticleCandidate(article: BackendDigestArticleCandidateResponse): AdminDigestArticleCandidate {
  return {
    id: article.id,
    title: article.title,
    sourceName: article.sourceName,
    publishTime: article.publishTime,
    categoryName: article.categoryName,
    summary: article.summary
  };
}

function cloneDigest(digest: AdminDigest): AdminDigest {
  return {
    ...digest,
    articles: digest.articles.map((article) => ({ ...article })),
    availableActions: [...digest.availableActions]
  };
}

function mapIngestionJob(job: BackendIngestionJobResponse): AdminIngestionJob {
  return { ...job };
}

function mapIngestionSource(source: BackendIngestionSourceResponse): AdminIngestionSource {
  return { ...source };
}

function cloneIngestionJob(job: AdminIngestionJob): AdminIngestionJob {
  return { ...job };
}

function cloneIngestionSource(source: AdminIngestionSource): AdminIngestionSource {
  return { ...source };
}
