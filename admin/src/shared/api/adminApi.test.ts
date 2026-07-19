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
              status: 'PENDING_REVIEW',
              licenseNote: '公开 PDF 授权说明',
              cacheStatus: 'SUCCESS',
              cacheErrorMessage: null,
              mimeType: 'application/pdf',
              cachedAt: '2026-06-10T08:22:00+08:00',
              reviewNote: '可发布',
              reviewedAt: '2026-06-10T08:23:00+08:00',
              reviewedBy: 'dev-admin'
            }
          ],
          content: {
            candidateId: 12,
            rawNewsItemId: 8,
            captureMode: 'SNIPPET',
            fetchStatus: 'SUCCESS',
            preview: '授权正文片段显示 AI 基建投资仍在扩张。',
            licensePolicy: 'SNIPPET_ALLOWED',
            licenseNote: '公开网页允许展示短片段。',
            fetchedAt: '2026-06-10T08:20:00+08:00',
            errorMessage: null
          },
          aiSummaryTask: {
            id: 701,
            status: 'SUCCESS',
            inputSourceType: 'CONTENT_SNIPPET',
            inputRefId: 33,
            inputPreview: '授权正文片段显示 AI 基建投资仍在扩张。',
            providerType: 'MOCK',
            modelName: 'mock-v1',
            promptVersion: 'candidate-summary-v1',
            generatedSummary: 'Mock AI 摘要：高盛认为 AI 基建投资仍在扩张。',
            generatedKeyPoints: ['Mock AI 要点：关注算力。', 'Mock AI 要点：关注电力。'],
            generatedImpactAnalysis: 'Mock AI 影响分析：发布前需人工审核。',
            errorMessage: null,
            requestedBy: 'dev-admin',
            startedAt: '2026-06-10T08:21:00+08:00',
            finishedAt: '2026-06-10T08:22:00+08:00'
          },
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
            licensePolicy: 'PDF_ALLOWED',
            fileSizeBytes: 1024,
            fileHash: 'abc',
            licenseNote: '公开 PDF 授权说明',
            cacheStatus: 'SUCCESS',
            cacheErrorMessage: null,
            mimeType: 'application/pdf',
            cachedAt: '2026-06-10T08:22:00+08:00',
            reviewNote: '可发布',
            reviewedAt: '2026-06-10T08:23:00+08:00',
            reviewedBy: 'dev-admin'
          })
        ],
        content: expect.objectContaining({
          captureMode: 'SNIPPET',
          fetchStatus: 'SUCCESS',
          preview: '授权正文片段显示 AI 基建投资仍在扩张。',
          licensePolicy: 'SNIPPET_ALLOWED'
        }),
        aiSummaryTask: expect.objectContaining({
          id: 701,
          status: 'SUCCESS',
          inputSourceType: 'CONTENT_SNIPPET',
          providerType: 'MOCK',
          generatedSummary: 'Mock AI 摘要：高盛认为 AI 基建投资仍在扩张。',
          generatedKeyPoints: ['Mock AI 要点：关注算力。', 'Mock AI 要点：关注电力。'],
          generatedImpactAnalysis: 'Mock AI 影响分析：发布前需人工审核。'
        })
      })
    );
  });

  it('uses AI summary generate and apply endpoints before publishing adopted draft', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch');
    const taskResponse = {
      id: 801,
      status: 'SUCCESS',
      inputSourceType: 'RSS_SUMMARY',
      inputRefId: null,
      inputPreview: '市场重新评估 AI 基建投资节奏。',
      providerType: 'MOCK',
      modelName: 'mock-v1',
      promptVersion: 'candidate-summary-v1',
      generatedSummary: 'Mock AI 摘要：市场重新评估 AI 基建投资节奏。',
      generatedKeyPoints: ['Mock AI 要点：关注算力投资。'],
      generatedImpactAnalysis: 'Mock AI 影响分析：需人工审核后发布。',
      errorMessage: null,
      requestedBy: 'dev-admin',
      startedAt: '2026-06-10T08:21:00+08:00',
      finishedAt: '2026-06-10T08:22:00+08:00'
    };
    fetchMock
      .mockResolvedValueOnce(await mockFetchResponse({ code: 'OK', data: taskResponse }))
      .mockResolvedValueOnce(await mockFetchResponse({ code: 'OK', data: taskResponse }))
      .mockResolvedValueOnce(
        await mockFetchResponse({
          code: 'OK',
          data: {
            id: 12,
            rawNewsItemId: 8,
            title: '高盛：AI 基建投资仍将持续',
            summary: '来源摘要。',
            categoryCode: 'investment_view',
            sourceName: 'Goldman Sachs Research',
            originalUrl: 'https://example.com/goldman',
            publishedAt: '2026-06-10T08:00:00+08:00',
            status: 'PUBLISHED',
            createdAt: '2026-06-10T08:40:00+08:00',
            publishedArticleId: 55,
            reviewNote: null
          }
        })
      );

    const client = createAdminApiClient({ apiBaseUrl, adminToken });
    const generated = await client.generateCandidateAiSummary(12);
    const applied = await client.applyCandidateAiSummary(12, generated.id);
    await client.publishCandidate(12, {
      aiSummary: applied.generatedSummary ?? '',
      keyPoints: applied.generatedKeyPoints,
      impactAnalysis: applied.generatedImpactAnalysis ?? ''
    });

    expect(fetchMock).toHaveBeenNthCalledWith(
      1,
      'http://localhost:8080/api/admin/candidates/12/ai-summary/generate',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          inputSourceType: 'AUTO',
          providerType: 'MOCK',
          promptVersion: 'candidate-summary-v1'
        })
      })
    );
    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      'http://localhost:8080/api/admin/candidates/12/ai-summary/801/apply',
      expect.objectContaining({ method: 'POST' })
    );
    expect(fetchMock).toHaveBeenNthCalledWith(
      3,
      'http://localhost:8080/api/admin/candidates/12/publish',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          publishNow: true,
          aiSummary: 'Mock AI 摘要：市场重新评估 AI 基建投资节奏。',
          keyPoints: ['Mock AI 要点：关注算力投资。'],
          impactAnalysis: 'Mock AI 影响分析：需人工审核后发布。'
        })
      })
    );
  });

  it('uses report asset cache and review endpoints', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch');
    const reportAssetResponse = {
      id: 21,
      title: 'AI infrastructure outlook',
      originalUrl: 'https://example.com/report.pdf',
      fileName: 'ai-infrastructure-outlook.pdf',
      fileSizeBytes: 2048,
      fileHash: 'cached-hash',
      licensePolicy: 'PDF_ALLOWED',
      status: 'PENDING_REVIEW',
      licenseNote: '公开 PDF 授权说明',
      cacheStatus: 'SUCCESS',
      cacheErrorMessage: null,
      mimeType: 'application/pdf',
      cachedAt: '2026-06-10T08:22:00+08:00',
      reviewNote: null,
      reviewedAt: null,
      reviewedBy: null
    };
    fetchMock
      .mockResolvedValueOnce(await mockFetchResponse({ code: 'OK', data: reportAssetResponse }))
      .mockResolvedValueOnce(
        await mockFetchResponse({
          code: 'OK',
          data: {
            ...reportAssetResponse,
            status: 'APPROVED',
            reviewNote: '授权公开 PDF 可发布',
            reviewedAt: '2026-06-10T08:24:00+08:00',
            reviewedBy: 'dev-admin'
          }
        })
      )
      .mockResolvedValueOnce(
        await mockFetchResponse({
          code: 'OK',
          data: {
            ...reportAssetResponse,
            status: 'REJECTED',
            reviewNote: '只保留原文链接',
            reviewedAt: '2026-06-10T08:25:00+08:00',
            reviewedBy: 'dev-admin'
          }
        })
      );

    const client = createAdminApiClient({ apiBaseUrl, adminToken });
    const cached = await client.cacheCandidateReportAsset(12, 21);
    const approved = await client.approveCandidateReportAsset(12, 21, '授权公开 PDF 可发布');
    const rejected = await client.rejectCandidateReportAsset(12, 21, '只保留原文链接');

    expect(cached.cacheStatus).toBe('SUCCESS');
    expect(approved.status).toBe('APPROVED');
    expect(rejected.status).toBe('REJECTED');
    expect(fetchMock).toHaveBeenNthCalledWith(
      1,
      'http://localhost:8080/api/admin/candidates/12/report-assets/21/cache',
      expect.objectContaining({ method: 'POST' })
    );
    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      'http://localhost:8080/api/admin/candidates/12/report-assets/21/approve',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ reviewNote: '授权公开 PDF 可发布' })
      })
    );
    expect(fetchMock).toHaveBeenNthCalledWith(
      3,
      'http://localhost:8080/api/admin/candidates/12/report-assets/21/reject',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ reviewNote: '只保留原文链接' })
      })
    );
  });

  it('requests authorized candidate content fetch and maps result', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(
      await mockFetchResponse({
        code: 'OK',
        data: {
          candidateId: 12,
          rawNewsItemId: 8,
          captureMode: 'SNIPPET',
          fetchStatus: 'SUCCESS',
          preview: '授权正文片段来自真实网页。',
          licensePolicy: 'SNIPPET_ALLOWED',
          licenseNote: '公开网页允许展示短片段。',
          fetchedAt: '2026-06-10T08:25:00+08:00',
          errorMessage: null
        }
      })
    );

    const client = createAdminApiClient({ apiBaseUrl, adminToken });
    const content = await client.fetchCandidateContent(12, 'SNIPPET');

    expect(content).toEqual(
      expect.objectContaining({
        candidateId: 12,
        captureMode: 'SNIPPET',
        fetchStatus: 'SUCCESS',
        preview: '授权正文片段来自真实网页。'
      })
    );
    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/admin/candidates/12/content/fetch',
      expect.objectContaining({
        method: 'POST',
        headers: expect.objectContaining({
          Authorization: 'Bearer dev-admin-token',
          'Content-Type': 'application/json'
        }),
        body: JSON.stringify({ mode: 'SNIPPET' })
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

  it('maps ingestion jobs, metrics and sources APIs', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch');
    fetchMock
      .mockResolvedValueOnce(
        await mockFetchResponse({
          code: 'OK',
          data: {
            items: [
              {
                id: 71,
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
              }
            ],
            page: 1,
            pageSize: 20,
            total: 1,
            hasMore: false
          }
        })
      )
      .mockResolvedValueOnce(
        await mockFetchResponse({
          code: 'OK',
          data: {
            fetchedCount: 42,
            candidateCount: 18,
            publishedCount: 6,
            failedCount: 1
          }
        })
      )
      .mockResolvedValueOnce(
        await mockFetchResponse({
          code: 'OK',
          data: [
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
            }
          ]
        })
      );

    const client = createAdminApiClient({ apiBaseUrl, adminToken });
    const jobs = await client.listIngestionJobs('FAILED');
    const metrics = await client.getTodayIngestionMetrics();
    const sources = await client.listIngestionSources();

    expect(jobs[0]).toEqual(
      expect.objectContaining({
        id: 71,
        sourceCode: 'fixture-global',
        status: 'FAILED',
        errorMessage: 'Provider timeout'
      })
    );
    expect(metrics).toEqual({
      fetchedCount: 42,
      candidateCount: 18,
      publishedCount: 6,
      failedCount: 1
    });
    expect(sources[0]).toEqual(
      expect.objectContaining({
        code: 'fixture-global',
        enabled: true,
        maxAgeHours: 24
      })
    );
    expect(fetchMock).toHaveBeenNthCalledWith(
      1,
      'http://localhost:8080/api/admin/ingestion/jobs?status=FAILED&page=1&pageSize=20',
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: 'Bearer dev-admin-token' })
      })
    );
    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      'http://localhost:8080/api/admin/ingestion/metrics/today',
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: 'Bearer dev-admin-token' })
      })
    );
    expect(fetchMock).toHaveBeenNthCalledWith(
      3,
      'http://localhost:8080/api/admin/ingestion/sources',
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: 'Bearer dev-admin-token' })
      })
    );
  });

  it('maps AI usage and guardrail status', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(
      await mockFetchResponse({
        code: 'OK',
        data: {
          requestCount: 24,
          successCount: 22,
          failedCount: 2,
          blockedCount: 1,
          promptTokens: 18400,
          completionTokens: 5200,
          estimatedCostUsd: 0.0123,
          dailyRequestLimit: 200,
          dailyTokenLimit: 200000,
          warningPercent: 80,
          alertLevel: 'WARNING'
        }
      })
    );

    const client = createAdminApiClient({ apiBaseUrl, adminToken });
    const usage = await client.getTodayAiUsage();

    expect(usage).toEqual(expect.objectContaining({
      requestCount: 24,
      blockedCount: 1,
      estimatedCostUsd: 0.0123,
      alertLevel: 'WARNING'
    }));
    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/admin/ai-usage/today',
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: 'Bearer dev-admin-token' })
      })
    );
  });

  it('updates ingestion source enabled state through API client', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(
      await mockFetchResponse({
        code: 'OK',
        data: {
          id: 1,
          code: 'fixture-global',
          name: 'Fixture Global',
          providerType: 'FIXTURE',
          defaultCategoryCode: 'global',
          enabled: false,
          contentAccessPolicy: 'SUMMARY_ONLY',
          maxAgeHours: 24,
          allowPdfDownload: false,
          allowFullText: false
        }
      })
    );

    const client = createAdminApiClient({ apiBaseUrl, adminToken });
    const source = await client.updateIngestionSourceEnabled(1, false);

    expect(source).toEqual(
      expect.objectContaining({
        id: 1,
        code: 'fixture-global',
        enabled: false
      })
    );
    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/admin/ingestion/sources/1/enabled',
      expect.objectContaining({
        method: 'PUT',
        headers: expect.objectContaining({
          Authorization: 'Bearer dev-admin-token',
          'Content-Type': 'application/json'
        }),
        body: JSON.stringify({ enabled: false })
      })
    );
  });

  it('runs ingestion source through API client', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(
      await mockFetchResponse({
        code: 'OK',
        data: {
          jobId: 301,
          sourceCode: 'fixture-global',
          providerType: 'FIXTURE',
          status: 'SUCCESS',
          fetchedCount: 3,
          newCount: 2,
          duplicateCount: 1,
          candidateCount: 2,
          errorMessage: null
        }
      })
    );

    const client = createAdminApiClient({ apiBaseUrl, adminToken });
    const result = await client.runIngestionSource(1, { pageSize: 3, generateCandidates: true });

    expect(result).toEqual(
      expect.objectContaining({
        jobId: 301,
        sourceCode: 'fixture-global',
        status: 'SUCCESS',
        fetchedCount: 3,
        candidateCount: 2
      })
    );
    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/admin/ingestion/sources/1/run',
      expect.objectContaining({
        method: 'POST',
        headers: expect.objectContaining({
          Authorization: 'Bearer dev-admin-token',
          'Content-Type': 'application/json'
        }),
        body: JSON.stringify({ pageSize: 3, generateCandidates: true })
      })
    );
  });

  it('maps publish operation logs API', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(
      await mockFetchResponse({
        code: 'OK',
        data: {
          items: [
            {
              id: 91,
              module: 'PUBLISH',
              actionType: 'PUBLISH_ARTICLE',
              targetType: 'ARTICLE',
              targetId: 501,
              targetTitle: '高盛：AI 基建投资仍将持续',
              status: 'SUCCESS',
              operatorName: 'dev-admin',
              detail: '文章发布成功',
              createdAt: '2026-06-10T09:10:00'
            }
          ],
          page: 1,
          pageSize: 20,
          total: 1,
          hasMore: false
        }
      })
    );

    const client = createAdminApiClient({ apiBaseUrl, adminToken });
    const logs = await client.listOperationLogs('PUBLISH');

    expect(logs[0]).toEqual(
      expect.objectContaining({
        id: 91,
        actionType: 'PUBLISH_ARTICLE',
        targetType: 'ARTICLE',
        targetTitle: '高盛：AI 基建投资仍将持续',
        operatorName: 'dev-admin'
      })
    );
    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/admin/operation-logs?module=PUBLISH&page=1&pageSize=20',
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: 'Bearer dev-admin-token' })
      })
    );
  });

  it('maps ingestion quality anomalies API', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(
      await mockFetchResponse({
        code: 'OK',
        data: {
          items: [
            {
              id: 301,
              rawNewsItemId: 301,
              title: '缺链接样本',
              sourceCode: 'fixture-global',
              sourceName: 'Fixture Global',
              originalUrl: '',
              publishedAt: '2026-06-10T08:00:00',
              fetchedAt: '2026-06-10T08:10:00',
              issueType: 'MISSING_ORIGINAL_URL',
              severity: 'HIGH',
              description: '原始资讯缺少原文链接，无法满足可追溯要求'
            }
          ],
          page: 1,
          pageSize: 20,
          total: 1,
          hasMore: false
        }
      })
    );

    const client = createAdminApiClient({ apiBaseUrl, adminToken });
    const anomalies = await client.listIngestionAnomalies();

    expect(anomalies[0]).toEqual(
      expect.objectContaining({
        rawNewsItemId: 301,
        title: '缺链接样本',
        issueType: 'MISSING_ORIGINAL_URL',
        severity: 'HIGH'
      })
    );
    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/admin/ingestion/anomalies?page=1&pageSize=20',
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: 'Bearer dev-admin-token' })
      })
    );
  });
});
