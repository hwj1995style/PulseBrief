import type { Article, Digest, PlayerItem } from './types';

export function sortHomeArticles(items: Article[], selectedCategories: string[]): Article[] {
  return [...items].sort((left, right) => {
    const leftSubscription = selectedCategories.includes(left.categoryCode) ? 1 : 0;
    const rightSubscription = selectedCategories.includes(right.categoryCode) ? 1 : 0;
    const leftTime = new Date(left.publishTime).getTime();
    const rightTime = new Date(right.publishTime).getTime();

    return (
      Number(right.isTop) - Number(left.isTop) ||
      Number(right.isBreaking) - Number(left.isBreaking) ||
      rightSubscription - leftSubscription ||
      right.hotScore - left.hotScore ||
      rightTime - leftTime
    );
  });
}

export function filterArticlesByCategory(items: Article[], categoryCode: string): Article[] {
  if (categoryCode === 'all') {
    return items;
  }

  return items.filter((article) => article.categoryCode === categoryCode);
}

export function canCompleteSubscriptions(selectedCategories: string[]): boolean {
  return selectedCategories.length >= 3;
}

export function toggleFavorite(favoriteIds: number[], articleId: number): number[] {
  if (favoriteIds.includes(articleId)) {
    return favoriteIds.filter((id) => id !== articleId);
  }

  return [...favoriteIds, articleId];
}

export function articleToPlayerItem(article: Article): PlayerItem {
  return {
    id: `article-${article.id}`,
    title: article.title,
    sourceLabel: `${article.sourceName} / ${article.categoryName}`,
    text: `${article.aiSummary} 核心要点：${article.keyPoints.join('；')}`
  };
}

export function digestToPlayerItem(digest: Digest): PlayerItem {
  return {
    id: `digest-${digest.id}`,
    title: digest.title,
    sourceLabel: '每日简报',
    text: digest.audioText
  };
}

export function relativeTime(isoTime: string): string {
  const now = new Date('2026-06-05T13:00:00+08:00').getTime();
  const published = new Date(isoTime).getTime();
  const diffHours = Math.max(1, Math.round((now - published) / 1000 / 60 / 60));

  if (diffHours < 24) {
    return `${diffHours} 小时前`;
  }

  return `${Math.round(diffHours / 24)} 天前`;
}

