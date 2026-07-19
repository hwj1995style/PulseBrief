# Admin 登录、RBAC 与密钥管理说明

## 安全闭环

Admin API 已从单个静态开发 Token 升级为数据库管理员账号和随机不透明会话 Token：

- 密码使用 BCrypt cost 12 单向哈希，数据库不保存明文密码。
- 登录成功生成 256 bit 随机会话 Token，数据库只保存 SHA-256 哈希。
- 会话默认有效期 12 小时，退出后立即撤销。
- 连续失败默认 5 次后锁定 15 分钟，登录错误不区分账号不存在、密码错误或锁定状态。
- 主配置中的旧静态 Token 默认关闭，仅测试配置显式开启兼容模式。

## RBAC

| 角色 | 只读 Admin API | 候选/简报写操作 | 采集配置与执行 |
| --- | --- | --- | --- |
| `VIEWER` | 允许 | 禁止 | 禁止 |
| `EDITOR` | 允许 | 允许 | 禁止 |
| `ADMIN` | 允许 | 允许 | 允许 |

认证身份会写入 PDF 审核人、AI 摘要请求人和发布操作日志。操作日志新增管理员 ID 与角色字段，旧记录保持兼容。

## 首个管理员

首次启动前，在未提交的 `.env.local` 或部署平台 Secret 中临时设置：

```text
PULSEBRIEF_ADMIN_BOOTSTRAP_USERNAME=<admin-name>
PULSEBRIEF_ADMIN_BOOTSTRAP_PASSWORD=<at-least-12-characters>
PULSEBRIEF_ADMIN_BOOTSTRAP_DISPLAY_NAME=PulseBrief Admin
PULSEBRIEF_ADMIN_BOOTSTRAP_ROLE=ADMIN
```

应用只在用户名不存在时创建账号。创建成功后应从运行环境中移除 `PULSEBRIEF_ADMIN_BOOTSTRAP_PASSWORD`，不要长期保留引导密码。

## 密钥管理

- DeepSeek、OpenAI、管理员引导密码和旧 Token 只允许通过环境变量、本地未提交文件或部署平台 Secret 注入。
- Admin API、前端构建变量、日志和数据库均不得返回 Provider API Key。
- `.env.local` 已被 Git 忽略；提交前仍需运行密钥模式扫描和 `scripts/check-provider-env.ps1`。
- 前端会话仅保存在当前标签会话的 `sessionStorage`，关闭浏览器会话后清除；生产部署必须使用 HTTPS，并配置严格 CSP。

## API

```text
POST /api/admin/auth/login
GET  /api/admin/auth/me
POST /api/admin/auth/logout
```

React Admin 在 API 模式下没有有效会话时显示登录页，登录后显示操作人和角色，并支持显式退出。Mock 模式继续使用内置管理员身份，不依赖真实账号。

