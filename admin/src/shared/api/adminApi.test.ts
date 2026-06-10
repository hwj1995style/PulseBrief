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

  it('sends candidate update requests with editable review fields', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(
      await mockFetchResponse({
        code: 'OK',
        data: {
          id: 15,
          rawNewsItemId: 11,
          title: '运营修订后的候选标题',
          summary: '运营修订后的候选摘要',
          categoryCode: 'ai',
          sourceName: 'Updated Source',
          tagNames: ['AI 基建', '算力'],
          originalUrl: 'https://example.com/updated',
          publishedAt: null,
          status: 'PENDING_REVIEW',
          createdAt: '2026-06-10T09:00:00+08:00',
          publishedArticleId: null,
          reviewNote: null
        }
      })
    );

    const client = createAdminApiClient({ apiBaseUrl, adminToken });
    const updated = await client.updateCandidate(15, {
      title: '运营修订后的候选标题',
      summary: '运营修订后的候选摘要',
      categoryCode: 'ai',
      sourceName: 'Updated Source',
      tagNames: ['AI 基建', '算力']
    });

    expect(updated).toEqual(
      expect.objectContaining({
        id: 15,
        title: '运营修订后的候选标题',
        categoryCode: 'ai',
        categoryName: 'AI 前沿',
        sourceName: 'Updated Source',
        tagNames: ['AI 基建', '算力']
      })
    );
    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/admin/candidates/15',
      expect.objectContaining({
        method: 'PUT',
        headers: expect.objectContaining({
          Authorization: 'Bearer dev-admin-token',
          'Content-Type': 'application/json'
        }),
        body: JSON.stringify({
          title: '运营修订后的候选标题',
          summary: '运营修订后的候选摘要',
          categoryCode: 'ai',
          sourceName: 'Updated Source',
          tagNames: ['AI 基建', '算力']
        })
      })
    );
  });

  it('maps digest APIs and sends create and publish requests', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch');
    fetchMock
      .mockResolvedValueOnce(
        await mockFetchResponse({
          code: 'OK',
          data: {
            items: [
              {
                id: 31,
                digestDate: '2026-06-10',
                digestType: 'MORNING',
                categoryCode: 'global',
                title: '今日全球早报',
                summary: '精选 10 条重点资讯',
                content: '英伟达 Blackwell Ultra 发布',
                audioText: '欢迎收听今日全球早报。',
                status: 'DRAFT',
                publishTime: null,
                articleCount: 1,
                articles: [
                  {
                    articleId: 501,
                    sortNo: 1,
                    highlightText: '英伟达 Blackwell Ultra 发布',
                    title: '英伟达推出新一代 AI 芯片',
                    sourceName: 'Tech Brief'
                  }
                ],
                availableActions: ['EDIT', 'PUBLISH']
              }
            ],
            page: 1,
            pageSize: 50,
            total: 1,
            hasMore: false
          }
        })
      )
      .mockResolvedValueOnce(
        await mockFetchResponse({
          code: 'OK',
          data: {
            items: [
              {
                id: 501,
                title: '英伟达推出新一代 AI 芯片',
                sourceName: 'Tech Brief',
                publishTime: '2小时前',
                categoryName: 'AI 前沿',
                summary: 'Blackwell Ultra 性能提升显著。',
                isHot: true,
                isBreaking: false
              }
            ],
            page: 1,
            pageSize: 50,
            total: 1,
            hasMore: false
          }
        })
      )
      .mockResolvedValueOnce(
        await mockFetchResponse({
          code: 'OK',
          data: {
            id: 32,
            digestDate: '2026-06-10',
            digestType: 'MORNING',
            categoryCode: 'global',
            title: '今日全球早报',
            summary: '精选 1 条重点资讯',
            content: '英伟达 Blackwell Ultra 发布',
            audioText: '欢迎收听今日全球早报。',
            status: 'DRAFT',
            publishTime: null,
            articleCount: 1,
            articles: [],
            availableActions: ['EDIT', 'PUBLISH']
          }
        })
      )
      .mockResolvedValueOnce(
        await mockFetchResponse({
          code: 'OK',
          data: {
            id: 32,
            digestDate: '2026-06-10',
            digestType: 'MORNING',
            categoryCode: 'global',
            title: '今日全球早报',
            summary: '精选 1 条重点资讯',
            content: '英伟达 Blackwell Ultra 发布',
            audioText: '欢迎收听今日全球早报。',
            status: 'PUBLISHED',
            publishTime: '2026-06-10T08:30:00+08:00',
            articleCount: 1,
            articles: [],
            availableActions: ['OFFLINE']
          }
        })
      );

    const client = createAdminApiClient({ apiBaseUrl, adminToken });
    const digests = await client.listDigests('DRAFT');
    const articles = await client.listDigestArticleCandidates('英伟达');
    const created = await client.createDigest({
      digestDate: '2026-06-10',
      digestType: 'MORNING',
      categoryCode: 'global',
      title: '今日全球早报',
      summary: '精选 1 条重点资讯',
      content: '英伟达 Blackwell Ultra 发布',
      audioText: '欢迎收听今日全球早报。',
      articles: [{ articleId: 501, sortNo: 1, highlightText: '英伟达 Blackwell Ultra 发布' }]
    });
    const published = await client.publishDigest(created.id);

    expect(digests[0]).toEqual(expect.objectContaining({ id: 31, status: 'DRAFT', articleCount: 1 }));
    expect(articles[0]).toEqual(expect.objectContaining({ id: 501, categoryName: 'AI 前沿' }));
    expect(published.status).toBe('PUBLISHED');
    expect(fetchMock).toHaveBeenNthCalledWith(
      1,
      'http://localhost:8080/api/admin/digests?status=DRAFT&page=1&pageSize=50',
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: 'Bearer dev-admin-token' })
      })
    );
    expect(fetchMock).toHaveBeenNthCalledWith(
      3,
      'http://localhost:8080/api/admin/digests',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          digestDate: '2026-06-10',
          digestType: 'MORNING',
          categoryCode: 'global',
          title: '今日全球早报',
          summary: '精选 1 条重点资讯',
          content: '英伟达 Blackwell Ultra 发布',
          audioText: '欢迎收听今日全球早报。',
          articles: [{ articleId: 501, sortNo: 1, highlightText: '英伟达 Blackwell Ultra 发布' }]
        })
      })
    );
    expect(fetchMock).toHaveBeenNthCalledWith(
      4,
      'http://localhost:8080/api/admin/digests/32/publish',
      expect.objectContaining({ method: 'POST' })
    );
  });

  it('sends digest update and offline requests', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch');
    fetchMock
      .mockResolvedValueOnce(
        await mockFetchResponse({
          code: 'OK',
          data: {
            id: 41,
            digestDate: '2026-06-10',
            digestType: 'MORNING',
            categoryCode: 'finance',
            title: '更新后的市场早报',
            summary: '更新后的摘要',
            content: '更新后的热点',
            audioText: '更新后的播报文案。',
            status: 'DRAFT',
            publishTime: null,
            articleCount: 1,
            articles: [],
            availableActions: ['EDIT', 'PUBLISH']
          }
        })
      )
      .mockResolvedValueOnce(
        await mockFetchResponse({
          code: 'OK',
          data: {
            id: 42,
            digestDate: '2026-06-10',
            digestType: 'MORNING',
            categoryCode: 'global',
            title: '今日全球早报',
            summary: '摘要',
            content: '热点',
            audioText: '播报文案',
            status: 'OFFLINE',
            publishTime: '2026-06-10T08:30:00+08:00',
            articleCount: 1,
            articles: [],
            availableActions: []
          }
        })
      );

    const client = createAdminApiClient({ apiBaseUrl, adminToken });
    const updated = await client.updateDigest(41, {
      digestDate: '2026-06-10',
      digestType: 'MORNING',
      categoryCode: 'finance',
      title: '更新后的市场早报',
      summary: '更新后的摘要',
      content: '更新后的热点',
      audioText: '更新后的播报文案。',
      articles: [{ articleId: 501, sortNo: 1, highlightText: '更新后的热点' }]
    });
    const offlined = await client.offlineDigest(42);

    expect(updated).toEqual(expect.objectContaining({ id: 41, title: '更新后的市场早报' }));
    expect(offlined.status).toBe('OFFLINE');
    expect(fetchMock).toHaveBeenNthCalledWith(
      1,
      'http://localhost:8080/api/admin/digests/41',
      expect.objectContaining({
        method: 'PUT',
        body: JSON.stringify({
          digestDate: '2026-06-10',
          digestType: 'MORNING',
          categoryCode: 'finance',
          title: '更新后的市场早报',
          summary: '更新后的摘要',
          content: '更新后的热点',
          audioText: '更新后的播报文案。',
          articles: [{ articleId: 501, sortNo: 1, highlightText: '更新后的热点' }]
        })
      })
    );
    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      'http://localhost:8080/api/admin/digests/42/offline',
      expect.objectContaining({ method: 'POST' })
    );
  });
});
