import type {
  AdminIngestionJob,
  AdminIngestionMetrics,
  AdminIngestionSource,
  AdminIngestionAnomaly,
  AdminOperationLog
} from '../shared/types/ingestion';

export const mockIngestionMetrics: AdminIngestionMetrics = {
  fetchedCount: 42,
  candidateCount: 18,
  publishedCount: 6,
  failedCount: 1
};

export const mockIngestionJobs: AdminIngestionJob[] = [
  {
    id: 1003,
    sourceCode: 'fixture-global',
    triggerType: 'SCHEDULED',
    status: 'FAILED',
    startedAt: '2026-06-10T08:00:00',
    finishedAt: '2026-06-10T08:01:00',
    fetchedCount: 0,
    newCount: 0,
    duplicateCount: 0,
    candidateCount: 0,
    errorMessage: 'Provider timeout'
  },
  {
    id: 1002,
    sourceCode: 'fixture-markets',
    triggerType: 'MANUAL',
    status: 'SUCCESS',
    startedAt: '2026-06-10T07:30:00',
    finishedAt: '2026-06-10T07:31:00',
    fetchedCount: 28,
    newCount: 17,
    duplicateCount: 11,
    candidateCount: 9,
    errorMessage: null
  },
  {
    id: 1001,
    sourceCode: 'fixture-research',
    triggerType: 'SCHEDULED',
    status: 'SUCCESS',
    startedAt: '2026-06-10T06:30:00',
    finishedAt: '2026-06-10T06:32:00',
    fetchedCount: 14,
    newCount: 9,
    duplicateCount: 5,
    candidateCount: 6,
    errorMessage: null
  }
];

export const mockIngestionSources: AdminIngestionSource[] = [
  {
    id: 1,
    code: 'fixture-global',
    name: 'Fixture Global',
    providerType: 'FIXTURE',
    defaultCategoryCode: 'global',
    enabled: true,
    contentAccessPolicy: 'SUMMARY_ONLY',
    maxAgeHours: 24,
    allowPdfDownload: false,
    allowFullText: false
  },
  {
    id: 2,
    code: 'fixture-research',
    name: 'Fixture Research',
    providerType: 'FIXTURE',
    defaultCategoryCode: 'investment_view',
    enabled: true,
    contentAccessPolicy: 'PDF_ALLOWED',
    maxAgeHours: 72,
    allowPdfDownload: true,
    allowFullText: false
  }
];

export const mockOperationLogs: AdminOperationLog[] = [
  {
    id: 2003,
    module: 'PUBLISH',
    actionType: 'PUBLISH_ARTICLE',
    targetType: 'ARTICLE',
    targetId: 501,
    targetTitle: '高盛：AI 基建投资仍将持续',
    status: 'SUCCESS',
    operatorName: 'dev-admin',
    detail: '候选资讯审核通过，已发布到 APP 文章流。',
    createdAt: '2026-06-10T09:10:00'
  },
  {
    id: 2002,
    module: 'PUBLISH',
    actionType: 'PUBLISH_DIGEST',
    targetType: 'DIGEST',
    targetId: 31,
    targetTitle: '今日全球早报',
    status: 'SUCCESS',
    operatorName: 'dev-admin',
    detail: '每日早报已发布，移动端简报入口可见。',
    createdAt: '2026-06-10T08:30:00'
  },
  {
    id: 2001,
    module: 'PUBLISH',
    actionType: 'OFFLINE_DIGEST',
    targetType: 'DIGEST',
    targetId: 30,
    targetTitle: '昨日晚间复盘',
    status: 'SUCCESS',
    operatorName: 'dev-admin',
    detail: '旧版简报下线，用户端不再展示。',
    createdAt: '2026-06-10T07:50:00'
  }
];

export const mockIngestionAnomalies: AdminIngestionAnomaly[] = [
  {
    id: 3002,
    rawNewsItemId: 3002,
    title: '缺链接资讯样本',
    sourceCode: 'fixture-global',
    sourceName: 'Fixture Global',
    originalUrl: '',
    publishedAt: '2026-06-10T08:00:00',
    fetchedAt: '2026-06-10T08:10:00',
    issueType: 'MISSING_ORIGINAL_URL',
    severity: 'HIGH',
    description: '原始资讯缺少原文链接，无法满足可追溯要求'
  },
  {
    id: 3001,
    rawNewsItemId: 3001,
    title: '未来发布时间样本',
    sourceCode: 'fixture-markets',
    sourceName: 'Fixture Markets',
    originalUrl: 'https://example.com/future-market',
    publishedAt: '2026-06-10T16:00:00',
    fetchedAt: '2026-06-10T08:20:00',
    issueType: 'PUBLISHED_AT_IN_FUTURE',
    severity: 'HIGH',
    description: '原始资讯发布时间晚于当前时间，可能来自来源时区或解析错误'
  }
];
