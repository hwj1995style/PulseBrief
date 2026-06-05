import 'package:pulsebrief/shared/models/news_category.dart';

const mockCategories = <NewsCategory>[
  NewsCategory(
    code: 'global',
    name: '全球热点',
    description: '追踪全球政治、经济和突发事件',
    todayCount: 36,
  ),
  NewsCategory(
    code: 'finance',
    name: '财经市场',
    description: '关注全球市场、利率、汇率、商品与资本动态',
    todayCount: 28,
  ),
  NewsCategory(
    code: 'tech',
    name: '科技趋势',
    description: '科技公司、产品、互联网与产业趋势',
    todayCount: 24,
  ),
  NewsCategory(
    code: 'ai',
    name: 'AI前沿',
    description: '大模型、算力、芯片和 AI 应用动态',
    todayCount: 21,
  ),
  NewsCategory(
    code: 'macro',
    name: '宏观政策',
    description: '央行、通胀、财政和监管政策',
    todayCount: 18,
  ),
  NewsCategory(
    code: 'ib',
    name: '投行观点',
    description: '高盛、大摩、野村等公开观点摘要',
    todayCount: 15,
  ),
  NewsCategory(
    code: 'industry',
    name: '产业观察',
    description: '半导体、新能源、医药、消费等产业链',
    todayCount: 20,
  ),
  NewsCategory(
    code: 'company',
    name: '公司动态',
    description: '重点公司新闻、公告和财报事件',
    todayCount: 17,
  ),
];
