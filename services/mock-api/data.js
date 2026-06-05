export const categories = [
  { code: 'global', name: '全球热点', description: '跨地区重大事件与多来源聚合。' },
  { code: 'finance', name: '财经市场', description: '股市、债券、汇率和大宗商品。' },
  { code: 'technology', name: '科技趋势', description: '科技公司、平台、产品和产业变化。' },
  { code: 'ai', name: 'AI 前沿', description: '大模型、算力、芯片和 AI 应用。' },
  { code: 'macro', name: '宏观政策', description: '利率、通胀、央行与财政政策。' },
  { code: 'ib_research', name: '投行观点', description: '公开投行观点和市场解读。' }
];

export const articles = [
  {
    id: 101,
    title: '全球央行释放谨慎信号，市场重新评估下半年利率路径',
    sourceName: 'Federal Reserve / ECB Public Remarks',
    categoryCode: 'macro',
    categoryName: '宏观政策',
    publishTime: '2026-06-05T07:10:00+08:00',
    hotScore: 96.5,
    isTop: true,
    summary: '多位央行官员强调通胀仍需观察，市场对快速降息的预期降温。',
    aiSummary: '主要央行对通胀回落保持谨慎，政策转向节奏可能慢于市场此前预期。',
    originalUrl: 'https://www.federalreserve.gov/'
  },
  {
    id: 102,
    title: '头部云厂商扩大 AI 基础设施投入，算力供应链订单能见度提升',
    sourceName: 'Company Blogs / Public IR',
    categoryCode: 'ai',
    categoryName: 'AI 前沿',
    publishTime: '2026-06-05T06:30:00+08:00',
    hotScore: 94.2,
    isTop: true,
    summary: '多家云厂商持续扩容 AI 训练与推理基础设施。',
    aiSummary: 'AI 基础设施投资仍是科技资本开支主线。',
    originalUrl: 'https://blogs.microsoft.com/'
  }
];

export const digests = [
  {
    id: 201,
    type: 'MORNING',
    title: '今日全球早报：利率、AI 算力与亚洲出口链条',
    publishTime: '2026-06-05T07:00:00+08:00',
    summary: '过去 24 小时重点集中在央行表态、AI 基础设施投资和投行公开观点。',
    audioText: '早上好，这里是 PulseBrief 今日全球早报。'
  }
];

export const adminOverview = {
  metrics: [
    { label: '今日采集文章数', value: '1,248', trend: '+18%' },
    { label: '今日有效文章数', value: '386', trend: '+9%' },
    { label: '今日简报数', value: '3', trend: '稳定' },
    { label: '推送点击率', value: '18.4%', trend: '+2.1pp' }
  ]
};

