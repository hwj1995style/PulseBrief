import { AlertTriangle, CheckCircle2, DatabaseZap, RefreshCw, RadioTower } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import {
  getTodayIngestionMetrics,
  listIngestionJobs,
  listIngestionSources,
  updateIngestionSourceEnabled
} from '../../shared/api/adminApi';
import type { AdminIngestionJob, AdminIngestionMetrics, AdminIngestionSource, IngestionJobStatus } from '../../shared/types/ingestion';

const emptyMetrics: AdminIngestionMetrics = {
  fetchedCount: 0,
  candidateCount: 0,
  publishedCount: 0,
  failedCount: 0
};

const statusLabels: Record<IngestionJobStatus, string> = {
  RUNNING: '运行中',
  SUCCESS: '成功',
  PARTIAL_SUCCESS: '部分成功',
  FAILED: '失败'
};

export function IngestionMonitorPage() {
  const [statusFilter, setStatusFilter] = useState<IngestionJobStatus | 'ALL'>('ALL');
  const [jobs, setJobs] = useState<AdminIngestionJob[]>([]);
  const [metrics, setMetrics] = useState<AdminIngestionMetrics>(emptyMetrics);
  const [sources, setSources] = useState<AdminIngestionSource[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);
  const [updatingSourceId, setUpdatingSourceId] = useState<number | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    Promise.all([
      listIngestionJobs(statusFilter),
      getTodayIngestionMetrics(),
      listIngestionSources()
    ])
      .then(([nextJobs, nextMetrics, nextSources]) => {
        if (cancelled) {
          return;
        }
        setJobs(nextJobs);
        setMetrics(nextMetrics);
        setSources(nextSources);
      })
      .catch((nextError: Error) => {
        if (!cancelled) {
          setError(nextError.message || '采集监控数据加载失败');
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [refreshKey, statusFilter]);

  const enabledSourceCount = useMemo(
    () => sources.filter((source) => source.enabled).length,
    [sources]
  );

  function handleToggleSource(source: AdminIngestionSource) {
    setUpdatingSourceId(source.id);
    setError(null);
    updateIngestionSourceEnabled(source.id, !source.enabled)
      .then((updatedSource) => {
        setSources((currentSources) =>
          currentSources.map((item) => (item.id === updatedSource.id ? updatedSource : item))
        );
      })
      .catch((nextError: Error) => {
        setError(nextError.message || '采集源状态更新失败');
      })
      .finally(() => {
        setUpdatingSourceId(null);
      });
  }

  return (
    <main className="workspace ingestion-workspace">
      <section className="workspace-main">
        <div className="page-heading">
          <div>
            <p className="page-kicker">运营质量与监控</p>
            <h1>采集任务</h1>
            <p>查看真实资讯采集结果、失败原因和今日发布链路表现。</p>
          </div>
          <button className="secondary-action" onClick={() => setRefreshKey((current) => current + 1)} type="button">
            <RefreshCw size={16} />
            刷新
          </button>
        </div>

        <div className="metric-grid">
          <MetricCard icon={<DatabaseZap size={18} />} label="今日采集" value={metrics.fetchedCount} />
          <MetricCard icon={<RadioTower size={18} />} label="候选生成" value={metrics.candidateCount} />
          <MetricCard icon={<CheckCircle2 size={18} />} label="已发布" tone="success" value={metrics.publishedCount} />
          <MetricCard icon={<AlertTriangle size={18} />} label="失败任务" tone="danger" value={metrics.failedCount} />
        </div>

        <section className="panel ingestion-panel">
          <div className="toolbar">
            <div className="filter-tabs" role="group" aria-label="采集任务筛选">
              <button
                aria-pressed={statusFilter === 'ALL'}
                className={statusFilter === 'ALL' ? 'filter-tab active' : 'filter-tab'}
                onClick={() => setStatusFilter('ALL')}
                type="button"
              >
                全部
              </button>
              <button
                aria-pressed={statusFilter === 'FAILED'}
                className={statusFilter === 'FAILED' ? 'filter-tab active' : 'filter-tab'}
                onClick={() => setStatusFilter('FAILED')}
                type="button"
              >
                失败任务
              </button>
            </div>
            <span className="muted">已启用来源 {enabledSourceCount} 个</span>
          </div>

          <div className="candidate-table ingestion-table" role="table" aria-label="采集任务列表">
            <div className="table-row table-head" role="row">
              <span role="columnheader">采集源</span>
              <span role="columnheader">状态</span>
              <span role="columnheader">计数</span>
              <span role="columnheader">时间</span>
              <span role="columnheader">错误信息</span>
            </div>
            {loading ? <p className="table-state">正在加载采集任务...</p> : null}
            {!loading && jobs.length === 0 ? <p className="table-state">暂无采集任务</p> : null}
            {!loading
              ? jobs.map((job) => (
                  <div className="table-row ingestion-row" key={job.id} role="row">
                    <span className="title-cell" role="cell">
                      {job.sourceCode}
                      <small>{job.triggerType}</small>
                    </span>
                    <span className={statusClass(job.status)} role="cell">
                      {statusLabels[job.status]}
                    </span>
                    <span role="cell">
                      拉取 {job.fetchedCount} / 新增 {job.newCount} / 重复 {job.duplicateCount}
                    </span>
                    <span className="muted" role="cell">
                      {formatTime(job.startedAt)}
                    </span>
                    <span className={job.errorMessage ? 'error-cell' : 'muted'} role="cell">
                      {job.errorMessage || '无'}
                    </span>
                  </div>
                ))
              : null}
          </div>
        </section>

        {error ? <p className="inline-error">{error}</p> : null}
      </section>

      <aside className="detail-panel ingestion-sources" aria-label="采集源配置">
        <div className="detail-header">
          <div>
            <p className="page-kicker">Source Controls</p>
            <h2>采集源状态</h2>
          </div>
          <span className="status-chip success">{enabledSourceCount}/{sources.length} 启用</span>
        </div>

        <div className="source-list">
          {sources.map((source) => (
            <article className="source-card" key={source.id}>
              <div>
                <strong>{source.name}</strong>
                <span>{source.code}</span>
              </div>
              <span className={source.enabled ? 'status-chip success' : 'status-chip warning'}>
                {source.enabled ? '启用' : '停用'}
              </span>
              <p>
                {source.contentAccessPolicy} · 最新 {source.maxAgeHours ?? '-'} 小时
              </p>
              <small>
                {source.allowPdfDownload ? '允许 PDF' : '不下载 PDF'} ·{' '}
                {source.allowFullText ? '允许全文' : '摘要优先'}
              </small>
              <button
                aria-label={`${source.enabled ? '停用' : '启用'} ${source.name}`}
                className={source.enabled ? 'secondary-action source-toggle danger' : 'secondary-action source-toggle'}
                disabled={updatingSourceId === source.id}
                onClick={() => handleToggleSource(source)}
                type="button"
              >
                {source.enabled ? '停用来源' : '启用来源'}
              </button>
            </article>
          ))}
        </div>
      </aside>
    </main>
  );
}

function MetricCard({
  icon,
  label,
  value,
  tone
}: {
  icon: ReactNode;
  label: string;
  value: number;
  tone?: 'success' | 'danger';
}) {
  return (
    <article className={tone ? `metric-card ${tone}` : 'metric-card'}>
      <span>
        {icon}
        {label}
      </span>
      <strong>{value}</strong>
    </article>
  );
}

function statusClass(status: IngestionJobStatus) {
  if (status === 'FAILED') {
    return 'status-chip danger';
  }
  if (status === 'SUCCESS') {
    return 'status-chip success';
  }
  return 'status-chip warning';
}

function formatTime(value: string) {
  if (!value) {
    return '-';
  }
  return value.replace('T', ' ').slice(0, 16);
}
