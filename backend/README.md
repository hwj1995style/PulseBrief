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

真实资讯采集 V1 已开始搭建适配层。当前阶段只提供 `FIXTURE` Provider，用于验证采集抽象、配置绑定和后续入库流程；不会请求真实外部 API，也不需要 API Key。

采集总开关默认关闭：

```text
PULSEBRIEF_INGESTION_ENABLED=false
```

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

当前阶段不接真实短信、邮件或 AI 服务；用户端数据仍来自 Flyway V2 种子数据。真实资讯采集已具备适配层和 fixture Provider，尚未写入业务表或发布到用户端。

## Smoke Check

启动 MySQL 和后端后，可用以下接口做冒烟检查：

```powershell
Invoke-RestMethod http://localhost:8080/api/categories
Invoke-RestMethod http://localhost:8080/api/articles/home
Invoke-RestMethod http://localhost:8080/api/digests/today
```
