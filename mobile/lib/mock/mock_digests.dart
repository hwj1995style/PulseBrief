import 'package:pulsebrief/shared/models/digest.dart';

const mockDigests = <Digest>[
  Digest(
    id: 'morning',
    title: '每日早报',
    subtitle: '全球市场开盘前，10 条必听要点',
    updateTime: '08:30 更新',
    summary: '精选全球、财经、AI 与投行观点 10 条重点',
    iconLabel: '☀',
    duration: '08:12',
  ),
  Digest(
    id: 'noon',
    title: '午间快讯',
    subtitle: '午间全球要闻速递，快速掌握最新动态',
    updateTime: '12:20 更新',
    summary: '亚洲与欧洲时段重点事件',
    iconLabel: '◴',
    duration: '04:36',
  ),
  Digest(
    id: 'evening',
    title: '晚间复盘',
    subtitle: '收盘后深度复盘，洞察市场与机会',
    updateTime: '21:00 更新',
    summary: '市场、产业和政策复盘',
    iconLabel: '☾',
    duration: '07:28',
  ),
  Digest(
    id: 'ai',
    title: 'AI 专题',
    subtitle: '聚焦 AI 前沿趋势与产业动态',
    updateTime: '10:00 更新',
    summary: '大模型、算力、芯片、应用动态',
    iconLabel: 'AI',
    duration: '06:10',
  ),
  Digest(
    id: 'ib',
    title: '投行观点精选',
    subtitle: '精选投行报告与核心观点解读',
    updateTime: '09:00 更新',
    summary: '公开观点摘要，不展示完整报告',
    iconLabel: 'IB',
    duration: '05:48',
  ),
];

const digestHighlights = <String>[
  '英伟达 Blackwell Ultra 发布',
  '美联储维持利率不变',
  '标普 500 再创阶段新高',
  '苹果推进自研 AI 芯片',
  'OPEC+ 延长减产计划',
  '高盛看多 AI 基建投资',
];
