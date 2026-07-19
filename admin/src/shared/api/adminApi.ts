import { mockCandidates } from '../../mock/candidates';
import { mockDigestArticleCandidates, mockDigests } from '../../mock/digests';
import {
  mockIngestionJobs,
  mockIngestionMetrics,
  mockIngestionSources,
  mockIngestionAnomalies,
  mockOperationLogs
} from '../../mock/ingestion';
import type {
  AdminCandidate,
  AdminCandidatePublishInput,
  AdminCandidateUpdateInput,
  CandidateAiSummaryTask,
  CandidateContent,
  CandidateContentFetchMode,
  CandidateStatus,
  ReportAsset
} from '../types/candidate';
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
  AdminIngestionRunInput,
  AdminIngestionRunResult,
  AdminIngestionAnomaly,
  AdminOperationLog,
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
  fetchCandidateContent: (id: number, mode: CandidateContentFetchMode) => Promise<CandidateContent>;
  cacheCandidateReportAsset: (candidateId: number, assetId: number) => Promise<ReportAsset>;
  approveCandidateReportAsset: (candidateId: number, assetId: number, reviewNote?: string) => Promise<ReportAsset>;
  rejectCandidateReportAsset: (candidateId: number, assetId: number, reviewNote?: string) => Promise<ReportAsset>;
  generateCandidateAiSummary: (id: number) => Promise<CandidateAiSummaryTask>;
  applyCandidateAiSummary: (id: number, taskId: number) => Promise<CandidateAiSummaryTask>;
  updateCandidate: (id: number, input: AdminCandidateUpdateInput) => Promise<AdminCandidate>;
  publishCandidate: (id: number, input?: AdminCandidatePublishInput) => Promise<AdminCandidate>;
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
  runIngestionSource: (id: number, input?: AdminIngestionRunInput) => Promise<AdminIngestionRunResult>;
  listIngestionAnomalies: () => Promise<AdminIngestionAnomaly[]>;
  listOperationLogs: (module?: string) => Promise<AdminOperationLog[]>;
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
  suggestedCategoryCode: string | null;
  classificationConfidence: number | null;
  classificationRule: string | null;
  categoryOverrideReason: string | null;
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
  licenseNote: string | null;
  cacheStatus: string;
  cacheErrorMessage: string | null;
  mimeType: string | null;
  cachedAt: string | null;
  reviewNote: string | null;
  reviewedAt: string | null;
  reviewedBy: string | null;
}

interface BackendCandidateContentResponse {
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

interface BackendAiSummaryTaskResponse {
  id: number;
  status: string;
  inputSourceType: string;
  inputRefId: number | null;
  inputPreview: string | null;
  providerType: string;
  modelName: string;
  promptVersion: string;
  generatedSummary: string | null;
  generatedKeyPoints: string[];
  generatedImpactAnalysis: string | null;
  errorMessage: string | null;
  requestedBy: string | null;
  startedAt: string | null;
  finishedAt: string | null;
}

interface BackendCandidateDetailResponse {
  candidate: BackendCandidateResponse;
  rawItem: BackendRawNewsItemResponse | null;
  reportAssets: BackendReportAssetResponse[];
  content: BackendCandidateContentResponse | null;
  aiSummaryTask: BackendAiSummaryTaskResponse | null;
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
  attemptCount: number;
  maxAttempts: number;
  nextRetryAt: string | null;
  cancelRequested: boolean;
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
  scheduleEnabled: boolean;
  scheduleIntervalMinutes: number;
  nextRunAt: string | null;
  contentAccessPolicy: string;
  maxAgeHours: number | null;
  allowPdfDownload: boolean;
  allowFullText: boolean;
}

interface BackendIngestionRunResponse {
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

interface BackendIngestionAnomalyResponse {
  id: number;
  rawNewsItemId: number;
  title: string;
  sourceCode: string;
  sourceName: string;
  originalUrl: string;
  publishedAt: string | null;
  fetchedAt: string | null;
  issueType: string;
  severity: string;
  description: string;
}

interface BackendOperationLogResponse {
  id: number;
  module: string;
  actionType: string;
  targetType: string;
  targetId: number;
  targetTitle: string;
  status: string;
  operatorName: string;
  detail: string;
  createdAt: string;
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

export function fetchCandidateContent(
  id: number,
  mode: CandidateContentFetchMode = 'SNIPPET'
): Promise<CandidateContent> {
  return defaultClient.fetchCandidateContent(id, mode);
}

export function cacheCandidateReportAsset(candidateId: number, assetId: number): Promise<ReportAsset> {
  return defaultClient.cacheCandidateReportAsset(candidateId, assetId);
}

export function approveCandidateReportAsset(
  candidateId: number,
  assetId: number,
  reviewNote?: string
): Promise<ReportAsset> {
  return defaultClient.approveCandidateReportAsset(candidateId, assetId, reviewNote);
}

export function rejectCandidateReportAsset(
  candidateId: number,
  assetId: number,
  reviewNote?: string
): Promise<ReportAsset> {
  return defaultClient.rejectCandidateReportAsset(candidateId, assetId, reviewNote);
}

export function generateCandidateAiSummary(id: number): Promise<CandidateAiSummaryTask> {
  return defaultClient.generateCandidateAiSummary(id);
}

export function applyCandidateAiSummary(id: number, taskId: number): Promise<CandidateAiSummaryTask> {
  return defaultClient.applyCandidateAiSummary(id, taskId);
}

export function updateCandidate(id: number, input: AdminCandidateUpdateInput): Promise<AdminCandidate> {
  return defaultClient.updateCandidate(id, input);
}

export function publishCandidate(id: number, input?: AdminCandidatePublishInput): Promise<AdminCandidate> {
  return defaultClient.publishCandidate(id, input);
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

export function runIngestionSource(
  id: number,
  input?: AdminIngestionRunInput
): Promise<AdminIngestionRunResult> {
  return defaultClient.runIngestionSource(id, input);
}

export function listIngestionAnomalies(): Promise<AdminIngestionAnomaly[]> {
  return defaultClient.listIngestionAnomalies();
}

export function listOperationLogs(module?: string): Promise<AdminOperationLog[]> {
  return defaultClient.listOperationLogs(module);
}

export function resetAdminApiMock() {
  defaultClient.resetMockData?.();
}

function createMockAdminApiClient(): MutableAdminApiClient {
  let localCandidates = mockCandidates.map(cloneCandidate);
  let localDigests = mockDigests.map(cloneDigest);
  let localIngestionJobs = mockIngestionJobs.map(cloneIngestionJob);
  let localIngestionMetrics = { ...mockIngestionMetrics };
  let localIngestionSources = mockIngestionSources.map(cloneIngestionSource);
  let localIngestionAnomalies = mockIngestionAnomalies.map(cloneIngestionAnomaly);
  let localOperationLogs = mockOperationLogs.map(cloneOperationLog);
  let nextDigestId = 900;
  let nextAiSummaryTaskId = 1000;

  function resetMockData() {
    localCandidates = mockCandidates.map(cloneCandidate);
    localDigests = mockDigests.map(cloneDigest);
    localIngestionJobs = mockIngestionJobs.map(cloneIngestionJob);
    localIngestionMetrics = { ...mockIngestionMetrics };
    localIngestionSources = mockIngestionSources.map(cloneIngestionSource);
    localIngestionAnomalies = mockIngestionAnomalies.map(cloneIngestionAnomaly);
    localOperationLogs = mockOperationLogs.map(cloneOperationLog);
    nextDigestId = 900;
    nextAiSummaryTaskId = 1000;
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
            categoryName: categoryNameByCode[input.categoryCode] ?? input.categoryCode,
            categoryOverrideReason:
              input.categoryCode === candidate.suggestedCategoryCode ? '' : input.categoryOverrideReason ?? ''
          }
        : candidate
    );
    return findCandidate(id);
  }

  function fetchCandidateContent(id: number, mode: CandidateContentFetchMode) {
    const fetchedAt = new Date().toISOString();
    const content: CandidateContent = {
      candidateId: id,
      rawNewsItemId: findCandidate(id).rawNewsItemId,
      captureMode: mode,
      fetchStatus: 'SUCCESS',
      preview: '授权正文片段预览：AI 基建投资仍处扩张阶段，算力、电力和数据中心产业链将持续受益。',
      licensePolicy: mode === 'FULLTEXT' ? 'FULLTEXT_ALLOWED' : 'SNIPPET_ALLOWED',
      licenseNote: 'Mock 授权说明：仅用于 Admin 本地演示。',
      fetchedAt,
      errorMessage: null
    };
    localCandidates = localCandidates.map((candidate) =>
      candidate.id === id ? { ...candidate, content } : candidate
    );
    return content;
  }

  function generateCandidateAiSummary(id: number) {
    const candidate = findCandidate(id);
    const now = new Date().toISOString();
    const inputSourceType = candidate.content?.fetchStatus === 'SUCCESS' ? 'CONTENT_SNIPPET' : 'RSS_SUMMARY';
    const task: CandidateAiSummaryTask = {
      id: nextAiSummaryTaskId++,
      status: 'SUCCESS',
      inputSourceType,
      inputRefId: candidate.content?.fetchStatus === 'SUCCESS' ? candidate.content.rawNewsItemId : null,
      inputPreview: candidate.content?.preview ?? candidate.summary,
      providerType: 'MOCK',
      modelName: 'mock-v1',
      promptVersion: 'candidate-summary-v1',
      generatedSummary: `Mock AI 摘要：${candidate.title}。基于已授权输入生成，发布前需人工审核。`,
      generatedKeyPoints: [
        'Mock AI 要点：核对来源、时间和授权边界。',
        'Mock AI 要点：保留人工编辑空间，避免自动发布。',
        'Mock AI 要点：关注事实完整性和措辞合规性。'
      ],
      generatedImpactAnalysis: 'Mock AI 影响分析：该草稿仅用于 Admin 审核，发布前需确认摘要、要点和可能影响。',
      errorMessage: null,
      requestedBy: 'dev-admin',
      startedAt: now,
      finishedAt: now
    };
    localCandidates = localCandidates.map((item) =>
      item.id === id ? { ...item, aiSummaryTask: task } : item
    );
    return cloneAiSummaryTask(task);
  }

  function applyCandidateAiSummary(id: number, taskId: number) {
    const candidate = findCandidate(id);
    const task = candidate.aiSummaryTask;
    if (!task || task.id !== taskId || task.status !== 'SUCCESS') {
      throw new Error('AI summary task is not ready');
    }
    localCandidates = localCandidates.map((item) =>
      item.id === id
        ? {
            ...item,
            aiSummary: task.generatedSummary ?? item.aiSummary,
            keyPoints: [...task.generatedKeyPoints],
            impactAnalysis: task.generatedImpactAnalysis ?? item.impactAnalysis,
            aiSummaryTask: task
          }
        : item
    );
    return cloneAiSummaryTask(task);
  }

  function updateReportAsset(
    candidateId: number,
    assetId: number,
    updater: (asset: ReportAsset) => ReportAsset
  ) {
    localCandidates = localCandidates.map((candidate) => {
      if (candidate.id !== candidateId) {
        return candidate;
      }
      return {
        ...candidate,
        reportAssets: candidate.reportAssets.map((asset) => {
          if (asset.id !== assetId) {
            return asset;
          }
          return updater(asset);
        })
      };
    });
    const updatedAsset = localCandidates
      .find((candidate) => candidate.id === candidateId)
      ?.reportAssets.find((asset) => asset.id === assetId);
    if (!updatedAsset) {
      throw new Error('Report asset not found');
    }
    return { ...updatedAsset };
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
    fetchCandidateContent: async (id, mode) => fetchCandidateContent(id, mode),
    cacheCandidateReportAsset: async (candidateId, assetId) =>
      updateReportAsset(candidateId, assetId, (asset) => ({
        ...asset,
        fileSizeBytes: asset.fileSizeBytes ?? 2048,
        fileHash: 'mock-cached-pdf-hash',
        licenseNote: 'Mock 授权说明：公开 PDF 可由运营审核。',
        cacheStatus: 'SUCCESS',
        cacheErrorMessage: null,
        mimeType: 'application/pdf',
        cachedAt: new Date().toISOString()
      })),
    approveCandidateReportAsset: async (candidateId, assetId, reviewNote) =>
      updateReportAsset(candidateId, assetId, (asset) => ({
        ...asset,
        status: 'APPROVED',
        reviewNote: reviewNote ?? null,
        reviewedAt: new Date().toISOString(),
        reviewedBy: 'dev-admin'
      })),
    rejectCandidateReportAsset: async (candidateId, assetId, reviewNote) =>
      updateReportAsset(candidateId, assetId, (asset) => ({
        ...asset,
        status: 'REJECTED',
        reviewNote: reviewNote ?? null,
        reviewedAt: new Date().toISOString(),
          reviewedBy: 'dev-admin'
      })),
    generateCandidateAiSummary: async (id) => generateCandidateAiSummary(id),
    applyCandidateAiSummary: async (id, taskId) => applyCandidateAiSummary(id, taskId),
    updateCandidate: async (id, input) => updateCandidate(id, input),
    publishCandidate: async (id, input) => {
      if (input) {
        localCandidates = localCandidates.map((candidate) =>
          candidate.id === id
            ? {
                ...candidate,
                aiSummary: input.aiSummary ?? candidate.aiSummary,
                keyPoints: input.keyPoints ?? candidate.keyPoints,
                impactAnalysis: input.impactAnalysis ?? candidate.impactAnalysis
              }
            : candidate
        );
      }
      return updateStatus(id, 'PUBLISHED');
    },
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
    getTodayIngestionMetrics: async () => ({ ...localIngestionMetrics }),
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
    runIngestionSource: async (id, input) => {
      const source = localIngestionSources.find((item) => item.id === id);
      if (!source) {
        throw new Error('Ingestion source not found');
      }
      if (!source.enabled) {
        throw new Error('Ingestion source is disabled');
      }
      const fetchedCount = input?.pageSize ?? 5;
      const newCount = Math.max(fetchedCount - 1, 0);
      const duplicateCount = fetchedCount - newCount;
      const candidateCount = input?.generateCandidates === false ? 0 : newCount;
      const result: AdminIngestionRunResult = {
        jobId: 5000 + localIngestionJobs.length,
        sourceCode: source.code,
        providerType: source.providerType,
        status: 'SUCCESS',
        fetchedCount,
        newCount,
        duplicateCount,
        candidateCount,
        errorMessage: null
      };
      const now = new Date().toISOString();
      localIngestionJobs = [
        {
          id: result.jobId,
          sourceCode: result.sourceCode,
          triggerType: 'MANUAL',
          status: result.status,
          startedAt: now,
          finishedAt: now,
          fetchedCount,
          newCount,
          duplicateCount,
          candidateCount,
          errorMessage: null
        },
        ...localIngestionJobs
      ];
      localIngestionMetrics = {
        ...localIngestionMetrics,
        fetchedCount: localIngestionMetrics.fetchedCount + fetchedCount,
        candidateCount: localIngestionMetrics.candidateCount + candidateCount
      };
      return result;
    },
    listIngestionAnomalies: async () => localIngestionAnomalies.map(cloneIngestionAnomaly),
    listOperationLogs: async (module) => {
      if (!module) {
        return localOperationLogs.map(cloneOperationLog);
      }
      return localOperationLogs.filter((log) => log.module === module).map(cloneOperationLog);
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
    fetchCandidateContent: async (id, mode) => {
      const content = await request<BackendCandidateContentResponse>(
        `/api/admin/candidates/${id}/content/fetch`,
        {
          method: 'POST',
          body: JSON.stringify({ mode })
        }
      );
      return mapCandidateContent(content);
    },
    cacheCandidateReportAsset: async (candidateId, assetId) => {
      const asset = await request<BackendReportAssetResponse>(
        `/api/admin/candidates/${candidateId}/report-assets/${assetId}/cache`,
        {
          method: 'POST'
        }
      );
      return mapReportAsset(asset);
    },
    approveCandidateReportAsset: async (candidateId, assetId, reviewNote) => {
      const asset = await request<BackendReportAssetResponse>(
        `/api/admin/candidates/${candidateId}/report-assets/${assetId}/approve`,
        {
          method: 'POST',
          body: JSON.stringify(reviewNote ? { reviewNote } : {})
        }
      );
      return mapReportAsset(asset);
    },
    rejectCandidateReportAsset: async (candidateId, assetId, reviewNote) => {
      const asset = await request<BackendReportAssetResponse>(
        `/api/admin/candidates/${candidateId}/report-assets/${assetId}/reject`,
        {
          method: 'POST',
          body: JSON.stringify(reviewNote ? { reviewNote } : {})
        }
      );
      return mapReportAsset(asset);
    },
    generateCandidateAiSummary: async (id) => {
      const task = await request<BackendAiSummaryTaskResponse>(
        `/api/admin/candidates/${id}/ai-summary/generate`,
        {
          method: 'POST',
          body: JSON.stringify({
            inputSourceType: 'AUTO',
            providerType: 'MOCK',
            promptVersion: 'candidate-summary-v1'
          })
        }
      );
      return mapAiSummaryTask(task);
    },
    applyCandidateAiSummary: async (id, taskId) => {
      const task = await request<BackendAiSummaryTaskResponse>(
        `/api/admin/candidates/${id}/ai-summary/${taskId}/apply`,
        {
          method: 'POST'
        }
      );
      return mapAiSummaryTask(task);
    },
    updateCandidate: async (id, input) => {
      const candidate = await request<BackendCandidateResponse>(`/api/admin/candidates/${id}`, {
        method: 'PUT',
        body: JSON.stringify(input)
      });
      return mapCandidate(candidate);
    },
    publishCandidate: async (id, input = {}) => {
      const candidate = await request<BackendCandidateResponse>(`/api/admin/candidates/${id}/publish`, {
        method: 'POST',
        body: JSON.stringify({ publishNow: true, ...input })
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
    },
    runIngestionSource: async (id, input = {}) => {
      const result = await request<BackendIngestionRunResponse>(`/api/admin/ingestion/sources/${id}/run`, {
        method: 'POST',
        body: JSON.stringify(input)
      });
      return mapIngestionRunResult(result);
    },
    listIngestionAnomalies: async () => {
      const query = new URLSearchParams({
        page: '1',
        pageSize: '20'
      });
      const data = await request<BackendPageResponse<BackendIngestionAnomalyResponse>>(
        `/api/admin/ingestion/anomalies?${query.toString()}`
      );
      return data.items.map(mapIngestionAnomaly);
    },
    listOperationLogs: async (module) => {
      const query = new URLSearchParams();
      if (module) {
        query.set('module', module);
      }
      query.set('page', '1');
      query.set('pageSize', '20');
      const data = await request<BackendPageResponse<BackendOperationLogResponse>>(
        `/api/admin/operation-logs?${query.toString()}`
      );
      return data.items.map(mapOperationLog);
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
    keyPoints: [],
    impactAnalysis: '',
    categoryCode: candidate.categoryCode,
    categoryName: categoryNameByCode[candidate.categoryCode] ?? candidate.categoryCode,
    suggestedCategoryCode: candidate.suggestedCategoryCode ?? candidate.categoryCode,
    suggestedCategoryName:
      categoryNameByCode[candidate.suggestedCategoryCode ?? candidate.categoryCode] ??
      candidate.suggestedCategoryCode ??
      candidate.categoryCode,
    classificationConfidence: candidate.classificationConfidence ?? 0.5,
    classificationRule: candidate.classificationRule ?? 'LEGACY_BACKFILL',
    categoryOverrideReason: candidate.categoryOverrideReason ?? '',
    sourceName: candidate.sourceName,
    tagNames: candidate.tagNames ?? [],
    originalUrl: candidate.originalUrl,
    publishedAt: candidate.publishedAt ?? '',
    fetchedAt: candidate.createdAt,
    status: candidate.status,
    content: null,
    aiSummaryTask: null,
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
    content: detail.content ? mapCandidateContent(detail.content) : candidate.content,
    aiSummaryTask: detail.aiSummaryTask ? mapAiSummaryTask(detail.aiSummaryTask) : null,
    reportAssets: detail.reportAssets.map(mapReportAsset)
  };
}

function mapCandidateContent(content: BackendCandidateContentResponse): CandidateContent {
  return {
    candidateId: content.candidateId,
    rawNewsItemId: content.rawNewsItemId,
    captureMode: content.captureMode,
    fetchStatus: content.fetchStatus,
    preview: content.preview,
    licensePolicy: content.licensePolicy,
    licenseNote: content.licenseNote,
    fetchedAt: content.fetchedAt,
    errorMessage: content.errorMessage
  };
}

function mapReportAsset(asset: BackendReportAssetResponse): ReportAsset {
  return {
    id: asset.id,
    title: asset.title,
    originalUrl: asset.originalUrl,
    fileName: asset.fileName,
    fileSizeBytes: asset.fileSizeBytes,
    fileHash: asset.fileHash,
    licensePolicy: asset.licensePolicy,
    status: asset.status,
    licenseNote: asset.licenseNote,
    cacheStatus: asset.cacheStatus,
    cacheErrorMessage: asset.cacheErrorMessage,
    mimeType: asset.mimeType,
    cachedAt: asset.cachedAt,
    reviewNote: asset.reviewNote,
    reviewedAt: asset.reviewedAt,
    reviewedBy: asset.reviewedBy
  };
}

function mapAiSummaryTask(task: BackendAiSummaryTaskResponse): CandidateAiSummaryTask {
  return {
    id: task.id,
    status: task.status,
    inputSourceType: task.inputSourceType,
    inputRefId: task.inputRefId,
    inputPreview: task.inputPreview,
    providerType: task.providerType,
    modelName: task.modelName,
    promptVersion: task.promptVersion,
    generatedSummary: task.generatedSummary,
    generatedKeyPoints: task.generatedKeyPoints ?? [],
    generatedImpactAnalysis: task.generatedImpactAnalysis,
    errorMessage: task.errorMessage,
    requestedBy: task.requestedBy,
    startedAt: task.startedAt,
    finishedAt: task.finishedAt
  };
}

function cloneCandidate(candidate: AdminCandidate): AdminCandidate {
  return {
    ...candidate,
    tagNames: [...(candidate.tagNames ?? [])],
    keyPoints: [...(candidate.keyPoints ?? [])],
    content: candidate.content ? { ...candidate.content } : null,
    aiSummaryTask: candidate.aiSummaryTask ? cloneAiSummaryTask(candidate.aiSummaryTask) : null,
    reportAssets: candidate.reportAssets.map((asset) => ({ ...asset }))
  };
}

function cloneAiSummaryTask(task: CandidateAiSummaryTask): CandidateAiSummaryTask {
  return {
    ...task,
    generatedKeyPoints: [...task.generatedKeyPoints]
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

function mapIngestionRunResult(result: BackendIngestionRunResponse): AdminIngestionRunResult {
  return { ...result };
}

function mapIngestionAnomaly(anomaly: BackendIngestionAnomalyResponse): AdminIngestionAnomaly {
  return { ...anomaly };
}

function mapOperationLog(log: BackendOperationLogResponse): AdminOperationLog {
  return { ...log };
}

function cloneIngestionJob(job: AdminIngestionJob): AdminIngestionJob {
  return { ...job };
}

function cloneIngestionSource(source: AdminIngestionSource): AdminIngestionSource {
  return { ...source };
}

function cloneIngestionAnomaly(anomaly: AdminIngestionAnomaly): AdminIngestionAnomaly {
  return { ...anomaly };
}

function cloneOperationLog(log: AdminOperationLog): AdminOperationLog {
  return { ...log };
}
