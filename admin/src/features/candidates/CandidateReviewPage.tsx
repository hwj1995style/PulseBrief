import { CheckCircle2, Download, ExternalLink, FileText, RefreshCw, Save, Search, Sparkles, XCircle } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import {
  applyCandidateAiSummary,
  approveCandidateReportAsset,
  cacheCandidateReportAsset,
  fetchCandidateContent,
  generateCandidateAiSummary,
  getCandidate,
  getInitialCandidates,
  listCandidates,
  publishCandidate,
  rejectCandidateReportAsset,
  rejectCandidate,
  updateCandidate
} from '../../shared/api/adminApi';
import type { AdminCandidate, CandidateStatus, ReportAsset } from '../../shared/types/candidate';
import { candidateStatusText, candidateStatusTone } from './candidateUtils';

const filters: Array<{ label: string; ariaLabel: string; value: CandidateStatus | 'ALL' }> = [
  { label: '全部', ariaLabel: '全部', value: 'ALL' },
  { label: '待审核', ariaLabel: '待审核', value: 'PENDING_REVIEW' },
  { label: '发布', ariaLabel: '已发布', value: 'PUBLISHED' },
  { label: '拒绝', ariaLabel: '已拒绝', value: 'REJECTED' }
];

const categoryOptions = [
  { label: '全球热点', value: 'global' },
  { label: '财经市场', value: 'finance' },
  { label: '科技趋势', value: 'technology' },
  { label: 'AI 前沿', value: 'ai' },
  { label: '宏观政策', value: 'macro' },
  { label: '投行观点', value: 'investment_view' },
  { label: '产业观察', value: 'industry' },
  { label: '公司动态', value: 'company' }
];

export function CandidateReviewPage() {
  const [filter, setFilter] = useState<CandidateStatus | 'ALL'>('PENDING_REVIEW');
  const [candidates, setCandidates] = useState<AdminCandidate[]>(() => getInitialCandidates());
  const [selectedId, setSelectedId] = useState<number | null>(() => getInitialCandidates()[0]?.id ?? null);
  const [isLoading, setIsLoading] = useState(() => getInitialCandidates().length === 0);
  const [actionLoading, setActionLoading] = useState(false);
  const [contentLoading, setContentLoading] = useState(false);
  const [aiSummaryLoading, setAiSummaryLoading] = useState(false);
  const [assetActionLoading, setAssetActionLoading] = useState('');
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [draftTitle, setDraftTitle] = useState('');
  const [draftSummary, setDraftSummary] = useState('');
  const [draftCategoryCode, setDraftCategoryCode] = useState('global');
  const [draftSourceName, setDraftSourceName] = useState('');
  const [draftTagText, setDraftTagText] = useState('');
  const [draftCandidateId, setDraftCandidateId] = useState<number | null>(null);
  const [isDraftDirty, setIsDraftDirty] = useState(false);

  useEffect(() => {
    void refreshCandidates();
  }, []);

  const filteredCandidates = useMemo(() => {
    if (filter === 'ALL') {
      return candidates;
    }
    return candidates.filter((candidate) => candidate.status === filter);
  }, [candidates, filter]);

  const selectedCandidate =
    candidates.find((candidate) => candidate.id === selectedId) ?? filteredCandidates[0] ?? candidates[0];

  const metrics = useMemo(
    () => ({
      fetched: candidates.length,
      pending: candidates.filter((candidate) => candidate.status === 'PENDING_REVIEW').length,
      published: candidates.filter((candidate) => candidate.status === 'PUBLISHED').length,
      rejected: candidates.filter((candidate) => candidate.status === 'REJECTED').length
    }),
    [candidates]
  );

  useEffect(() => {
    if (!selectedCandidate) {
      setDraftTitle('');
      setDraftSummary('');
      setDraftCategoryCode('global');
      setDraftSourceName('');
      setDraftTagText('');
      setDraftCandidateId(null);
      setIsDraftDirty(false);
      return;
    }
    if (isDraftDirty && draftCandidateId === selectedCandidate.id) {
      return;
    }
    setDraftTitle(selectedCandidate.title);
    setDraftSummary(selectedCandidate.summary);
    setDraftCategoryCode(selectedCandidate.categoryCode);
    setDraftSourceName(selectedCandidate.sourceName);
    setDraftTagText(selectedCandidate.tagNames.join('，'));
    setDraftCandidateId(selectedCandidate.id);
    setIsDraftDirty(false);
  }, [
    draftCandidateId,
    isDraftDirty,
    selectedCandidate?.id,
    selectedCandidate?.title,
    selectedCandidate?.summary,
    selectedCandidate?.categoryCode,
    selectedCandidate?.sourceName,
    selectedCandidate?.tagNames
  ]);

  async function updateStatus(status: CandidateStatus) {
    if (!selectedCandidate) {
      return;
    }
    setActionLoading(true);
    setErrorMessage('');
    setSuccessMessage('');
    try {
      const updated =
        status === 'PUBLISHED'
          ? await publishCandidate(selectedCandidate.id, {
              title: draftTitle.trim(),
              summary: draftSummary.trim(),
              aiSummary: selectedCandidate.aiSummary,
              keyPoints: selectedCandidate.keyPoints,
              impactAnalysis: selectedCandidate.impactAnalysis,
              categoryCode: draftCategoryCode,
              publishNow: true
            })
          : await rejectCandidate(selectedCandidate.id, '运营后台审核拒绝');
      setCandidates((items) => items.map((item) => (item.id === updated.id ? { ...item, ...updated } : item)));
      setSelectedId(updated.id);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : '审核操作失败，请稍后重试。');
    } finally {
      setActionLoading(false);
    }
  }

  async function refreshCandidates() {
    setIsLoading(true);
    setErrorMessage('');
    setSuccessMessage('');
    try {
      const items = await listCandidates('ALL');
      setCandidates(items);
      setSelectedId((current) => current ?? items[0]?.id ?? null);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : '候选资讯加载失败，请检查 Admin API 配置。');
    } finally {
      setIsLoading(false);
    }
  }

  async function selectCandidate(candidate: AdminCandidate) {
    setSelectedId(candidate.id);
    setIsDraftDirty(false);
    setErrorMessage('');
    setSuccessMessage('');
    try {
      const detail = await getCandidate(candidate.id);
      setCandidates((items) => items.map((item) => (item.id === detail.id ? { ...item, ...detail } : item)));
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : '候选详情加载失败，已展示列表摘要。');
    }
  }

  async function saveCandidateDraft() {
    if (!selectedCandidate) {
      return;
    }
    const title = draftTitle.trim();
    if (!title) {
      setErrorMessage('候选标题不能为空。');
      return;
    }
    setActionLoading(true);
    setErrorMessage('');
    setSuccessMessage('');
    try {
      const updated = await updateCandidate(selectedCandidate.id, {
        title,
        summary: draftSummary.trim(),
        categoryCode: draftCategoryCode,
        sourceName: draftSourceName.trim(),
        tagNames: parseTagText(draftTagText)
      });
      setCandidates((items) => items.map((item) => (item.id === updated.id ? { ...item, ...updated } : item)));
      setSelectedId(updated.id);
      setIsDraftDirty(false);
      setSuccessMessage('候选内容已保存');
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : '候选内容保存失败，请稍后重试。');
    } finally {
      setActionLoading(false);
    }
  }

  async function fetchAuthorizedSnippet() {
    if (!selectedCandidate) {
      return;
    }
    setContentLoading(true);
    setErrorMessage('');
    setSuccessMessage('');
    try {
      const content = await fetchCandidateContent(selectedCandidate.id, 'SNIPPET');
      setCandidates((items) =>
        items.map((item) => (item.id === selectedCandidate.id ? { ...item, content } : item))
      );
      setSuccessMessage(content.fetchStatus === 'SUCCESS' ? '授权正文片段已抓取' : '正文抓取未写入内容');
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : '正文片段抓取失败，请检查授权配置。');
    } finally {
      setContentLoading(false);
    }
  }

  async function generateAiSummaryDraft() {
    if (!selectedCandidate) {
      return;
    }
    setAiSummaryLoading(true);
    setErrorMessage('');
    setSuccessMessage('');
    try {
      const task = await generateCandidateAiSummary(selectedCandidate.id);
      setCandidates((items) =>
        items.map((item) => (item.id === selectedCandidate.id ? { ...item, aiSummaryTask: task } : item))
      );
      setSuccessMessage(task.status === 'SUCCESS' ? 'AI 摘要已生成' : 'AI 摘要任务已记录');
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'AI 摘要生成失败，请稍后重试。');
    } finally {
      setAiSummaryLoading(false);
    }
  }

  async function applyAiSummaryDraft() {
    if (!selectedCandidate?.aiSummaryTask) {
      return;
    }
    setAiSummaryLoading(true);
    setErrorMessage('');
    setSuccessMessage('');
    try {
      const task = await applyCandidateAiSummary(selectedCandidate.id, selectedCandidate.aiSummaryTask.id);
      setCandidates((items) =>
        items.map((item) =>
          item.id === selectedCandidate.id
            ? {
                ...item,
                aiSummary: task.generatedSummary ?? item.aiSummary,
                keyPoints: [...task.generatedKeyPoints],
                impactAnalysis: task.generatedImpactAnalysis ?? item.impactAnalysis,
                aiSummaryTask: task
              }
            : item
        )
      );
      setSuccessMessage('AI 摘要草稿已采用');
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'AI 摘要草稿采用失败，请稍后重试。');
    } finally {
      setAiSummaryLoading(false);
    }
  }

  async function runReportAssetAction(asset: ReportAsset, action: 'cache' | 'approve' | 'reject') {
    if (!selectedCandidate) {
      return;
    }
    const loadingKey = `${asset.id}:${action}`;
    setAssetActionLoading(loadingKey);
    setErrorMessage('');
    setSuccessMessage('');
    try {
      const updatedAsset =
        action === 'cache'
          ? await cacheCandidateReportAsset(selectedCandidate.id, asset.id)
          : action === 'approve'
            ? await approveCandidateReportAsset(selectedCandidate.id, asset.id, '授权公开 PDF 可发布')
            : await rejectCandidateReportAsset(selectedCandidate.id, asset.id, '只保留原文链接');
      setCandidates((items) =>
        items.map((item) =>
          item.id === selectedCandidate.id
            ? {
                ...item,
                reportAssets: item.reportAssets.map((currentAsset) =>
                  currentAsset.id === updatedAsset.id ? updatedAsset : currentAsset
                )
              }
            : item
        )
      );
      setSuccessMessage(
        action === 'cache' ? 'PDF 文件已缓存' : action === 'approve' ? 'PDF 资产已审批' : 'PDF 资产已拒绝'
      );
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'PDF 资产操作失败，请检查授权配置。');
    } finally {
      setAssetActionLoading('');
    }
  }

  return (
    <main className="workspace">
      <section className="workspace-main">
        <div className="page-heading">
          <div>
            <p className="page-kicker">内容运营</p>
            <h1>候选资讯审核</h1>
            <p>审核真实采集内容，确认摘要、来源和授权边界后发布到 Flutter 用户端。</p>
          </div>
          <button className="icon-button" aria-label="刷新候选资讯" disabled={isLoading} onClick={refreshCandidates}>
            <RefreshCw size={18} className={isLoading ? 'spin-icon' : undefined} />
          </button>
        </div>

        <div className="metric-grid" aria-label="候选统计">
          <Metric label="今日采集" value={metrics.fetched} />
          <Metric label="待审核" value={metrics.pending} tone="warning" />
          <Metric label="发布数" value={metrics.published} tone="success" />
          <Metric label="已拒绝" value={metrics.rejected} tone="danger" />
        </div>

        <div className="panel">
          <div className="toolbar">
            <div className="filter-tabs" aria-label="候选状态筛选">
              {filters.map((item) => (
                <button
                  key={item.value}
                  type="button"
                  aria-label={item.ariaLabel}
                  aria-pressed={filter === item.value}
                  className={filter === item.value ? 'filter-tab active' : 'filter-tab'}
                  onClick={() => setFilter(item.value)}
                >
                  <span aria-hidden="true">{item.label}</span>
                </button>
              ))}
            </div>
            <label className="search-box">
              <Search size={16} />
              <input placeholder="搜索标题、来源或分类" />
            </label>
          </div>

          <div className="candidate-table" role="table" aria-label="候选资讯列表">
            <div className="table-row table-head" role="row">
              <span role="columnheader">标题</span>
              <span role="columnheader">来源</span>
              <span role="columnheader">分类</span>
              <span role="columnheader">状态</span>
              <span role="columnheader">操作</span>
            </div>
            {isLoading ? (
              <p className="table-state">正在加载候选资讯...</p>
            ) : filteredCandidates.length === 0 ? (
              <p className="table-state">当前筛选暂无候选资讯。</p>
            ) : (
              filteredCandidates.map((candidate) => (
              <button
                className={selectedCandidate?.id === candidate.id ? 'table-row selected' : 'table-row'}
                key={candidate.id}
                onClick={() => selectCandidate(candidate)}
                role="row"
                type="button"
              >
                <span className="title-cell" role="cell">
                  {candidate.title}
                  <small>{candidate.summary}</small>
                </span>
                <span role="cell">{candidate.sourceName}</span>
                <span role="cell">{candidate.categoryName}</span>
                <span role="cell">
                  <StatusChip status={candidate.status} />
                </span>
                <span className="row-action" role="cell">查看</span>
              </button>
              ))
            )}
          </div>
        </div>
        {errorMessage ? <p className="inline-error">{errorMessage}</p> : null}
        {successMessage ? <p className="inline-success">{successMessage}</p> : null}
      </section>

      <aside className="detail-panel" aria-label="候选详情">
        {selectedCandidate ? (
          <>
            <div className="detail-header">
              <StatusChip status={selectedCandidate.status} />
              <a href={selectedCandidate.originalUrl} aria-label="打开原文">
                <ExternalLink size={18} />
              </a>
            </div>
            <h2>{selectedCandidate.title} · 详情</h2>
            <p className="detail-meta">
              {selectedCandidate.sourceName} · {selectedCandidate.categoryName} · {selectedCandidate.fetchedAt}
            </p>

            <section className="detail-section">
              <h3>审核编辑</h3>
              <div className="digest-form-grid candidate-form-grid">
                <label className="field-label">
                  候选标题
                  <input
                    value={draftTitle}
                    onChange={(event) => {
                      setDraftTitle(event.target.value);
                      setIsDraftDirty(true);
                    }}
                    disabled={selectedCandidate.status !== 'PENDING_REVIEW' || actionLoading}
                  />
                </label>
                <label className="field-label">
                  候选分类
                  <select
                    value={draftCategoryCode}
                    onChange={(event) => {
                      setDraftCategoryCode(event.target.value);
                      setIsDraftDirty(true);
                    }}
                    disabled={selectedCandidate.status !== 'PENDING_REVIEW' || actionLoading}
                  >
                    {categoryOptions.map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="field-label">
                  候选来源
                  <input
                    value={draftSourceName}
                    onChange={(event) => {
                      setDraftSourceName(event.target.value);
                      setIsDraftDirty(true);
                    }}
                    disabled={selectedCandidate.status !== 'PENDING_REVIEW' || actionLoading}
                  />
                </label>
                <label className="field-label">
                  候选标签
                  <input
                    value={draftTagText}
                    onChange={(event) => {
                      setDraftTagText(event.target.value);
                      setIsDraftDirty(true);
                    }}
                    disabled={selectedCandidate.status !== 'PENDING_REVIEW' || actionLoading}
                    placeholder="用逗号分隔，如 AI 基建，算力"
                  />
                </label>
                <label className="field-label wide">
                  候选摘要
                  <textarea
                    value={draftSummary}
                    onChange={(event) => {
                      setDraftSummary(event.target.value);
                      setIsDraftDirty(true);
                    }}
                    disabled={selectedCandidate.status !== 'PENDING_REVIEW' || actionLoading}
                    rows={4}
                  />
                </label>
              </div>
              <button
                className="secondary-action"
                disabled={selectedCandidate.status !== 'PENDING_REVIEW' || actionLoading}
                onClick={saveCandidateDraft}
                type="button"
              >
                <Save size={18} />
                保存候选内容
              </button>
            </section>

            <section className="detail-section">
              <h3>运营标签</h3>
              {selectedCandidate.tagNames.length > 0 ? (
                <div className="tag-list" aria-label="候选运营标签">
                  {selectedCandidate.tagNames.map((tagName) => (
                    <span className="tag-chip" key={tagName}>
                      {tagName}
                    </span>
                  ))}
                </div>
              ) : (
                <p className="muted">暂无运营标签。</p>
              )}
            </section>

            <section className="detail-section">
              <div className="section-heading-inline">
                <h3>AI 摘要草稿</h3>
                <button
                  className="secondary-action compact"
                  disabled={selectedCandidate.status !== 'PENDING_REVIEW' || aiSummaryLoading}
                  onClick={generateAiSummaryDraft}
                  type="button"
                >
                  <Sparkles size={18} />
                  {selectedCandidate.aiSummaryTask ? '重新生成' : aiSummaryLoading ? '生成中...' : '生成 AI 摘要'}
                </button>
              </div>
              {selectedCandidate.aiSummaryTask ? (
                <div className="content-preview">
                  <p className="detail-meta">
                    {selectedCandidate.aiSummaryTask.status} · {selectedCandidate.aiSummaryTask.inputSourceType}
                    {' · '}
                    {selectedCandidate.aiSummaryTask.providerType}/{selectedCandidate.aiSummaryTask.modelName}
                    {selectedCandidate.aiSummaryTask.finishedAt ? ` · ${selectedCandidate.aiSummaryTask.finishedAt}` : ''}
                  </p>
                  {selectedCandidate.aiSummaryTask.generatedSummary ? (
                    <p>{selectedCandidate.aiSummaryTask.generatedSummary}</p>
                  ) : null}
                  {selectedCandidate.aiSummaryTask.generatedKeyPoints.length > 0 ? (
                    <ul className="compact-list">
                      {selectedCandidate.aiSummaryTask.generatedKeyPoints.map((point) => (
                        <li key={point}>{point}</li>
                      ))}
                    </ul>
                  ) : null}
                  {selectedCandidate.aiSummaryTask.errorMessage ? (
                    <p className="inline-error">{selectedCandidate.aiSummaryTask.errorMessage}</p>
                  ) : null}
                  <button
                    className="secondary-action compact"
                    disabled={
                      selectedCandidate.status !== 'PENDING_REVIEW'
                      || aiSummaryLoading
                      || selectedCandidate.aiSummaryTask.status !== 'SUCCESS'
                    }
                    onClick={applyAiSummaryDraft}
                    type="button"
                  >
                    <CheckCircle2 size={16} />
                    采用草稿
                  </button>
                </div>
              ) : (
                <p>{selectedCandidate.aiSummary}</p>
              )}
              {selectedCandidate.impactAnalysis ? <p>{selectedCandidate.impactAnalysis}</p> : null}
            </section>

            <section className="detail-section">
              <h3>来源摘要</h3>
              <p>{selectedCandidate.summary}</p>
            </section>

            <section className="detail-section">
              <div className="section-heading-inline">
                <h3>授权正文</h3>
                <button
                  className="secondary-action compact"
                  disabled={selectedCandidate.status !== 'PENDING_REVIEW' || contentLoading}
                  onClick={fetchAuthorizedSnippet}
                  type="button"
                >
                  <FileText size={18} />
                  {contentLoading ? '抓取中...' : '抓取正文片段'}
                </button>
              </div>
              {selectedCandidate.content ? (
                <div className="content-preview">
                  {selectedCandidate.content.fetchStatus === 'SUCCESS' ? (
                    <p className="inline-success">授权正文片段已抓取</p>
                  ) : null}
                  <p className="detail-meta">
                    {selectedCandidate.content.captureMode} · {selectedCandidate.content.fetchStatus}
                    {selectedCandidate.content.fetchedAt ? ` · ${selectedCandidate.content.fetchedAt}` : ''}
                  </p>
                  {selectedCandidate.content.preview ? <p>{selectedCandidate.content.preview}</p> : null}
                  {selectedCandidate.content.errorMessage ? (
                    <p className="inline-error">{selectedCandidate.content.errorMessage}</p>
                  ) : null}
                  {selectedCandidate.content.licensePolicy ? (
                    <p className="muted">
                      {selectedCandidate.content.licensePolicy}
                      {selectedCandidate.content.licenseNote ? ` · ${selectedCandidate.content.licenseNote}` : ''}
                    </p>
                  ) : null}
                </div>
              ) : (
                <p className="muted">尚未抓取授权正文片段。</p>
              )}
            </section>

            <section className="detail-section">
              <h3>PDF 资产</h3>
              {selectedCandidate.reportAssets.length > 0 ? (
                selectedCandidate.reportAssets.map((asset) => (
                  <div className="asset-row" key={asset.id}>
                    <FileText size={18} />
                    <div className="asset-copy">
                      <span>{asset.fileName}</span>
                      <small>{asset.title}</small>
                      <small>
                        {asset.licensePolicy} · {asset.mimeType ?? 'MIME 待缓存'}
                        {asset.fileSizeBytes ? ` · ${formatBytes(asset.fileSizeBytes)}` : ''}
                      </small>
                      {asset.licenseNote ? <small>{asset.licenseNote}</small> : null}
                      {asset.cacheErrorMessage ? <small className="asset-error">{asset.cacheErrorMessage}</small> : null}
                    </div>
                    <div className="asset-state">
                      <b>{asset.status}</b>
                      <b className={asset.cacheStatus === 'SUCCESS' ? 'success' : undefined}>
                        {asset.cacheStatus ?? 'NOT_CACHED'}
                      </b>
                    </div>
                    <div className="asset-actions">
                      <a href={asset.originalUrl} aria-label={`打开 PDF ${asset.fileName}`}>
                        <ExternalLink size={16} />
                      </a>
                      <button
                        className="secondary-action compact"
                        disabled={
                          selectedCandidate.status !== 'PENDING_REVIEW'
                          || Boolean(assetActionLoading)
                          || asset.cacheStatus === 'SUCCESS'
                        }
                        onClick={() => runReportAssetAction(asset, 'cache')}
                        type="button"
                      >
                        <Download size={16} />
                        缓存 PDF
                      </button>
                      <button
                        className="secondary-action compact"
                        disabled={
                          selectedCandidate.status !== 'PENDING_REVIEW'
                          || Boolean(assetActionLoading)
                          || asset.cacheStatus !== 'SUCCESS'
                          || asset.status === 'APPROVED'
                        }
                        onClick={() => runReportAssetAction(asset, 'approve')}
                        type="button"
                      >
                        <CheckCircle2 size={16} />
                        审批 PDF
                      </button>
                      <button
                        className="secondary-action compact danger"
                        disabled={
                          selectedCandidate.status !== 'PENDING_REVIEW'
                          || Boolean(assetActionLoading)
                          || asset.status === 'REJECTED'
                        }
                        onClick={() => runReportAssetAction(asset, 'reject')}
                        type="button"
                      >
                        <XCircle size={16} />
                        拒绝 PDF
                      </button>
                    </div>
                  </div>
                ))
              ) : (
                <p className="muted">暂无授权 PDF 元数据。</p>
              )}
            </section>

            <div className="detail-actions">
              <button
                className="primary-action"
                disabled={selectedCandidate.status !== 'PENDING_REVIEW' || actionLoading}
                onClick={() => updateStatus('PUBLISHED')}
                type="button"
              >
                <CheckCircle2 size={18} />
                发布为文章
              </button>
              <button
                className="secondary-action danger"
                disabled={selectedCandidate.status !== 'PENDING_REVIEW' || actionLoading}
                onClick={() => updateStatus('REJECTED')}
                type="button"
              >
                <XCircle size={18} />
                拒绝候选
              </button>
            </div>
          </>
        ) : (
          <p className="muted">暂无候选资讯。</p>
        )}
      </aside>
    </main>
  );
}

function parseTagText(value: string): string[] {
  const tags = value
    .split(/[,，、;\n]+/)
    .map((item) => item.trim())
    .filter(Boolean);
  return Array.from(new Set(tags));
}

function formatBytes(value: number): string {
  if (value >= 1024 * 1024) {
    return `${(value / 1024 / 1024).toFixed(1)} MB`;
  }
  if (value >= 1024) {
    return `${Math.round(value / 1024)} KB`;
  }
  return `${value} B`;
}

function Metric({ label, value, tone }: { label: string; value: number; tone?: string }) {
  return (
    <div className={`metric-card ${tone ?? ''}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function StatusChip({ status }: { status: CandidateStatus }) {
  return <span className={`status-chip ${candidateStatusTone(status)}`}>{candidateStatusText(status)}</span>;
}
