import type { AdminCandidate } from '../shared/types/candidate';

export const mockCandidates: AdminCandidate[] = [
  {
    id: 201,
    rawNewsItemId: 101,
    title: '高盛：AI 基建投资仍将持续',
    summary: '高盛公开观点认为，AI 基础设施投资仍处扩张阶段，算力、电力和数据中心产业链将持续受益。',
    aiSummary: 'AI 基建投资仍处于扩张阶段，云厂商资本开支、数据中心电力需求和高端芯片供给是审核重点。',
    categoryCode: 'investment_view',
    categoryName: '投行观点',
    sourceName: 'Goldman Sachs Research',
    tagNames: ['AI 基建', '投行观点'],
    originalUrl: 'https://example.com/goldman-ai-capex',
    publishedAt: '2026-06-09 08:40',
    fetchedAt: '2026-06-09 09:00',
    status: 'PENDING_REVIEW',
    content: null,
    reportAssets: [
      {
        id: 301,
        title: 'AI infrastructure outlook',
        fileName: 'ai-infrastructure-outlook.pdf',
        licensePolicy: 'PDF_ALLOWED',
        status: 'PENDING_REVIEW'
      }
    ]
  },
  {
    id: 202,
    rawNewsItemId: 102,
    title: '美联储官员释放谨慎信号，市场重新评估降息节奏',
    summary: '市场关注美联储最新表态，投资者正在调整对后续利率路径的预期。',
    aiSummary: '利率预期仍是全球资产定价的核心变量，需关注美元、美债和成长股估值波动。',
    categoryCode: 'macro',
    categoryName: '宏观政策',
    sourceName: 'Federal Reserve',
    tagNames: ['利率', '美联储'],
    originalUrl: 'https://example.com/fed-rate-path',
    publishedAt: '2026-06-09 08:10',
    fetchedAt: '2026-06-09 08:36',
    status: 'PENDING_REVIEW',
    content: null,
    reportAssets: []
  },
  {
    id: 203,
    rawNewsItemId: 103,
    title: '全球科技巨头继续加码 AI 应用，企业端落地成为竞争焦点',
    summary: 'AI 应用正在从模型能力竞争转向企业效率提升和场景落地。',
    aiSummary: '企业端 AI 应用进入效率验证阶段，软件、云服务和垂直行业解决方案值得跟踪。',
    categoryCode: 'ai',
    categoryName: 'AI 前沿',
    sourceName: 'Tech Brief',
    tagNames: ['企业 AI'],
    originalUrl: 'https://example.com/enterprise-ai',
    publishedAt: '2026-06-09 07:55',
    fetchedAt: '2026-06-09 08:22',
    status: 'REJECTED',
    content: null,
    reportAssets: []
  }
];
