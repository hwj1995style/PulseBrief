import { Check, FileAudio, PlayCircle, RefreshCw, Send, Sparkles } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import {
  createDigest,
  getInitialDigests,
  listDigestArticleCandidates,
  listDigests,
  publishDigest
} from '../../shared/api/adminApi';
import type { AdminDigest, AdminDigestArticleCandidate } from '../../shared/types/digest';
import { buildDigestInput, digestStatusClass, digestStatusLabel } from './digestUtils';

export function DigestManagementPage() {
  const [digests, setDigests] = useState<AdminDigest[]>(() => getInitialDigests());
  const [articles, setArticles] = useState<AdminDigestArticleCandidate[]>([]);
  const [selectedArticleIds, setSelectedArticleIds] = useState<number[]>([]);
  const [selectedDigestId, setSelectedDigestId] = useState<number | null>(digests[0]?.id ?? null);
  const [isLoading, setIsLoading] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  useEffect(() => {
    void refreshDigests();
  }, []);

  const selectedArticles = useMemo(
    () => articles.filter((article) => selectedArticleIds.includes(article.id)),
    [articles, selectedArticleIds]
  );

  const selectedDigest = useMemo(
    () => digests.find((digest) => digest.id === selectedDigestId) ?? digests[0] ?? null,
    [digests, selectedDigestId]
  );

  const draftCount = digests.filter((digest) => digest.status === 'DRAFT').length;
  const publishedCount = digests.filter((digest) => digest.status === 'PUBLISHED').length;

  async function refreshDigests() {
    setIsLoading(true);
    setErrorMessage('');
    try {
      const [digestList, articleList] = await Promise.all([
        listDigests('ALL'),
        listDigestArticleCandidates()
      ]);
      setDigests(digestList);
      setArticles(articleList);
      setSelectedDigestId((current) => current ?? digestList[0]?.id ?? null);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : '简报数据加载失败，请检查 Admin API 配置。');
    } finally {
      setIsLoading(false);
    }
  }

  function toggleArticle(articleId: number) {
    setSelectedArticleIds((current) =>
      current.includes(articleId) ? current.filter((id) => id !== articleId) : [...current, articleId]
    );
  }

  async function handleCreateDraft() {
    if (selectedArticles.length === 0) {
      setErrorMessage('请先选择至少 1 条已发布文章。');
      return;
    }
    setIsSaving(true);
    setErrorMessage('');
    try {
      const digest = await createDigest(buildDigestInput(selectedArticles));
      setDigests((current) => [digest, ...current]);
      setSelectedDigestId(digest.id);
      setSuccessMessage('草稿已创建，可发布到 APP');
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : '简报草稿创建失败。');
    } finally {
      setIsSaving(false);
    }
  }

  async function handlePublishDigest() {
    if (!selectedDigest || selectedDigest.status !== 'DRAFT') {
      return;
    }
    setIsSaving(true);
    setErrorMessage('');
    try {
      const digest = await publishDigest(selectedDigest.id);
      setDigests((current) => current.map((item) => (item.id === digest.id ? digest : item)));
      setSelectedDigestId(digest.id);
      setSuccessMessage('已发布到 APP');
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : '简报发布失败。');
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <main className="workspace digest-workspace">
      <section className="workspace-main">
        <div className="page-heading">
          <div>
            <p className="page-kicker">Daily Digest</p>
            <h1>简报管理</h1>
            <p>从已发布文章中选择热点，生成每日简报草稿并发布到 Flutter APP。</p>
          </div>
          <button className="icon-button" aria-label="刷新简报数据" disabled={isLoading} onClick={refreshDigests}>
            <RefreshCw className={isLoading ? 'spin-icon' : undefined} size={18} />
          </button>
        </div>

        <div className="metric-grid" aria-label="简报统计">
          <MetricCard label="草稿" value={draftCount} tone="warning" />
          <MetricCard label="已发布" value={publishedCount} tone="success" />
          <MetricCard label="已选文章" value={selectedArticles.length} />
          <MetricCard label="候选文章" value={articles.length} />
        </div>

        {errorMessage ? <p className="inline-error">{errorMessage}</p> : null}
        {successMessage ? <p className="inline-success">{successMessage}</p> : null}

        <section className="panel digest-builder" aria-label="创建每日简报">
          <div className="builder-header">
            <div>
              <strong>今日全球早报</strong>
              <span>默认创建 MORNING 类型，后续编辑接口完成后开放更多类型。</span>
            </div>
            <button
              className="primary-action compact"
              disabled={isSaving || selectedArticles.length === 0}
              onClick={handleCreateDraft}
            >
              <Sparkles size={16} />
              创建草稿
            </button>
          </div>

          <div className="selection-strip">
            <strong>已选 {selectedArticles.length} 条</strong>
            <span>{selectedArticles.map((article) => article.categoryName).join(' / ') || '请选择已发布文章'}</span>
          </div>
        </section>

        <section className="panel digest-list-panel">
          <div className="toolbar">
            <strong>简报列表</strong>
            <span className="muted">{digests.length} 条记录</span>
          </div>
          <div className="digest-list" aria-label="简报列表">
            {digests.length === 0 ? (
              <p className="table-state">暂无简报记录。</p>
            ) : (
              digests.map((digest) => (
                <button
                  className={selectedDigest?.id === digest.id ? 'digest-row selected' : 'digest-row'}
                  key={digest.id}
                  onClick={() => setSelectedDigestId(digest.id)}
                >
                  <span className={`status-chip ${digestStatusClass(digest.status)}`}>
                    {digestStatusLabel(digest.status)}
                  </span>
                  <span className="title-cell">
                    {digest.title}
                    <small>
                      {digest.digestDate} · {digest.articleCount} 条热点
                    </small>
                  </span>
                  <span>{digest.digestType}</span>
                </button>
              ))
            )}
          </div>
        </section>

        <section className="panel article-pool">
          <div className="toolbar">
            <strong>已发布文章池</strong>
            <span className="muted">选择后生成热点清单</span>
          </div>
          <div className="article-pool-list">
            {articles.map((article) => {
              const selected = selectedArticleIds.includes(article.id);
              return (
                <button
                  className={selected ? 'article-pool-row selected' : 'article-pool-row'}
                  key={article.id}
                  aria-label={`${selected ? '取消选择' : '选择'} ${article.title}`}
                  onClick={() => toggleArticle(article.id)}
                >
                  <span className="article-check">{selected ? <Check size={16} /> : article.id}</span>
                  <span className="title-cell">
                    {article.title}
                    <small>
                      {article.sourceName} · {article.publishTime} · {article.categoryName}
                    </small>
                    <small>{article.summary}</small>
                  </span>
                </button>
              );
            })}
          </div>
        </section>
      </section>

      <aside className="detail-panel digest-detail" aria-label="简报详情">
        {selectedDigest ? (
          <>
            <div className="detail-header">
              <FileAudio size={22} />
              <span className={`status-chip ${digestStatusClass(selectedDigest.status)}`}>
                {digestStatusLabel(selectedDigest.status)}
              </span>
            </div>
            <h2>{selectedDigest.title}</h2>
            <p className="detail-meta">
              {selectedDigest.digestDate} · {selectedDigest.digestType} · {selectedDigest.articleCount} 条热点
            </p>

            <section className="detail-section">
              <h3>摘要</h3>
              <p>{selectedDigest.summary}</p>
            </section>

            <section className="detail-section">
              <h3>热点清单</h3>
              <ol className="digest-points">
                {selectedDigest.content.split('\n').map((point) => (
                  <li key={point}>{point}</li>
                ))}
              </ol>
            </section>

            <section className="detail-section">
              <h3>播报文案</h3>
              <p>{selectedDigest.audioText}</p>
            </section>

            <div className="detail-actions">
              <button
                className="primary-action"
                disabled={isSaving || selectedDigest.status !== 'DRAFT'}
                onClick={handlePublishDigest}
              >
                <Send size={16} />
                发布简报
              </button>
              <button className="secondary-action" disabled>
                <PlayCircle size={16} />
                预览播报
              </button>
            </div>
          </>
        ) : (
          <p className="muted">暂无简报详情。</p>
        )}
      </aside>
    </main>
  );
}

function MetricCard({ label, value, tone }: { label: string; value: number; tone?: 'success' | 'warning' }) {
  return (
    <div className={tone ? `metric-card ${tone}` : 'metric-card'}>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}
