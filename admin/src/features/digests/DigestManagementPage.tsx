import { ArchiveX, Check, FileAudio, PlayCircle, RefreshCw, Save, Send, Sparkles } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import {
  createDigest,
  getInitialDigests,
  listDigestArticleCandidates,
  listDigests,
  offlineDigest,
  publishDigest,
  updateDigest
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
  const [draftTitle, setDraftTitle] = useState('今日全球早报');
  const [draftSummary, setDraftSummary] = useState('精选 0 条重点资讯');
  const [draftAudioText, setDraftAudioText] = useState('欢迎收听脉闻今日全球早报。');

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
  const isEditingDraft = selectedDigest?.status === 'DRAFT';

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

  function selectDigest(digest: AdminDigest) {
    setSelectedDigestId(digest.id);
    if (digest.status === 'DRAFT') {
      loadDigestIntoForm(digest);
    }
  }

  function loadDigestIntoForm(digest: AdminDigest) {
    setDraftTitle(digest.title);
    setDraftSummary(digest.summary);
    setDraftAudioText(digest.audioText);
    setSelectedArticleIds(digest.articles.map((article) => article.articleId));
    setSuccessMessage('');
  }

  function currentDigestInput() {
    return buildDigestInput(selectedArticles, undefined, {
      title: draftTitle.trim() || '今日全球早报',
      summary: draftSummary.trim() || `精选 ${selectedArticles.length} 条重点资讯`,
      audioText: draftAudioText.trim() || '欢迎收听脉闻今日全球早报。'
    });
  }

  async function handleCreateDraft() {
    if (selectedArticles.length === 0) {
      setErrorMessage('请先选择至少 1 条已发布文章。');
      return;
    }
    setIsSaving(true);
      setErrorMessage('');
    try {
      const digest = await createDigest(currentDigestInput());
      setDigests((current) => [digest, ...current]);
      setSelectedDigestId(digest.id);
      loadDigestIntoForm(digest);
      setSuccessMessage('草稿已创建，可发布到 APP');
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : '简报草稿创建失败。');
    } finally {
      setIsSaving(false);
    }
  }

  async function handleSaveDraft() {
    if (!selectedDigest || selectedDigest.status !== 'DRAFT') {
      setErrorMessage('请选择草稿后再保存。');
      return;
    }
    if (selectedArticles.length === 0) {
      setErrorMessage('请先选择至少 1 条已发布文章。');
      return;
    }
    setIsSaving(true);
    setErrorMessage('');
    try {
      const digest = await updateDigest(selectedDigest.id, currentDigestInput());
      setDigests((current) => current.map((item) => (item.id === digest.id ? digest : item)));
      setSelectedDigestId(digest.id);
      loadDigestIntoForm(digest);
      setSuccessMessage('草稿已保存');
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : '简报草稿保存失败。');
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

  async function handleOfflineDigest() {
    if (!selectedDigest || selectedDigest.status !== 'PUBLISHED') {
      return;
    }
    setIsSaving(true);
    setErrorMessage('');
    try {
      const digest = await offlineDigest(selectedDigest.id);
      setDigests((current) => current.map((item) => (item.id === digest.id ? digest : item)));
      setSelectedDigestId(digest.id);
      setSuccessMessage('已下线，用户端不可见');
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : '简报下线失败。');
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
              <strong>{isEditingDraft ? '编辑每日简报草稿' : '今日全球早报'}</strong>
              <span>编辑标题、摘要、播报文案，并通过文章池维护热点清单。</span>
            </div>
            <button
              className="primary-action compact"
              disabled={isSaving || selectedArticles.length === 0}
              onClick={isEditingDraft ? handleSaveDraft : handleCreateDraft}
            >
              {isEditingDraft ? <Save size={16} /> : <Sparkles size={16} />}
              {isEditingDraft ? '保存草稿' : '创建草稿'}
            </button>
          </div>

          <div className="digest-form-grid">
            <label className="field-label">
              <span>简报标题</span>
              <input value={draftTitle} onChange={(event) => setDraftTitle(event.target.value)} />
            </label>
            <label className="field-label">
              <span>简报摘要</span>
              <input value={draftSummary} onChange={(event) => setDraftSummary(event.target.value)} />
            </label>
            <label className="field-label full">
              <span>播报文案</span>
              <textarea value={draftAudioText} onChange={(event) => setDraftAudioText(event.target.value)} />
            </label>
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
                  onClick={() => selectDigest(digest)}
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
                className="secondary-action"
                disabled={selectedDigest.status !== 'DRAFT'}
                onClick={() => loadDigestIntoForm(selectedDigest)}
              >
                <Save size={16} />
                编辑草稿
              </button>
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
              <button
                className="secondary-action danger"
                disabled={isSaving || selectedDigest.status !== 'PUBLISHED'}
                onClick={handleOfflineDigest}
              >
                <ArchiveX size={16} />
                下线简报
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
