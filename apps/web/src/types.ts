export type TabKey = 'home' | 'categories' | 'digests' | 'profile';
export type AppMode = 'user' | 'admin';
export type DigestType = 'MORNING' | 'NOON' | 'EVENING' | 'TOPIC';

export interface Category {
  code: string;
  name: string;
  description: string;
  subscribedByDefault: boolean;
}

export interface Article {
  id: number;
  title: string;
  sourceName: string;
  sourceType: 'GDELT' | 'RSS' | 'OFFICIAL' | 'IB_PUBLIC';
  categoryCode: string;
  categoryName: string;
  language: string;
  country: string;
  publishTime: string;
  hotScore: number;
  isTop: boolean;
  isBreaking: boolean;
  summary: string;
  aiSummary: string;
  keyPoints: string[];
  impactAnalysis: string;
  originalUrl: string;
  favoriteCount: number;
  playCount: number;
}

export interface Digest {
  id: number;
  type: DigestType;
  title: string;
  publishTime: string;
  summary: string;
  content: string;
  audioText: string;
  articleIds: number[];
}

export interface PlayerItem {
  id: string;
  title: string;
  sourceLabel: string;
  text: string;
}

export interface UserProfile {
  nickname: string;
  selectedCategories: string[];
  playbackSpeed: string;
  language: string;
  autoPlayNext: boolean;
  morningPush: boolean;
  eveningPush: boolean;
  breakingPush: boolean;
  ibPush: boolean;
}

export interface AdminMetric {
  label: string;
  value: string;
  trend: string;
}

