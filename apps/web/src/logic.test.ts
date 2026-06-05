import { describe, expect, it } from 'vitest';
import { articles } from './data';
import {
  articleToPlayerItem,
  canCompleteSubscriptions,
  filterArticlesByCategory,
  sortHomeArticles,
  toggleFavorite
} from './logic';

describe('PulseBrief business logic', () => {
  it('sorts home articles by top, breaking, subscription, heat, and recency', () => {
    const sorted = sortHomeArticles(articles, ['global', 'finance', 'technology']);

    expect(sorted.slice(0, 3).every((article) => article.isTop)).toBe(true);
    expect(sorted[0].isBreaking).toBe(true);
    expect(sorted[1].hotScore).toBeGreaterThan(sorted[2].hotScore);
  });

  it('filters articles by category code', () => {
    const aiArticles = filterArticlesByCategory(articles, 'ai');

    expect(aiArticles).toHaveLength(1);
    expect(aiArticles[0].categoryName).toBe('AI 前沿');
  });

  it('requires at least three subscriptions before entering the app', () => {
    expect(canCompleteSubscriptions(['global', 'finance'])).toBe(false);
    expect(canCompleteSubscriptions(['global', 'finance', 'technology'])).toBe(true);
  });

  it('toggles favorite ids without duplicating entries', () => {
    expect(toggleFavorite([], 101)).toEqual([101]);
    expect(toggleFavorite([101], 101)).toEqual([]);
  });

  it('builds player text from article summary and key points', () => {
    const playerItem = articleToPlayerItem(articles[0]);

    expect(playerItem.id).toBe('article-101');
    expect(playerItem.text).toContain('核心要点');
  });
});
