import type { AdminDigest, AdminDigestArticleCandidate } from '../shared/types/digest';

export const mockDigestArticleCandidates: AdminDigestArticleCandidate[] = [
  {
    id: 501,
    title: '高盛：AI 基建投资仍将持续',
    sourceName: 'Goldman Sachs Research',
    publishTime: '2小时前',
    categoryName: '投行观点',
    summary: 'AI 基础设施投资仍处于扩张阶段，算力、电力和数据中心产业链将持续受益。'
  },
  {
    id: 502,
    title: '美联储官员释放谨慎信号',
    sourceName: 'Federal Reserve',
    publishTime: '3小时前',
    categoryName: '宏观政策',
    summary: '投资者正在调整对后续利率路径的预期。'
  },
  {
    id: 503,
    title: '全球科技巨头继续加码 AI 应用',
    sourceName: 'Tech Brief',
    publishTime: '4小时前',
    categoryName: 'AI 前沿',
    summary: 'AI 应用正在从模型能力竞争转向企业效率提升和场景落地。'
  }
];

export const mockDigests: AdminDigest[] = [
  {
    id: 301,
    digestDate: '2026-06-10',
    digestType: 'MORNING',
    categoryCode: 'global',
    title: '今日全球早报',
    summary: '精选全球、财经、AI 与投行观点重点',
    content: '美联储维持谨慎信号\n标普 500 继续走强',
    audioText: '欢迎收听脉闻今日全球早报。',
    status: 'PUBLISHED',
    publishTime: '2026-06-10T08:30:00+08:00',
    articleCount: 2,
    articles: [
      {
        articleId: 502,
        sortNo: 1,
        highlightText: '美联储维持谨慎信号',
        title: '美联储官员释放谨慎信号',
        sourceName: 'Federal Reserve'
      }
    ],
    availableActions: ['OFFLINE']
  }
];
