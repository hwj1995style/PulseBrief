import { CheckCircle2, ExternalLink, FileText, RefreshCw, Search, XCircle } from 'lucide-react';
import { useMemo, useState } from 'react';
import { getInitialCandidates, updateCandidateStatus } from '../../shared/api/adminApi';
import type { AdminCandidate, CandidateStatus } from '../../shared/types/candidate';
import { candidateStatusText, candidateStatusTone } from './candidateUtils';

const filters: Array<{ label: string; ariaLabel: string; value: CandidateStatus | 'ALL' }> = [
  { label: '全部', ariaLabel: '全部', value: 'ALL' },
  { label: '待审核', ariaLabel: '待审核', value: 'PENDING_REVIEW' },
  { label: '发布', ariaLabel: '已发布', value: 'PUBLISHED' },
  { label: '拒绝', ariaLabel: '已拒绝', value: 'REJECTED' }
];

export function CandidateReviewPage() {
  const [filter, setFilter] = useState<CandidateStatus | 'ALL'>('PENDING_REVIEW');
  const [candidates, setCandidates] = useState<AdminCandidate[]>(() => getInitialCandidates());
  const [selectedId, setSelectedId] = useState<number | null>(() => getInitialCandidates()[0]?.id ?? null);

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

  async function updateStatus(status: CandidateStatus) {
    if (!selectedCandidate) {
      return;
    }
    const updated = await updateCandidateStatus(selectedCandidate.id, status);
    setCandidates((items) => items.map((item) => (item.id === updated.id ? updated : item)));
    setSelectedId(updated.id);
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
          <button className="icon-button" aria-label="刷新候选资讯">
            <RefreshCw size={18} />
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
            {filteredCandidates.map((candidate) => (
              <button
                className={selectedCandidate?.id === candidate.id ? 'table-row selected' : 'table-row'}
                key={candidate.id}
                onClick={() => setSelectedId(candidate.id)}
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
            ))}
          </div>
        </div>
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
              <h3>AI 摘要草稿</h3>
              <p>{selectedCandidate.aiSummary}</p>
            </section>

            <section className="detail-section">
              <h3>来源摘要</h3>
              <p>{selectedCandidate.summary}</p>
            </section>

            <section className="detail-section">
              <h3>PDF 资产</h3>
              {selectedCandidate.reportAssets.length > 0 ? (
                selectedCandidate.reportAssets.map((asset) => (
                  <div className="asset-row" key={asset.id}>
                    <FileText size={18} />
                    <span>{asset.fileName}</span>
                    <b>{asset.status}</b>
                  </div>
                ))
              ) : (
                <p className="muted">暂无授权 PDF 元数据。</p>
              )}
            </section>

            <div className="detail-actions">
              <button
                className="primary-action"
                disabled={selectedCandidate.status !== 'PENDING_REVIEW'}
                onClick={() => updateStatus('PUBLISHED')}
                type="button"
              >
                <CheckCircle2 size={18} />
                发布为文章
              </button>
              <button
                className="secondary-action danger"
                disabled={selectedCandidate.status !== 'PENDING_REVIEW'}
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
