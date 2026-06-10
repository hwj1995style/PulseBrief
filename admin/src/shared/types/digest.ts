export type AdminDigestStatus = 'DRAFT' | 'PUBLISHED' | 'OFFLINE';

export interface AdminDigestArticle {
  articleId: number;
  sortNo: number;
  highlightText: string;
  title: string;
  sourceName: string;
}

export interface AdminDigest {
  id: number;
  digestDate: string;
  digestType: string;
  categoryCode: string;
  title: string;
  summary: string;
  content: string;
  audioText: string;
  status: AdminDigestStatus;
  publishTime: string | null;
  articleCount: number;
  articles: AdminDigestArticle[];
  availableActions: string[];
}

export interface AdminDigestArticleCandidate {
  id: number;
  title: string;
  sourceName: string;
  publishTime: string;
  categoryName: string;
  summary: string;
}

export interface AdminDigestCreateInput {
  digestDate: string;
  digestType: string;
  categoryCode: string;
  title: string;
  summary: string;
  content: string;
  audioText: string;
  articles: Array<{
    articleId: number;
    sortNo: number;
    highlightText: string;
  }>;
}
