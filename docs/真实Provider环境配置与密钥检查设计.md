# 真实 Provider 环境配置与密钥检查设计

## 背景与现状

PulseBrief 已完成真实资讯采集基础链路、fixture Provider、候选审核发布、运营监控和 CI 验证流水线。下一阶段要接入第一批真实公开 RSS/API Provider，但在真正访问外部来源之前，需要先把环境变量、密钥、来源许可和最新窗口检查固化下来，避免开发或 CI 环境误触发外部请求、提交密钥或做历史回溯。

当前已有配置：

1. `PULSEBRIEF_INGESTION_ENABLED=false` 默认关闭采集。
2. `news_ingestion_source` 已支持来源许可、最新窗口、PDF 下载和全文许可字段。
3. `RawNewsIngestionService` 已按采集源最新窗口跳过过期内容。
4. CI 已显式设置 `PULSEBRIEF_INGESTION_ENABLED=false`。

## 目标与非目标

目标：

1. 提供真实 Provider 接入前的 `.env.example` 样例。
2. 提供本地检查脚本，验证采集开关、Provider 类型、RSS URL/API Key、许可策略、最新窗口、限流和禁止回溯。
3. 明确 CI 和本地开发默认不请求真实外部 Provider。
4. 防止明显占位符、示例 URL 或空密钥被误认为可用配置。
5. 为后续 RSS Provider 或 API Provider 实现提供配置边界。

非目标：

1. 本轮不实现真实 RSS Provider。
2. 本轮不实现真实 API Provider。
3. 本轮不发起任何外部网络采集请求。
4. 本轮不引入新的密钥管理服务。
5. 本轮不修改数据库结构。

## 影响范围

新增：

1. `.env.example`：真实 Provider 接入前的安全配置样例。
2. `scripts/check-provider-env.ps1`：本地环境配置检查脚本。
3. `docs/真实Provider环境配置与密钥检查设计.md`：本设计文档。

更新：

1. `README.md`：补充真实 Provider 配置检查入口。
2. `backend/README.md`：补充 Provider 环境变量边界。
3. `docs/测试方案.md`：补充配置检查命令。
4. `docs/下一阶段任务清单.md`：新增阶段 22。

## 数据模型或权限模型

本轮不新增数据模型和权限模型。

配置模型约定：

| 变量 | 说明 | 默认建议 |
| --- | --- | --- |
| `PULSEBRIEF_INGESTION_ENABLED` | 是否启用真实采集 | `false` |
| `PULSEBRIEF_PROVIDER_KIND` | `FIXTURE/RSS/API` | 空 |
| `PULSEBRIEF_RSS_FEED_URLS` | 逗号分隔 RSS URL，仅 RSS 使用 | 空 |
| `PULSEBRIEF_PROVIDER_API_BASE_URL` | API Provider 基础地址，仅 API 使用 | 空 |
| `PULSEBRIEF_PROVIDER_API_KEY` | API Provider 密钥，仅 API 使用 | 空 |
| `PULSEBRIEF_PROVIDER_LICENSE_POLICY` | 来源许可策略 | `SUMMARY_ONLY` |
| `PULSEBRIEF_PROVIDER_LICENSE_NOTE` | 来源授权说明 | 空 |
| `PULSEBRIEF_INGESTION_MAX_AGE_HOURS` | 新闻最新窗口 | `24` |
| `PULSEBRIEF_INGESTION_PDF_MAX_AGE_HOURS` | PDF/报告最新窗口 | `72` |
| `PULSEBRIEF_INGESTION_MAX_ITEMS_PER_SOURCE` | 单源单次最大采集条数 | `50` |
| `PULSEBRIEF_PROVIDER_RATE_LIMIT_PER_HOUR` | 单源每小时请求上限 | `60` |
| `PULSEBRIEF_INGESTION_ALLOW_BACKFILL` | 是否允许历史回溯 | `false` |
| `PULSEBRIEF_INGESTION_ALLOW_FULL_TEXT` | 是否允许保存授权全文 | `false` |
| `PULSEBRIEF_INGESTION_ALLOW_PDF_DOWNLOAD` | 是否允许下载授权 PDF | `false` |

许可策略沿用现有设计：

```text
SUMMARY_ONLY
SNIPPET_ALLOWED
FULLTEXT_ALLOWED
PDF_ALLOWED
LINK_ONLY
UNKNOWN
```

## 后端实现方案

本轮不改 Spring Boot 运行逻辑，只新增检查脚本作为接入前门禁：

```powershell
.\scripts\check-provider-env.ps1
.\scripts\check-provider-env.ps1 -EnvFile .\.env.local
```

检查规则：

1. `PULSEBRIEF_INGESTION_ENABLED` 为 `false` 或未设置时，脚本通过，但提示真实采集关闭。
2. 启用采集时，必须设置 `PULSEBRIEF_PROVIDER_KIND`。
3. `RSS` Provider 必须设置 `PULSEBRIEF_RSS_FEED_URLS`，且 URL 不能是 `example.com` 占位符。
4. `API` Provider 必须设置 `PULSEBRIEF_PROVIDER_API_BASE_URL` 和 `PULSEBRIEF_PROVIDER_API_KEY`。
5. API Key 不能是 `changeme`、`placeholder`、`your-api-key` 等占位符。
6. 禁止 `PULSEBRIEF_INGESTION_ALLOW_BACKFILL=true`。
7. 新闻最新窗口必须在 `1..72` 小时内。
8. PDF 最新窗口必须在 `1..168` 小时内。
9. 单源单次最大采集条数必须在 `1..100` 内。
10. 每小时请求上限必须在 `1..60` 内。
11. 全文或 PDF 能力开启时，必须配置匹配的许可策略和授权说明。

## 前端影响

Flutter 用户端无影响。

React Admin 本轮无 UI 改动。后续真实 Provider 接入后，Admin 采集源页面可展示来源许可、启停状态和异常数据。

## 测试与回归方案

新增验证：

1. 默认环境执行 `.\scripts\check-provider-env.ps1` 应通过，并提示真实采集关闭。
2. `.env.example` 执行 `.\scripts\check-provider-env.ps1 -EnvFile .\.env.example` 应通过。
3. 构造启用 RSS 且使用 `example.com` URL 的临时 env 文件，应失败。
4. 构造启用 API 但缺少 API Key 的临时 env 文件，应失败。

完整回归：

```powershell
.\scripts\check-provider-env.ps1
.\scripts\check-provider-env.ps1 -EnvFile .\.env.example
.\scripts\use-jdk17.ps1
cd backend
.\mvnw.cmd test
cd ..\admin
npm test -- --run
npm run lint
npm run build
cd ..\mobile
..\scripts\use-flutter.ps1
flutter analyze
flutter test
cd ..
docker compose -f deploy/docker-compose.yml config
```

## 风险与分阶段落地建议

风险：

1. 检查脚本只能发现配置层问题，不能替代 Provider 运行时限流、重试和失败日志。
2. RSS 来源许可仍需要人工确认，不能仅凭 URL 自动判断授权。
3. API 密钥后续需要接入更正式的密钥注入方式，不能依赖 `.env.local` 作为生产方案。

分阶段建议：

1. 阶段 22：先落地配置样例和本地检查脚本。
2. 阶段 23：实现第一个真实 RSS Provider，默认只采集标题、摘要、来源、发布时间和原文链接。
3. 阶段 24：增加真实 Provider 的 Admin 手动触发和失败日志闭环。
4. 阶段 25：再评估公开 API Provider、AI 摘要服务和授权 PDF 缓存。
