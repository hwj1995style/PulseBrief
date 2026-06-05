import type { AdminMetric, Article, Category, Digest, UserProfile } from './types';

export const categories: Category[] = [
  { code: 'global', name: '全球热点', description: '跨地区重大事件与多来源聚合。', subscribedByDefault: true },
  { code: 'finance', name: '财经市场', description: '股市、债券、汇率和大宗商品。', subscribedByDefault: true },
  { code: 'technology', name: '科技趋势', description: '科技公司、平台、产品和产业变化。', subscribedByDefault: true },
  { code: 'ai', name: 'AI 前沿', description: '大模型、算力、芯片和 AI 应用。', subscribedByDefault: false },
  { code: 'macro', name: '宏观政策', description: '利率、通胀、央行与财政政策。', subscribedByDefault: false },
  { code: 'ib_research', name: '投行观点', description: '公开投行观点和市场解读。', subscribedByDefault: false },
  { code: 'china_us', name: '中美动态', description: '贸易、科技、监管与外交进展。', subscribedByDefault: false },
  { code: 'industry', name: '产业观察', description: '半导体、新能源、医药和消费。', subscribedByDefault: false },
  { code: 'company', name: '公司动态', description: '重点公司公告和公开博客。', subscribedByDefault: false }
];

export const articles: Article[] = [
  {
    id: 101,
    title: '全球央行释放谨慎信号，市场重新评估下半年利率路径',
    sourceName: 'Federal Reserve / ECB Public Remarks',
    sourceType: 'OFFICIAL',
    categoryCode: 'macro',
    categoryName: '宏观政策',
    language: 'en',
    country: 'US/EU',
    publishTime: '2026-06-05T07:10:00+08:00',
    hotScore: 96.5,
    isTop: true,
    isBreaking: false,
    summary: '多位央行官员强调通胀仍需观察，市场对快速降息的预期降温。',
    aiSummary: '最新公开讲话显示，主要央行对通胀回落保持谨慎，政策转向节奏可能比市场此前预期更慢。债券收益率和成长股估值将继续受到利率路径扰动。',
    keyPoints: ['央行措辞偏谨慎', '市场降息预期后移', '债券和成长股波动可能上升'],
    impactAnalysis: '宏观资产需要重新定价，短期更利好现金流稳定和防御性行业。',
    originalUrl: 'https://www.federalreserve.gov/',
    favoriteCount: 214,
    playCount: 501
  },
  {
    id: 102,
    title: '头部云厂商扩大 AI 基础设施投入，算力供应链订单能见度提升',
    sourceName: 'Company Blogs / Public IR',
    sourceType: 'RSS',
    categoryCode: 'ai',
    categoryName: 'AI 前沿',
    language: 'en',
    country: 'US',
    publishTime: '2026-06-05T06:30:00+08:00',
    hotScore: 94.2,
    isTop: true,
    isBreaking: true,
    summary: '多家云厂商在公开资料中提到持续扩容 AI 训练与推理基础设施。',
    aiSummary: 'AI 基础设施投资仍是科技资本开支主线。服务器、网络、散热和先进封装链条的订单能见度继续提升，但市场会更关注投入回报和推理需求兑现。',
    keyPoints: ['AI capex 仍处高位', '供应链订单能见度改善', '投资人关注推理商业化'],
    impactAnalysis: '利好算力硬件和云服务生态，但高估值标的需要业绩兑现支撑。',
    originalUrl: 'https://blogs.microsoft.com/',
    favoriteCount: 198,
    playCount: 455
  },
  {
    id: 103,
    title: '投行公开观点提示：亚洲出口链条受益于电子周期复苏',
    sourceName: 'Goldman Sachs Insights',
    sourceType: 'IB_PUBLIC',
    categoryCode: 'ib_research',
    categoryName: '投行观点',
    language: 'en',
    country: 'US',
    publishTime: '2026-06-05T05:50:00+08:00',
    hotScore: 90.4,
    isTop: true,
    isBreaking: false,
    summary: '公开观点认为电子终端补库存和 AI 服务器需求可能共同支撑亚洲出口。',
    aiSummary: '投行公开观点显示，亚洲出口链条正在从传统电子补库存和 AI 服务器增量需求中获得支撑。需要继续观察汇率、关税和终端需求弹性。',
    keyPoints: ['电子周期出现复苏迹象', 'AI 服务器形成增量需求', '汇率和贸易政策仍是风险'],
    impactAnalysis: '半导体、零部件和物流链条可能获得估值修复机会。',
    originalUrl: 'https://www.goldmansachs.com/insights/',
    favoriteCount: 132,
    playCount: 320
  },
  {
    id: 104,
    title: '中美科技政策继续牵动半导体投资情绪',
    sourceName: 'GDELT Event Index',
    sourceType: 'GDELT',
    categoryCode: 'china_us',
    categoryName: '中美动态',
    language: 'en',
    country: 'CN/US',
    publishTime: '2026-06-04T22:15:00+08:00',
    hotScore: 87.9,
    isTop: false,
    isBreaking: false,
    summary: '多来源报道显示，半导体、云服务和先进制造仍是政策关注重点。',
    aiSummary: '中美科技政策不确定性继续影响半导体链条投资情绪。企业可能加速供应链多元化，同时资本市场会更关注国产替代和海外收入敞口。',
    keyPoints: ['科技政策仍是核心变量', '供应链多元化加速', '关注企业海外收入敞口'],
    impactAnalysis: '政策敏感型行业波动仍高，基本面和合规能力的重要性上升。',
    originalUrl: 'https://www.gdeltproject.org/',
    favoriteCount: 91,
    playCount: 214
  },
  {
    id: 105,
    title: '新能源车供应链进入价格和产品力双重竞争阶段',
    sourceName: 'Industry Public Reports',
    sourceType: 'RSS',
    categoryCode: 'industry',
    categoryName: '产业观察',
    language: 'zh',
    country: 'CN',
    publishTime: '2026-06-04T20:35:00+08:00',
    hotScore: 80.6,
    isTop: false,
    isBreaking: false,
    summary: '车企继续通过价格、智能化配置和海外渠道争夺增量市场。',
    aiSummary: '新能源车行业竞争从单纯价格战转向价格、智能化、渠道和供应链效率的综合竞争。上游材料和零部件企业分化会进一步扩大。',
    keyPoints: ['终端竞争持续', '智能化配置成为差异点', '供应链利润分化扩大'],
    impactAnalysis: '整车龙头和具备成本优势的核心零部件企业更具韧性。',
    originalUrl: 'https://www.sec.gov/edgar',
    favoriteCount: 72,
    playCount: 168
  },
  {
    id: 106,
    title: '大型科技公司公开博客更新隐私与模型安全治理实践',
    sourceName: 'OpenAI / Google Public Blogs',
    sourceType: 'RSS',
    categoryCode: 'technology',
    categoryName: '科技趋势',
    language: 'en',
    country: 'US',
    publishTime: '2026-06-04T19:20:00+08:00',
    hotScore: 78.1,
    isTop: false,
    isBreaking: false,
    summary: '公开博客强调模型评估、数据治理和用户控制能力。',
    aiSummary: '科技公司正在把模型安全、隐私治理和合规透明度变成产品能力的一部分。这会影响企业客户采购决策，也可能成为差异化竞争点。',
    keyPoints: ['模型安全治理公开化', '隐私控制成为产品能力', '企业客户更重视合规证据'],
    impactAnalysis: 'B2B AI 产品的合规和安全能力会更直接影响商业转化。',
    originalUrl: 'https://openai.com/news/',
    favoriteCount: 63,
    playCount: 144
  }
];

export const digests: Digest[] = [
  {
    id: 201,
    type: 'MORNING',
    title: '今日全球早报：利率、AI 算力与亚洲出口链条',
    publishTime: '2026-06-05T07:00:00+08:00',
    summary: '过去 24 小时重点集中在央行表态、AI 基础设施投资和投行对亚洲出口链条的公开观点。',
    content: '今天需要关注三条主线：第一，主要央行继续释放谨慎信号；第二，AI 基础设施投资维持高位；第三，亚洲出口链条可能受益于电子周期复苏。',
    audioText: '早上好，这里是 PulseBrief 今日全球早报。今天的重点是利率路径、AI 算力投入，以及亚洲出口链条复苏。',
    articleIds: [101, 102, 103]
  },
  {
    id: 202,
    type: 'NOON',
    title: '午间快讯：科技政策与产业链波动',
    publishTime: '2026-06-05T12:00:00+08:00',
    summary: '中美科技政策和半导体链条继续牵动市场情绪。',
    content: '午间重点关注科技政策对半导体估值和供应链配置的影响。',
    audioText: '这里是午间快讯。科技政策和半导体链条仍是今天市场关注的焦点。',
    articleIds: [104, 106]
  },
  {
    id: 203,
    type: 'EVENING',
    title: '晚间复盘：风险偏好回落，结构机会仍在',
    publishTime: '2026-06-05T18:00:00+08:00',
    summary: '市场风险偏好受到利率和政策变量影响，但 AI、出口链和优质制造仍有结构机会。',
    content: '晚间复盘看，宏观变量压制风险偏好，不过 AI 基础设施、亚洲出口链条和优质制造仍是结构性主线。',
    audioText: '晚上好，这里是 PulseBrief 晚间复盘。今天风险偏好略有回落，但结构性机会仍在。',
    articleIds: [101, 102, 103, 105]
  }
];

export const defaultProfile: UserProfile = {
  nickname: 'Pulse Reader',
  selectedCategories: categories.filter((category) => category.subscribedByDefault).map((category) => category.code),
  playbackSpeed: '1.0x',
  language: '中文',
  autoPlayNext: true,
  morningPush: true,
  eveningPush: true,
  breakingPush: false,
  ibPush: false
};

export const adminMetrics: AdminMetric[] = [
  { label: '今日采集文章数', value: '1,248', trend: '+18%' },
  { label: '今日有效文章数', value: '386', trend: '+9%' },
  { label: '今日简报数', value: '3', trend: '稳定' },
  { label: '今日活跃用户', value: '8,420', trend: '+12%' },
  { label: '今日播放次数', value: '5,916', trend: '+21%' },
  { label: '推送点击率', value: '18.4%', trend: '+2.1pp' }
];

