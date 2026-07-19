# DeepSeek 模型分类接入说明

## 定位

PulseBrief 在现有关键词规则分类之后增加可选的 DeepSeek 模型分类。模型只处理未命中高确定性关键词规则的候选元数据，不覆盖关键词结果，也不能绕过 Admin 审核直接发布。

分类顺序为：

1. 高确定性关键词规则。
2. DeepSeek 模型建议（显式启用时）。
3. 来源默认分类。
4. `global` 安全回退。
5. Admin 人工覆盖最终分类，并强制填写覆盖原因。

## 配置

模型分类默认关闭，与 DeepSeek 摘要开关相互独立，但共享 API Key、Base URL、模型和超时配置：

```text
PULSEBRIEF_DEEPSEEK_API_KEY=<local-secret>
PULSEBRIEF_DEEPSEEK_BASE_URL=https://api.deepseek.com/chat/completions
PULSEBRIEF_DEEPSEEK_MODEL=deepseek-v4-flash
PULSEBRIEF_DEEPSEEK_TIMEOUT_SECONDS=30
PULSEBRIEF_DEEPSEEK_CLASSIFICATION_ENABLED=true
PULSEBRIEF_DEEPSEEK_CLASSIFICATION_MIN_CONFIDENCE=0.65
PULSEBRIEF_DEEPSEEK_CLASSIFICATION_MAX_INPUT_CHARACTERS=4000
PULSEBRIEF_DEEPSEEK_CLASSIFICATION_MAX_OUTPUT_TOKENS=300
```

启用前运行：

```powershell
.\scripts\check-provider-env.ps1 -EnvFile .\.env.local
```

## 输入、输出与回退

- 只发送标题、来源名称和 RSS 摘要，不发送正文、PDF、登录后材料或其他原始 payload。
- 只接受 `ai`、`macro`、`finance`、`technology`、`investment_view`、`industry`、`company`、`global`。
- 响应必须包含合法分类、`0-1` 置信度和简短原因代码。
- 低于最小置信度时忽略模型建议。
- HTTP 错误、超时、截断、空内容、非法 JSON、非法分类或置信度异常均安全回退。
- 接受的模型建议以 `MODEL_DEEPSEEK:<REASON>` 写入 `classificationRule`；模型回退写入 `MODEL_FALLBACK_SOURCE_DEFAULT` 或 `MODEL_FALLBACK_GLOBAL`，便于 Admin 识别。

## 测试边界

CI 默认关闭模型分类，不访问真实 DeepSeek。自动化测试使用本地模拟 HTTP 服务覆盖请求边界、合法响应、低置信度、非法分类、Provider 失败回退和 Spring 条件启用。真实付费 smoke 必须由开发者在本地显式开启。
