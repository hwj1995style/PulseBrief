# AI 用量、成本、限流与告警说明

## 目标

PulseBrief 为 DeepSeek 摘要和模型分类建立统一的日级保护：每次真实请求生成用量事件，记录成功、失败或阻断状态、输入/输出 Token 和估算成本；达到每日请求或 Token 上限后，在调用外部 API 前停止请求。

Mock Provider 不计入真实 AI 用量。AI 调用失败或被限流不影响采集入库和人工审核发布。

## 配置

```text
PULSEBRIEF_AI_USAGE_ENABLED=true
PULSEBRIEF_AI_DAILY_REQUEST_LIMIT=200
PULSEBRIEF_AI_DAILY_TOKEN_LIMIT=200000
PULSEBRIEF_AI_WARNING_PERCENT=80
PULSEBRIEF_AI_DEEPSEEK_INPUT_COST_PER_MILLION_USD=0
PULSEBRIEF_AI_DEEPSEEK_OUTPUT_COST_PER_MILLION_USD=0
```

成本单价默认为 `0`，项目不硬编码可能变化的供应商价格。部署时应根据当前合同或供应商价格页填写“每百万 Token 美元单价”，修改配置即可更新后续事件的成本估算，不改变历史事件。

启用前运行：

```powershell
.\scripts\check-provider-env.ps1 -EnvFile .\.env.local
```

## 用量事件与限流

`ai_usage_event` 记录：

- 操作类型：`SUMMARY` 或 `CLASSIFICATION`。
- Provider、模型和请求状态。
- `prompt_tokens`、`completion_tokens`。
- 按配置单价计算的 `estimated_cost_usd`。
- 失败错误类型和开始/完成时间。

请求状态包括 `RUNNING`、`SUCCESS`、`FAILED`、`BLOCKED`。每日请求数包含运行中、成功和失败请求；阻断事件单独统计。每日 Token 达到上限或请求数达到上限时，新请求生成 `BLOCKED` 事件并立即失败，不调用 DeepSeek。

## Admin 监控与告警

`GET /api/admin/ai-usage/today` 返回今日请求、成功、失败、阻断、输入/输出 Token、估算成本、预算阈值和告警级别。

Admin 采集监控页展示 AI 请求、Token、估算成本和失败/阻断数量：

- `NORMAL`：低于告警比例。
- `WARNING`：请求或 Token 使用比例达到配置阈值。
- `LIMIT_REACHED`：达到限额或当天出现阻断事件。

第一批告警为 Admin 页面内告警；邮件、Slack/Teams、Prometheus 指标和多实例强一致限流留作生产部署增强。
