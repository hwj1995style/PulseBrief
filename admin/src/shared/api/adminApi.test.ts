import { afterEach, describe, expect, it, vi } from 'vitest';
import { createAdminApiClient } from './adminApi';

const apiBaseUrl = 'http://localhost:8080';
const adminToken = 'dev-admin-token';

function mockFetchResponse(data: unknown) {
  return Promise.resolve({
    ok: true,
    status: 200,
    json: () => Promise.resolve(data)
  } as Response);
}

describe('adminApi HTTP client', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('maps backend candidate list into AdminCandidate records', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(
      await mockFetchResponse({
        code: 'OK',
        data: {
          items: [
            {
              id: 11,
              rawNewsItemId: 7,
              title: '美联储官员释放谨慎信号',
              summary: '市场重新评估降息节奏。',
              categoryCode: 'macro',
              sourceName: 'Federal Reserve',
              originalUrl: 'https://example.com/fed',
              publishedAt: '2026-06-10T08:00:00+08:00',
              status: 'PENDING_REVIEW',
              createdAt: '2026-06-10T08:30:00+08:00',
              publishedArticleId: null,
              reviewNote: null
            }
          ],
          page: 1,
          pageSize: 50,
          total: 1,
          hasMore: false
        },
        traceId: 'test-trace'
      })
    );

    const client = createAdminApiClient({ apiBaseUrl, adminToken });
    const candidates = await client.listCandidates('PENDING_REVIEW');

    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/admin/candidates?status=PENDING_REVIEW&page=1&pageSize=50',
      expect.objectContaining({
        headers: expect.objectContaining({
          Authorization: 'Bearer dev-admin-token'
        })
      })
    );
    expect(candidates).toEqual([
      expect.objectContaining({
        id: 11,
        rawNewsItemId: 7,
        title: '美联储官员释放谨慎信号',
        categoryCode: 'macro',
        categoryName: '宏观政策',
        aiSummary: '市场重新评估降息节奏。',
        fetchedAt: '2026-06-10T08:30:00+08:00',
        reportAssets: []
      })
    ]);
  });

  it('hydrates candidate detail with raw item metadata and report assets', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(
      await mockFetchResponse({
        code: 'OK',
        data: {
          candidate: {
            id: 12,
            rawNewsItemId: 8,
            title: '高盛：AI 基建投资仍将持续',
            summary: '算力、电力和数据中心产业链将持续受益。',
            categoryCode: 'investment_view',
            sourceName: 'Goldman Sachs Research',
            originalUrl: 'https://example.com/goldman',
            publishedAt: '2026-06-10T08:00:00+08:00',
            status: 'PENDING_REVIEW',
            createdAt: '2026-06-10T08:40:00+08:00',
            publishedArticleId: null,
            reviewNote: null
          },
          rawItem: {
            id: 8,
            sourceCode: 'goldman',
            providerItemId: 'g-1',
            title: '高盛：AI 基建投资仍将持续',
            summary: '原始来源摘要。',
            sourceName: 'Goldman Sachs Research',
            originalUrl: 'https://example.com/goldman',
            publishedAt: '2026-06-10T08:00:00+08:00',
            fetchedAt: '2026-06-10T08:18:00+08:00',
            language: 'zh',
            country: 'US',
            status: 'CANDIDATE_GENERATED'
          },
          reportAssets: [
            {
              id: 21,
              title: 'AI infrastructure outlook',
              originalUrl: 'https://example.com/report.pdf',
              fileName: 'ai-infrastructure-outlook.pdf',
              fileSizeBytes: 1024,
              fileHash: 'abc',
              licensePolicy: 'PDF_ALLOWED',
              status: 'PENDING_REVIEW'
            }
          ],
          duplicateHints: [],
          availableActions: ['publish', 'reject']
        },
        traceId: 'test-trace'
      })
    );

    const client = createAdminApiClient({ apiBaseUrl, adminToken });
    const detail = await client.getCandidate(12);

    expect(detail).toEqual(
      expect.objectContaining({
        id: 12,
        categoryName: '投行观点',
        aiSummary: '原始来源摘要。',
        fetchedAt: '2026-06-10T08:18:00+08:00',
        reportAssets: [
          expect.objectContaining({
            id: 21,
            fileName: 'ai-infrastructure-outlook.pdf',
            licensePolicy: 'PDF_ALLOWED'
          })
        ]
      })
    );
  });

  it('uses publish and reject action endpoints with Authorization header', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch');
    fetchMock
      .mockResolvedValueOnce(
        await mockFetchResponse({
          code: 'OK',
          data: {
            id: 13,
            rawNewsItemId: 9,
            title: '候选一',
            summary: '摘要',
            categoryCode: 'ai',
            sourceName: 'Tech Brief',
            originalUrl: 'https://example.com/ai',
            publishedAt: null,
            status: 'PUBLISHED',
            createdAt: '2026-06-10T09:00:00+08:00',
            publishedArticleId: 101,
            reviewNote: null
          }
        })
      )
      .mockResolvedValueOnce(
        await mockFetchResponse({
          code: 'OK',
          data: {
            id: 14,
            rawNewsItemId: 10,
            title: '候选二',
            summary: '摘要',
            categoryCode: 'macro',
            sourceName: 'Macro Brief',
            originalUrl: 'https://example.com/macro',
            publishedAt: null,
            status: 'REJECTED',
            createdAt: '2026-06-10T09:00:00+08:00',
            publishedArticleId: null,
            reviewNote: '重复内容'
          }
        })
      );

    const client = createAdminApiClient({ apiBaseUrl, adminToken });
    await client.publishCandidate(13);
    await client.rejectCandidate(14, '重复内容');

    expect(fetchMock).toHaveBeenNthCalledWith(
      1,
      'http://localhost:8080/api/admin/candidates/13/publish',
      expect.objectContaining({
        method: 'POST',
        headers: expect.objectContaining({
          Authorization: 'Bearer dev-admin-token',
          'Content-Type': 'application/json'
        })
      })
    );
    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      'http://localhost:8080/api/admin/candidates/14/reject',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ reviewNote: '重复内容' })
      })
    );
  });
});
