# AI 摘要任务与 Admin 审核入口设计

## 背景与现状

PulseBrief 用户端文章详情已经有 `aiSummary`、`keyPoints` 和 `impactAnalysis` 展示字段，后端 `news_article` 也已保存 `ai_summary`、`key_points` 和 `impact_analysis`。但当前这些内容仍来自种子数据或 Admin 发布请求，真实采集链路尚未接入 AI 摘要服务。

当前采集与审核链路已经具备以下基础：

1. RSS Provider 只采集标题、摘要、来源、发布时间和原文链接。
2. `candidate_article` 作为 Admin 人工审核发布入口。
3. 阶段 25 已新增 `raw_news_content`，可保存授权正文片段或授权全文。
4. 阶段 26 已设计授权 PDF 元数据与文件缓存，但不做 PDF 解析或 AI 摘要。
5. Admin 候选详情目前展示“AI 摘要草稿”，但实际优先使用候选摘要兜底。

阶段 27 的目标是设计独立 AI 摘要任务与 Admin 审核入口，让系统可以在默认无 AI Key、CI 不访问真实 AI 服务的前提下，生成可审核、可编辑、可回退的摘要草稿。AI 摘要不得绕过 Admin 审核直接发布到用户端。

## 目标与非目标

目标：

1. 设计 AI 摘要服务抽象，支持 `MOCK` 和真实 Provider 双模式。
2. 设计摘要任务数据模型，记录输入来源、输入哈希、Provider、模型、状态、错误信息和生成结果。
3. 明确输入边界：RSS 摘要、授权正文片段、授权全文或后续授权 PDF 解析结果。
4. 设计 Admin 候选详情中的摘要预览、编辑、重生成、采用和发布前确认流程。
5. 设计提示词版本、长度限制、合规措辞、失败降级和审计策略。
6. 补充自动化测试、mock Provider 测试和真实 AI Provider 显式开关策略。

非目标：

1. 不在本阶段接入生产真实 AI Key。
2. 不让 CI 访问 OpenAI、云模型或任何外部 AI Provider。
3. 不把未经授权的网页全文、未审核 PDF 或登录后内容发送给 AI 服务。
4. 不把 AI 生成结果自动发布到用户端。
5. 不做复杂分类；分类服务属于阶段 28。
6. 不做 PDF OCR、PDF 文本解析或研报全文摘要；PDF 解析输入属于后续阶段。
7. 不做个性化推荐、向量检索或多轮对话。

## 影响范围

后端：

1. 新增 AI 摘要任务表和任务领域模型。
2. 新增 AI Provider 抽象和 mock Provider。
3. 新增摘要任务编排服务，负责输入选择、授权校验、Prompt 构造、Provider 调用、状态记录和结果保存。
4. 扩展 Admin 候选详情，返回最新 AI 摘要任务和可采用草稿。
5. 新增 Admin 手动生成、重生成、采用或丢弃 AI 摘要草稿接口。

Admin：

1. 候选详情展示 AI 摘要任务状态、输入来源、模型、生成时间、错误信息和草稿内容。
2. 支持手动生成或重生成摘要。
3. 支持将 AI 草稿写入发布表单，也支持人工编辑后发布。
4. 失败时保留候选原摘要，不阻断人工审核发布。

Flutter：

1. 阶段 27 不直接修改用户端 UI。
2. 用户端继续只读取已发布 `news_article` 的 `ai_summary/key_points/impact_analysis`。
3. 未审核 AI 草稿不进入用户端。

配置与部署：

1. 默认使用 `MOCK` Provider 或关闭真实 Provider。
2. 真实 AI Provider 必须显式配置环境变量和密钥。
3. CI 不注入真实 AI Key。

## 数据模型或权限模型

### 输入边界

AI 摘要任务可使用的输入类型：

| 输入类型 | 来源 | 阶段 27 行为 |
| --- | --- | --- |
| `RSS_SUMMARY` | `raw_news_item.title/summary/source/original_url` | 默认可用，适合作为最小摘要输入 |
| `CONTENT_SNIPPET` | `raw_news_content.capture_mode = SNIPPET` 且 `SUCCESS` | 可用，必须保留许可快照 |
| `CONTENT_FULLTEXT` | `raw_news_content.capture_mode = FULLTEXT` 且 `SUCCESS` | 仅 `FULLTEXT_ALLOWED` 且授权说明明确时可用 |
| `PDF_TEXT` | 后续 PDF 解析结果 | 阶段 27 只预留，不实现 |
| `MANUAL_NOTE` | Admin 人工补充内容 | 可作为可选输入，需记录操作人和时间 |

禁止输入：

1. 未授权网页全文。
2. `SUMMARY_ONLY/LINK_ONLY/UNKNOWN` source 的网页正文。
3. 未审核、未授权或缓存失败的 PDF 内容。
4. 需要登录、Cookie、Token、验证码或绕过访问控制获取的内容。
5. 含有密钥、手机号、邮箱、支付账号等敏感信息的文本；后续可增加脱敏过滤。

### 摘要任务模型

建议新增 `ai_summary_task` 表：

| 字段 | 说明 |
| --- | --- |
| `id` | 主键 |
| `candidate_article_id` | 候选资讯 ID |
| `raw_news_item_id` | 原始资讯 ID |
| `input_source_type` | `RSS_SUMMARY/CONTENT_SNIPPET/CONTENT_FULLTEXT/PDF_TEXT/MANUAL_NOTE` |
| `input_ref_id` | 输入记录 ID，例如 `raw_news_content.id`，RSS 摘要可为空 |
| `input_hash` | 输入文本哈希，用于避免重复生成 |
| `input_preview` | 输入预览，便于 Admin 审核 |
| `provider_type` | `MOCK/OPENAI/OTHER` |
| `model_name` | 模型名，mock 可为 `mock-v1` |
| `prompt_version` | Prompt 模板版本 |
| `task_status` | `PENDING/RUNNING/SUCCESS/FAILED/SKIPPED/CANCELLED` |
| `generated_summary` | AI 摘要草稿 |
| `generated_key_points` | AI 要点草稿，JSON 或换行文本 |
| `generated_impact_analysis` | 可能影响草稿 |
| `token_prompt_count` | 输入 token 数，mock 可为 0 |
| `token_completion_count` | 输出 token 数，mock 可为 0 |
| `error_message` | 失败原因 |
| `requested_by` | 触发人，开发态可先用 `dev-admin` |
| `started_at` | 任务开始时间 |
| `finished_at` | 任务结束时间 |
| `created_at/updated_at` | 审计时间 |

候选文章可后续扩展草稿快照字段，但阶段 27 推荐优先使用独立任务表，避免 `candidate_article` 继续膨胀。

### 任务状态机

```text
PENDING
-> RUNNING
-> SUCCESS
-> FAILED
-> SKIPPED
-> CANCELLED
```

状态含义：

1. `PENDING`：任务已创建，等待执行。
2. `RUNNING`：正在调用 Provider。
3. `SUCCESS`：生成成功，可供 Admin 采用或编辑。
4. `FAILED`：Provider、Prompt、输入或网络失败。
5. `SKIPPED`：输入不满足授权或内容不足。
6. `CANCELLED`：后续队列化后用于取消任务。

发布规则：

1. AI 摘要任务成功后只产生草稿，不自动发布。
2. Admin 发布文章时仍通过发布请求提交最终 `aiSummary/keyPoints/impactAnalysis`。
3. 如果没有 AI 结果，发布可继续使用候选摘要兜底。
4. 低质量、失败或被拒绝的 AI 草稿不得进入 `news_article`。

## 后端实现方案

### 服务拆分

建议新增：

```text
AiSummaryProvider
AiSummaryPromptBuilder
AiSummaryInputSelector
AiSummaryTaskService
AiSummaryTaskRepository
```

职责：

1. `AiSummaryProvider`：定义统一生成接口，返回摘要、要点、影响分析和 token 使用量。
2. `MockAiSummaryProvider`：默认测试与本地无 Key 模式，基于输入生成确定性草稿。
3. `AiSummaryPromptBuilder`：按 prompt version 构造输入，限制长度并附带合规要求。
4. `AiSummaryInputSelector`：选择可授权输入，优先级为授权全文、授权片段、RSS 摘要。
5. `AiSummaryTaskService`：编排创建任务、校验输入、调用 Provider、保存结果和失败状态。

### Provider 抽象

建议接口：

```text
generate(AiSummaryRequest request) -> AiSummaryProviderResult
```

请求字段：

1. 标题。
2. 来源名称。
3. 发布时间。
4. 输入正文或摘要。
5. 输入来源类型和许可说明。
6. 输出语言。
7. Prompt version。

响应字段：

1. `summary`：建议 120 到 220 字中文摘要。
2. `keyPoints`：3 到 5 条要点。
3. `impactAnalysis`：1 段可能影响。
4. `modelName`。
5. token 使用量。
6. 原始响应摘要或 request id，避免保存完整敏感响应。

### Prompt 边界

Prompt 目标：

1. 用中文输出。
2. 只基于输入内容，不编造未提供事实。
3. 保留不确定措辞，避免投资建议式表达。
4. 标明“可能影响”，不使用确定收益或买卖建议。
5. 输出结构稳定，便于 Admin 编辑。

长度限制：

1. `RSS_SUMMARY` 输入上限建议 2000 字。
2. `CONTENT_SNIPPET` 输入上限建议 4000 字。
3. `CONTENT_FULLTEXT` 输入上限建议 12000 字，超出时截断并记录。
4. 输出摘要建议 120 到 220 字。
5. 要点建议 3 到 5 条，每条 20 到 60 字。
6. 影响分析建议 80 到 160 字。

### API 设计

候选详情扩展：

```json
{
  "aiSummaryTask": {
    "id": 901,
    "status": "SUCCESS",
    "inputSourceType": "CONTENT_SNIPPET",
    "providerType": "MOCK",
    "modelName": "mock-v1",
    "promptVersion": "candidate-summary-v1",
    "generatedSummary": "AI 基建投资仍处扩张阶段...",
    "generatedKeyPoints": ["云厂商资本开支仍是观察重点"],
    "generatedImpactAnalysis": "相关产业链可能受到市场关注。",
    "errorMessage": null,
    "finishedAt": "2026-06-25T10:00:00"
  }
}
```

生成或重生成：

```http
POST /api/admin/candidates/{id}/ai-summary/generate
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "inputSourceType": "AUTO",
  "providerType": "MOCK",
  "promptVersion": "candidate-summary-v1"
}
```

采用草稿：

```http
POST /api/admin/candidates/{id}/ai-summary/{taskId}/apply
Authorization: Bearer <admin-token>
```

阶段 27 可以选择只返回草稿给前端，由前端填充发布表单；也可以将草稿写入候选级 AI draft 字段。推荐第一批只返回草稿并在 Admin 表单中采用，减少数据模型改动。

错误处理：

1. 候选不存在返回 404。
2. 候选非 `PENDING_REVIEW` 返回 409。
3. 无可授权输入返回 `200 + SKIPPED`，便于 Admin 展示原因。
4. Provider 失败返回 `200 + FAILED` 并记录错误，不影响候选发布。
5. 真实 Provider 未配置 Key 返回 `SKIPPED` 或 422；推荐默认 fallback 到 mock 只在测试环境使用。

## 前端影响

Admin 候选详情新增 AI 摘要任务区块：

1. 展示最新任务状态、输入来源、Provider、模型、Prompt 版本和生成时间。
2. 展示生成摘要、要点和影响分析。
3. 支持“生成 AI 摘要”“重新生成”“采用到发布草稿”“丢弃草稿”。
4. 支持人工编辑 AI 摘要、要点和影响分析后再发布。
5. 失败或跳过时展示原因，并保留候选摘要作为兜底。

当前候选详情已有“AI 摘要草稿”区域，阶段 27 可改造为：

1. 未生成：展示候选摘要兜底和生成按钮。
2. 生成中：展示 loading。
3. 成功：展示 AI 草稿和采用按钮。
4. 失败：展示错误和重试按钮。

Flutter 用户端不展示未审核 AI 草稿。只有 Admin 发布后的 `news_article.ai_summary/key_points/impact_analysis` 可被用户端读取。

## 测试与回归方案

后端自动化测试：

1. 默认无 AI Key 时应用上下文启动成功。
2. `MOCK` Provider 可基于 RSS 摘要生成确定性草稿。
3. `CONTENT_SNIPPET` 成功时优先使用授权正文片段作为输入。
4. `CONTENT_FULLTEXT` 只有授权全文成功且许可说明存在时可用。
5. 未授权正文、未审核 PDF、登录后内容不进入 AI 输入。
6. 输入为空或过短时任务 `SKIPPED`。
7. Provider 抛错时任务 `FAILED`，候选仍可发布。
8. 任务记录保存输入来源、模型、prompt version、状态、错误信息和生成时间。
9. Admin 候选详情返回最新 AI 摘要任务。
10. Admin 发布接口仍使用人工确认后的请求字段，不自动采用最新 AI 结果。

Admin 测试：

1. API client 映射 AI 摘要任务状态和草稿字段。
2. 候选详情展示生成、重新生成和采用按钮。
3. mock client 支持生成成功、失败和采用到草稿。
4. AI 生成失败时页面保留候选摘要兜底。

回归命令建议：

```powershell
cd backend
.\mvnw.cmd "-Dtest=AiSummaryTaskServiceTest,AdminCandidateAiSummaryControllerTest,IngestionSchemaTest" test
.\mvnw.cmd test

cd ..\admin
npm test -- --run adminApi.test.ts App.test.tsx
npm test -- --run
npm run lint
npm run build
```

真实 AI Provider smoke：

1. 必须显式开启 `PULSEBRIEF_AI_LIVE_TEST_ENABLED=true`。
2. 必须提供 `PULSEBRIEF_AI_PROVIDER`、`PULSEBRIEF_AI_MODEL` 和对应 API Key。
3. 只允许使用 fixture 或已授权输入。
4. 单次最多生成 1 条候选摘要。
5. 不提交 prompt 完整输入、模型响应原文、API Key、日志或数据库 dump。
6. smoke 不进入 CI。

CI 规则：

1. CI 默认只运行 mock Provider。
2. CI 不注入真实 AI Key。
3. CI 不访问外部 AI API。
4. CI 不依赖真实 RSS、网页、PDF、代理或对象存储。

## 风险与分阶段落地建议

风险：

1. AI 可能幻觉或夸大事实，必须 Admin 审核后发布。
2. 未授权全文进入模型会产生版权和合规风险，必须由输入选择器兜住。
3. 真实 Provider 失败、限流或成本波动不能阻断采集主链路。
4. Prompt 版本变化会影响输出质量，需要记录 prompt version。
5. AI 输出可能包含投资建议式表述，需要提示词和 Admin 审核共同控制。

分阶段建议：

1. 第一批：完成设计文档、测试方案和任务清单。
2. 第二批：新增 `ai_summary_task` migration、领域模型和 repository。
3. 第三批：实现 mock Provider、输入选择器和任务服务。
4. 第四批：新增 Admin API 和候选详情展示。
5. 第五批：本地显式真实 AI smoke。
6. 第六批：再进入队列化、重试、成本统计和质量评估。

## 当前决策

1. 阶段 27 采用独立 `ai_summary_task` 表记录 AI 生成过程和结果。
2. 默认 Provider 为 `MOCK`，无 AI Key 时 CI 和本地测试必须通过。
3. AI 输入优先级为授权全文、授权正文片段、RSS 摘要；PDF 文本仅预留，不在阶段 27 实现。
4. 未授权全文、未审核 PDF 和登录后内容不得发送给 AI Provider。
5. AI 结果只作为 Admin 可编辑草稿，不自动发布。
6. 真实 AI Provider 只允许本地显式 smoke，不进入 CI。
