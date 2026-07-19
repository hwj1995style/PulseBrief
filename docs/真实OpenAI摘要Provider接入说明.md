# 真实 OpenAI 摘要 Provider 接入说明

## 实现范围

PulseBrief 已在现有 `AiSummaryProvider` 抽象上增加 OpenAI Responses API 实现。该实现只生成 Admin 审核草稿，不自动覆盖候选内容，也不绕过人工确认或发布流程。

Provider 只接收摘要任务服务选出的合规输入，优先级保持为：

1. 明确授权的全文；
2. 明确授权的正文片段；
3. RSS 提供的摘要。

未授权全文、未审核 PDF 文本和登录后内容不会发送给 OpenAI。输入还会按 `PULSEBRIEF_OPENAI_MAX_INPUT_CHARACTERS` 截断。

## 配置

真实 Provider 默认关闭。复制根目录 `.env.example` 为未提交的本地配置文件，并设置：

```text
PULSEBRIEF_OPENAI_ENABLED=true
PULSEBRIEF_OPENAI_API_KEY=<local-secret>
PULSEBRIEF_OPENAI_BASE_URL=https://api.openai.com/v1/responses
PULSEBRIEF_OPENAI_MODEL=gpt-5.6-luna
PULSEBRIEF_OPENAI_TIMEOUT_SECONDS=30
PULSEBRIEF_OPENAI_MAX_INPUT_CHARACTERS=12000
PULSEBRIEF_OPENAI_MAX_OUTPUT_TOKENS=1200
```

启用前先运行：

```powershell
.\scripts\check-provider-env.ps1 -EnvFile .\.env.local
```

密钥只能通过环境变量或本地未提交配置注入，不得写入源码、测试、日志或 Git。

## 输出与失败边界

- 请求使用严格 JSON Schema，要求 `summary`、固定 3 条 `keyPoints` 和 `impactAnalysis`。
- 模型名、输入 token 和输出 token 会写入摘要任务审计记录。
- 非 2xx、超时、中断、无输出或结构不合法会把摘要任务标记为失败，不影响采集、候选人工编辑或人工发布。
- 服务端错误只记录状态和安全错误信息，不记录密钥或远端响应正文。
- Admin 可分别选择 Mock 或 OpenAI 生成，并且只能应用成功任务的草稿。

## 验证策略

默认 CI 和本地全量测试不访问 OpenAI。自动测试通过本地模拟 HTTP 服务验证认证头、Responses API 请求结构、严格 Schema、结果解析和用量统计。真实付费 smoke 必须由开发者显式提供密钥并开启开关，且不进入 CI。

