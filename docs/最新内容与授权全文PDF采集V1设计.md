# PulseBrief 最新内容与授权全文 PDF 采集 V1 设计

## 背景与现状

PulseBrief 已完成真实资讯采集的基础链路：采集适配层、原始资讯池、去重、候选资讯生成和 Admin 审核发布 API 设计。原设计为了控制版权和合规风险，V1 默认只采集标题、摘要、来源、发布时间和原文链接，不抓取新闻全文或研报 PDF。

现在产品方向调整为：V1 希望尽量获取更完整的内容信息，并支持研报 PDF 下载能力，但必须只采集最新内容，不能做历史批量回溯，也不能抓取未授权、付费、登录后可见或禁止转载的内容。

因此本设计将采集策略调整为“最新优先 + 授权优先 + 候选审核优先”：

1. 只采集最新窗口内的内容。
2. 默认仍采集标题、摘要和原文链接。
3. 只有明确允许的公开来源，才采集正文片段、授权全文或授权 PDF。
4. 所有全文和 PDF 都必须先进入候选池，由 Admin 审核后才能发布或展示。

## 目标与非目标

目标：

1. 明确 V1 只采集最新内容，不做历史批量回溯。
2. 设计最新内容窗口、去重窗口、抓取频率和采集上限。
3. 设计授权全文和授权 PDF 的白名单来源策略。
4. 设计全文、正文片段、PDF 元数据和文件缓存的字段边界。
5. 设计 Admin 审核时对全文和 PDF 的合规确认流程。
6. 设计用户端展示边界：优先展示摘要、结构化要点和原文入口，PDF 下载只对授权内容开放。

非目标：

1. 不采集历史归档内容。
2. 不批量下载历史研报库。
3. 不抓取付费研报、券商内部研报、登录后 PDF、会员内容或需要绕过限制的内容。
4. 不绕过 robots、反爬、验证码、登录墙或下载限制。
5. 不对未授权新闻网页做全文转载。
6. 不把 PDF 作为默认用户端能力；必须经过来源授权和 Admin 审核。
7. 不在 V1 做 OCR、复杂 PDF 解析或大模型全文总结。

## 最新内容采集边界

V1 使用固定时间窗口控制采集范围。

建议默认配置：

```text
PULSEBRIEF_INGESTION_MAX_AGE_HOURS=24
PULSEBRIEF_INGESTION_PDF_MAX_AGE_HOURS=72
PULSEBRIEF_INGESTION_MAX_ITEMS_PER_SOURCE=50
PULSEBRIEF_INGESTION_ALLOW_BACKFILL=false
```

规则：

1. 新闻、快讯、观点类内容默认只采集最近 24 小时。
2. 官方报告、公开 PDF 和机构观点类内容可放宽到最近 72 小时。
3. 任何 Provider 都必须按 `publishedAt >= now - maxAgeHours` 过滤。
4. 如果外部源缺失发布时间，V1 默认不采集全文或 PDF，只可进入低优先级标题候选。
5. 不提供历史日期选择、不提供全量同步、不提供从第一页翻到历史页的批量抓取。
6. 手动触发采集也必须遵守最新窗口。

## 来源许可策略

每个采集源必须声明 `content_access_policy`：

| 策略 | 说明 | V1 行为 |
| --- | --- | --- |
| `SUMMARY_ONLY` | 只允许标题、摘要、链接 | 不抓全文，不下载 PDF |
| `SNIPPET_ALLOWED` | 允许短摘录 | 只保存正文片段 |
| `FULLTEXT_ALLOWED` | 明确允许全文使用 | 可保存授权全文 |
| `PDF_ALLOWED` | 明确允许 PDF 下载或缓存 | 可下载授权 PDF |
| `LINK_ONLY` | 只允许链接跳转 | 只保存标题和链接 |
| `UNKNOWN` | 授权不明确 | 按 `LINK_ONLY` 处理 |

许可来源示例：

1. 官方公开新闻稿。
2. 政府、央行、交易所、监管机构公开公告。
3. 明确可下载的公开报告。
4. 具有开放许可或明确授权的 RSS/API。
5. 自有内容或已签约数据源。

禁止来源示例：

1. 付费新闻全文。
2. 券商、投行、数据库供应商的登录后研报。
3. 标注禁止转载、禁止商业使用、禁止下载的网页或 PDF。
4. 需要验证码、Cookie、Token 或绕过访问控制的内容。
5. 来源许可不明但内容较长的网页全文。

## 数据模型调整建议

### news_ingestion_source 扩展

建议新增字段：

| 字段 | 说明 |
| --- | --- |
| `content_access_policy` | 内容访问策略 |
| `max_age_hours` | 当前源最新内容窗口 |
| `allow_pdf_download` | 是否允许下载 PDF |
| `allow_full_text` | 是否允许保存全文 |
| `license_note` | 来源授权说明 |

### raw_news_item 扩展

建议新增字段：

| 字段 | 说明 |
| --- | --- |
| `content_capture_mode` | `LINK_ONLY/SUMMARY/SNIPPET/FULLTEXT/PDF` |
| `content_text` | 授权正文或正文片段 |
| `content_text_hash` | 正文去重哈希 |
| `content_license` | 内容许可说明 |
| `content_captured_at` | 正文采集时间 |

### candidate_article 扩展

建议新增字段：

| 字段 | 说明 |
| --- | --- |
| `content_preview` | Admin 可审核的正文片段 |
| `has_full_text` | 是否有授权全文 |
| `has_pdf` | 是否有关联 PDF |
| `compliance_status` | `PENDING/APPROVED/REJECTED` |
| `compliance_note` | 审核备注 |

### 新增 report_asset

研报 PDF 建议独立建表，避免和文章字段混在一起。

| 字段 | 说明 |
| --- | --- |
| `id` | 主键 |
| `candidate_article_id` | 候选资讯 ID |
| `source_code` | 采集源 |
| `title` | PDF 标题 |
| `original_url` | PDF 原始链接 |
| `file_name` | 本地缓存文件名 |
| `file_size_bytes` | 文件大小 |
| `file_hash` | 文件哈希 |
| `license_policy` | PDF 授权策略 |
| `asset_status` | `PENDING_REVIEW/APPROVED/REJECTED/PUBLISHED` |
| `downloaded_at` | 下载时间 |
| `created_at` | 创建时间 |
| `updated_at` | 更新时间 |

V1 文件存储建议先使用本地 `D:\Projects\PulseBrief\data\reports` 或 Docker volume，后续再切换对象存储。Git 不提交 PDF 文件。

## 采集流程

```text
Provider 拉取最新列表
→ 按发布时间窗口过滤
→ 判断来源许可
→ 拉取标题/摘要/链接
→ 若许可允许，抓取正文片段、全文或 PDF
→ 写入 raw_news_item / report_asset
→ 去重
→ 生成 candidate_article
→ Admin 审核内容和许可
→ 发布为 news_article / 授权 PDF 入口
```

关键规则：

1. 全文和 PDF 的采集必须发生在最新窗口内。
2. PDF 下载前必须校验来源策略为 `PDF_ALLOWED`。
3. 全文保存前必须校验来源策略为 `FULLTEXT_ALLOWED`。
4. 未授权内容只保存标题、摘要、原文链接。
5. 候选发布时必须带上合规审核状态。

## Admin 审核影响

Admin 候选详情需要新增：

1. 内容许可策略。
2. 来源授权备注。
3. 正文片段预览。
4. PDF 文件元数据。
5. 原始链接和 PDF 原始链接。
6. 合规审核动作：通过、拒绝、仅保留原文链接。

发布规则：

1. `SUMMARY_ONLY` 或 `LINK_ONLY`：只能发布摘要和原文入口。
2. `SNIPPET_ALLOWED`：可发布短摘录和结构化摘要。
3. `FULLTEXT_ALLOWED`：可发布授权全文，但 V1 用户端仍优先展示摘要和要点。
4. `PDF_ALLOWED`：可展示 PDF 下载入口或跳转入口。
5. 合规未通过时不能发布 PDF 下载入口。

## 用户端展示边界

Flutter V1 建议保持资讯摘要产品定位：

1. 首页、分类页仍展示标题、摘要、来源、发布时间和播放入口。
2. 详情页仍优先展示 AI 摘要、核心要点、可能影响和原文入口。
3. 对授权全文，可增加“阅读全文”折叠区，但默认不展开。
4. 对授权 PDF，可增加“下载公开报告”或“查看公开 PDF”按钮。
5. 对未授权来源，只展示“阅读原文”跳转。

## 测试与回归方案

后端测试：

1. 发布时间早于窗口的内容不会入库。
2. 缺少发布时间的 PDF 不会下载。
3. `SUMMARY_ONLY` 来源不会保存全文或 PDF。
4. `FULLTEXT_ALLOWED` 来源可保存授权全文。
5. `PDF_ALLOWED` 来源可生成 `report_asset`。
6. 重复 PDF 通过 `file_hash` 去重。
7. 未通过合规审核的 PDF 不会出现在用户端。
8. 手动触发采集也必须遵守最新窗口。

Admin 测试：

1. 候选详情展示许可策略和 PDF 元数据。
2. 合规未通过时发布按钮禁用或返回 `409`。
3. Admin 可选择“仅保留原文链接”发布。

Flutter 测试：

1. 未授权内容不展示 PDF 下载按钮。
2. 授权 PDF 内容展示下载或查看入口。
3. 详情页仍不展示未授权全文。

## 风险与分阶段落地建议

风险：

1. 全文和 PDF 最容易触发版权风险，必须以来源授权为准。
2. PDF 文件可能体积较大，需要限流、大小限制和存储清理。
3. 研报时效强，过期内容不应进入 V1 采集。
4. 来源许可可能变化，需要可配置、可关闭、可回退。
5. 用户端若直接展示全文，会偏离“简报摘要”定位。

分阶段建议：

1. 先实现最新窗口过滤和来源许可字段。
2. 再实现公开官方来源的正文片段采集。
3. 再实现授权 PDF 元数据入库，不立即开放下载。
4. 再实现 PDF 下载缓存和 Admin 合规审核。
5. 最后在 Flutter 详情页开放授权 PDF 入口。

## 当前决策

1. V1 只采集最新内容，不做历史批量回溯。
2. 新闻默认最新窗口为 24 小时。
3. 官方报告和公开 PDF 默认最新窗口为 72 小时。
4. 全文和 PDF 必须来源授权明确。
5. 未授权内容只保留标题、摘要、来源、发布时间和原文链接。
6. PDF 文件不提交 Git，只存本地数据目录或后续对象存储。
