import type { AdminDigestArticleCandidate, AdminDigestCreateInput } from '../../shared/types/digest';

export function todayIsoDate() {
  return new Date().toISOString().slice(0, 10);
}

export function buildDigestInput(
  selectedArticles: AdminDigestArticleCandidate[],
  digestDate = todayIsoDate(),
  overrides: Partial<Pick<AdminDigestCreateInput, 'title' | 'summary' | 'audioText' | 'categoryCode'>> = {}
): AdminDigestCreateInput {
  const highlights = selectedArticles.map((article) => article.title);
  return {
    digestDate,
    digestType: 'MORNING',
    categoryCode: overrides.categoryCode ?? 'global',
    title: overrides.title ?? '今日全球早报',
    summary: overrides.summary ?? `精选 ${selectedArticles.length} 条重点资讯`,
    content: highlights.join('\n'),
    audioText: overrides.audioText ?? `欢迎收听脉闻今日全球早报。本期重点包括：${highlights.join('；')}。`,
    articles: selectedArticles.map((article, index) => ({
      articleId: article.id,
      sortNo: index + 1,
      highlightText: article.title
    }))
  };
}

export function digestStatusLabel(status: string) {
  if (status === 'PUBLISHED') {
    return '已发布';
  }
  if (status === 'OFFLINE') {
    return '已下线';
  }
  return '草稿';
}

export function digestStatusClass(status: string) {
  if (status === 'PUBLISHED') {
    return 'success';
  }
  if (status === 'OFFLINE') {
    return 'danger';
  }
  return 'warning';
}
