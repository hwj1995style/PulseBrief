import { AlertTriangle, CheckCircle2, ClipboardList, DatabaseZap, RefreshCw, RadioTower, ShieldAlert } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import {
  getTodayIngestionMetrics,
  listIngestionAnomalies,
  listIngestionJobs,
  listOperationLogs,
  listIngestionSources,
  runIngestionSource,
  updateIngestionSourceEnabled
} from '../../shared/api/adminApi';
import type {
  AdminIngestionJob,
  AdminIngestionMetrics,
  AdminIngestionSource,
  AdminIngestionAnomaly,
  AdminOperationLog,
  IngestionJobStatus
} from '../../shared/types/ingestion';

const emptyMetrics: AdminIngestionMetrics = {
  fetchedCount: 0,
  candidateCount: 0,
  publishedCount: 0,
  failedCount: 0
};

const statusLabels: Record<IngestionJobStatus, string> = {
  RUNNING: '运行中',
  WAITING_RETRY: '等待重试',
  SUCCESS: '成功',
  PARTIAL_SUCCESS: '部分成功',
  FAILED: '失败',
  CANCELLED: '已取消'
};

const operationActionLabels: Record<string, string> = {
  PUBLISH_ARTICLE: '文章发布',
  PUBLISH_DIGEST: '简报发布',
  OFFLINE_DIGEST: '简报下线'
};

const anomalyIssueLabels: Record<string, string> = {
  MISSING_SOURCE: '缺少来源',
  MISSING_ORIGINAL_URL: '缺少原文链接',
  PUBLISHED_AT_MISSING: '缺少发布时间',
  PUBLISHED_AT_IN_FUTURE: '发布时间异常'
};

export function IngestionMonitorPage() {
  const [statusFilter, setStatusFilter] = useState<IngestionJobStatus | 'ALL'>('ALL');
  const [jobs, setJobs] = useState<AdminIngestionJob[]>([]);
  const [metrics, setMetrics] = useState<AdminIngestionMetrics>(emptyMetrics);
  const [sources, setSources] = useState<AdminIngestionSource[]>([]);
  const [anomalies, setAnomalies] = useState<AdminIngestionAnomaly[]>([]);
  const [operationLogs, setOperationLogs] = useState<AdminOperationLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [runMessage, setRunMessage] = useState<string | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);
  const [updatingSourceId, setUpdatingSourceId] = useState<number | null>(null);
  const [runningSourceId, setRunningSourceId] = useState<number | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    Promise.all([
      listIngestionJobs(statusFilter),
      getTodayIngestionMetrics(),
      listIngestionSources(),
      listIngestionAnomalies(),
      listOperationLogs('PUBLISH')
    ])
      .then(([nextJobs, nextMetrics, nextSources, nextAnomalies, nextOperationLogs]) => {
        if (cancelled) {
          return;
        }
        setJobs(nextJobs);
        setMetrics(nextMetrics);
        setSources(nextSources);
        setAnomalies(nextAnomalies);
        setOperationLogs(nextOperationLogs);
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
    setRunMessage(null);
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

  function handleRunSource(source: AdminIngestionSource) {
    setRunningSourceId(source.id);
    setError(null);
    setRunMessage(null);
    runIngestionSource(source.id, { pageSize: 5, generateCandidates: true })
      .then(() => {
        setRunMessage(`手动采集完成：${source.name}`);
        setRefreshKey((current) => current + 1);
      })
      .catch((nextError: Error) => {
        setError(nextError.message || '手动采集失败');
        setRefreshKey((current) => current + 1);
      })
      .finally(() => {
        setRunningSourceId(null);
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
                      <small>{job.triggerType} · 第 {job.attemptCount ?? 1}/{job.maxAttempts ?? 1} 次</small>
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
                      {job.nextRetryAt ? ` · 下次重试 ${formatTime(job.nextRetryAt)}` : ''}
                    </span>
                  </div>
                ))
              : null}
          </div>
        </section>

        <section className="panel operation-log-panel" aria-label="发布操作日志">
          <div className="section-title-row">
            <div>
              <p className="page-kicker">Publish Audit</p>
              <h2>
                <ClipboardList size={18} />
                发布操作日志
              </h2>
            </div>
            <span className="muted">最近 {operationLogs.length} 条</span>
          </div>

          <div className="operation-log-list">
            {loading ? <p className="table-state compact">正在加载发布操作日志...</p> : null}
            {!loading && operationLogs.length === 0 ? <p className="table-state compact">暂无发布操作记录</p> : null}
            {!loading
              ? operationLogs.map((log) => (
                  <article className="operation-log-row" key={log.id}>
                    <div>
                      <strong>{operationActionLabels[log.actionType] ?? log.actionType}</strong>
                      <span>{log.targetTitle}</span>
                    </div>
                    <span className={log.status === 'SUCCESS' ? 'status-chip success' : 'status-chip danger'}>
                      {log.status === 'SUCCESS' ? '成功' : log.status}
                    </span>
                    <p>{log.detail}</p>
                    <small>
                      {log.operatorName}
                      {log.operatorRole ? ` (${log.operatorRole})` : ''} · {formatTime(log.createdAt)}
                    </small>
                  </article>
                ))
              : null}
          </div>
        </section>

        <section className="panel operation-log-panel anomaly-panel" aria-label="异常数据检测">
          <div className="section-title-row">
            <div>
              <p className="page-kicker">Quality Signals</p>
              <h2>
                <ShieldAlert size={18} />
                异常数据检测
              </h2>
            </div>
            <span className="muted">待处理 {anomalies.length} 条</span>
          </div>

          <div className="operation-log-list">
            {loading ? <p className="table-state compact">正在加载异常数据...</p> : null}
            {!loading && anomalies.length === 0 ? <p className="table-state compact">暂无异常数据</p> : null}
            {!loading
              ? anomalies.map((anomaly) => (
                  <article className="operation-log-row anomaly-row" key={`${anomaly.rawNewsItemId}-${anomaly.issueType}`}>
                    <div>
                      <strong>{anomaly.title}</strong>
                      <span>{anomalyIssueLabels[anomaly.issueType] ?? anomaly.issueType}</span>
                    </div>
                    <span className={anomaly.severity === 'HIGH' ? 'status-chip danger' : 'status-chip warning'}>
                      {anomaly.severity === 'HIGH' ? '高危' : '中等'}
                    </span>
                    <p>{anomaly.description}</p>
                    <small>
                      {anomaly.issueType} · {anomaly.sourceCode} · {formatTime(anomaly.fetchedAt ?? '')}
                    </small>
                  </article>
                ))
              : null}
          </div>
        </section>

        {error ? <p className="inline-error">{error}</p> : null}
        {runMessage ? <p className="inline-success">{runMessage}</p> : null}
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
              <small>
                {source.scheduleEnabled
                  ? `每 ${source.scheduleIntervalMinutes ?? 60} 分钟调度 · 下次 ${formatTime(source.nextRunAt ?? '')}`
                  : '定时调度未启用'}
              </small>
              <button
                aria-label={`手动采集 ${source.name}`}
                className="secondary-action source-toggle"
                disabled={!source.enabled || runningSourceId === source.id}
                onClick={() => handleRunSource(source)}
                type="button"
              >
                <RefreshCw className={runningSourceId === source.id ? 'spin-icon' : undefined} size={14} />
                {runningSourceId === source.id ? '采集中' : '手动采集'}
              </button>
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
