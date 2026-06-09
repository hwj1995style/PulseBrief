# PulseBrief Backend

Spring Boot 3 backend for PulseBrief.

## Requirements

Use JDK 17 for this project. The repository keeps Java switching local to the current PowerShell session:

```powershell
..\scripts\use-jdk17.ps1
```

## Local Database

The local MySQL container publishes host port `3307` to avoid collisions with existing MySQL services on `3306`.

```powershell
docker compose -f ..\deploy\docker-compose.yml up -d
```

Default connection:

```text
jdbc:mysql://localhost:3307/pulsebrief
user: pulsebrief
password: pulsebrief_dev
```

## Test

```powershell
..\scripts\use-jdk17.ps1
.\mvnw.cmd test
```

## Run

```powershell
..\scripts\use-jdk17.ps1
.\mvnw.cmd spring-boot:run
```

## API Documentation

本地后端启动后可访问在线接口文档：

```text
http://localhost:8080/swagger-ui.html
http://localhost:8080/v3/api-docs
```

Swagger UI 调试需要登录接口时，使用开发态 Bearer Token：

```text
Authorization: Bearer dev-token-1
```

## News Ingestion

真实资讯采集 V1 已开始搭建后端基础能力。当前阶段提供 `FIXTURE` Provider，用于验证采集抽象、配置绑定和入库流程；不会请求真实外部 API，也不需要 API Key。

采集总开关默认关闭：

```text
PULSEBRIEF_INGESTION_ENABLED=false
```

当前已新增原始资讯池、采集任务日志、采集源配置表和候选资讯审核池。采集后的 `raw_news_item` 可生成 `PENDING_REVIEW` 候选资讯，未审核候选不会进入用户端文章 API。

Admin 审核发布 API 设计见 `docs/Admin审核发布V1设计.md`。当前已完成接口边界设计，Admin API 代码实现将按设计分阶段推进。

最新内容与授权全文/PDF 采集设计见 `docs/最新内容与授权全文PDF采集V1设计.md`。真实 Provider 必须默认只采集最新内容：新闻默认 24 小时窗口，公开报告和授权 PDF 默认 72 小时窗口；付费研报、登录后 PDF、历史研报库和未授权全文不进入 V1。当前后端已支持采集源许可字段、最新窗口过滤、`report_asset` 授权 PDF 元数据入库和文件哈希去重；授权全文字段与 PDF 文件缓存后续分阶段实现。

后续接入真实公开 API 或 RSS 时，密钥、关键词、语言、国家/市场和请求频率必须通过环境变量或本地未提交配置注入，不提交到 Git。

## V1 App API

当前已实现用户端 V1 API 骨架：

```text
POST   /api/auth/login
POST   /api/auth/guest
GET    /api/categories
GET    /api/articles/home
GET    /api/articles
GET    /api/articles/{id}
POST   /api/articles/{id}/favorite
DELETE /api/articles/{id}/favorite
GET    /api/digests/today
GET    /api/digests/{id}
GET    /api/user/subscriptions
PUT    /api/user/subscriptions
GET    /api/user/profile
GET    /api/user/favorites
GET    /api/user/read-history
POST   /api/user/read-history
DELETE /api/user/read-history
POST   /api/playback/history
GET    /api/playback/history
DELETE /api/playback/history
```

登录接口当前使用开发态 mock token：

```json
{
  "accessToken": "dev-token-1",
  "tokenType": "Bearer"
}
```

需要登录的接口使用请求头：

```text
Authorization: Bearer dev-token-1
```

当前阶段不接真实短信、邮件或 AI 服务；用户端数据仍来自 Flyway V2 种子数据。真实资讯采集已具备适配层、fixture Provider、原始资讯入库、去重和候选资讯生成能力，候选内容必须经后续 Admin 审核发布后才会进入用户端。

## Smoke Check

启动 MySQL 和后端后，可用以下接口做冒烟检查：

```powershell
Invoke-RestMethod http://localhost:8080/api/categories
Invoke-RestMethod http://localhost:8080/api/articles/home
Invoke-RestMethod http://localhost:8080/api/digests/today
```
