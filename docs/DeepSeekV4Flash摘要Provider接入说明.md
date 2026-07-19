# DeepSeek V4 Flash 摘要 Provider 接入说明

## 定位

PulseBrief 已新增 `DEEPSEEK` 摘要 Provider，默认模型为 `deepseek-v4-flash`。它通过 DeepSeek OpenAI 兼容的 Chat Completions API 生成中文审核草稿，现有 Mock 与 OpenAI Provider 继续保留作为测试和回退路径。

真实 Provider 默认关闭，生成结果不会自动应用或发布，仍需 Admin 人工审核。

## 配置

```text
PULSEBRIEF_DEEPSEEK_ENABLED=true
PULSEBRIEF_DEEPSEEK_API_KEY=<local-secret>
PULSEBRIEF_DEEPSEEK_BASE_URL=https://api.deepseek.com/chat/completions
PULSEBRIEF_DEEPSEEK_MODEL=deepseek-v4-flash
PULSEBRIEF_DEEPSEEK_TIMEOUT_SECONDS=30
PULSEBRIEF_DEEPSEEK_MAX_INPUT_CHARACTERS=12000
PULSEBRIEF_DEEPSEEK_MAX_OUTPUT_TOKENS=1200
```

密钥只能放在环境变量或未提交的 `.env.local` 中。启用前运行：

```powershell
.\scripts\check-provider-env.ps1 -EnvFile .\.env.local
```

## 请求与校验

- 使用 `POST /chat/completions`、非流式响应和 `response_format: {"type":"json_object"}`。
- 摘要任务关闭思考模式，以减少摘要场景的延迟和输出成本。
- 提示词明确要求 JSON，并提供目标结构示例。
- 服务端继续本地校验 `summary`、固定 3 条 `keyPoints` 和 `impactAnalysis`，不信任仅由模型保证的结构。
- 若 JSON 模式首次返回空内容，只自动重试一次；HTTP 错误、截断、无效 JSON 或字段错误会标记任务失败。
- 记录响应中的实际模型、prompt token 和 completion token。

## 合规与测试

输入仍由 `AiSummaryTaskService` 统一筛选，只允许授权全文、授权正文片段或 RSS 摘要。未授权全文、未审核 PDF 和登录后材料不会发送给 DeepSeek。

CI 不访问 DeepSeek。自动测试使用本地模拟 HTTP 服务验证请求格式、认证、JSON 解析、Token 统计、空响应重试和缺少密钥时的拒绝逻辑。真实付费 smoke 必须显式配置本地密钥。

## 模型分类

同一个 DeepSeek 连接现在也可用于候选资讯模型分类，分类开关与摘要开关相互独立。模型仅处理未命中关键词规则的标题、来源和 RSS 摘要，并保留来源默认分类、全局分类和 Admin 人工覆盖回退。配置与分类边界见 `docs/DeepSeek模型分类接入说明.md`。

