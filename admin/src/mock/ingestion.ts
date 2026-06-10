import type { AdminIngestionJob, AdminIngestionMetrics, AdminIngestionSource } from '../shared/types/ingestion';

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
