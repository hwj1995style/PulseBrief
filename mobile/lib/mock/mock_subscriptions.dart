import 'package:pulsebrief/shared/models/subscription_topic.dart';

const mockSubscriptionTopics = <SubscriptionTopic>[
  SubscriptionTopic(name: '全球热点', description: '全球重点事件', selected: true),
  SubscriptionTopic(name: '财经市场', description: '股市、利率、汇率', selected: true),
  SubscriptionTopic(name: '科技趋势', description: '科技公司与产品', selected: true),
  SubscriptionTopic(name: 'AI 前沿', description: '大模型、算力、芯片', selected: true),
  SubscriptionTopic(name: '宏观政策', description: '央行、通胀、财政'),
  SubscriptionTopic(name: '投行观点', description: '公开观点摘要', selected: true),
  SubscriptionTopic(name: '中美动态', description: '关系、贸易、产业'),
  SubscriptionTopic(name: '产业观察', description: '重点产业链'),
  SubscriptionTopic(name: '公司动态', description: '重点公司新闻'),
  SubscriptionTopic(name: '半导体', description: '芯片与设备'),
  SubscriptionTopic(name: '新能源', description: '能源与电动车'),
  SubscriptionTopic(name: '数字资产', description: '加密与链上数据'),
];

const focusChannels = <SubscriptionTopic>[
  SubscriptionTopic(
    name: '投行观点',
    description: '高盛、大摩、野村等公开观点摘要',
    selected: true,
  ),
  SubscriptionTopic(
    name: 'AI 早报',
    description: '大模型、算力、芯片、应用动态',
    selected: true,
  ),
  SubscriptionTopic(name: '财经快讯', description: '股市、利率、汇率和商品市场', selected: true),
  SubscriptionTopic(name: '宏观政策', description: '央行、通胀、财政和监管动态'),
];
