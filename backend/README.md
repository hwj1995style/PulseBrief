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
POST   /api/playback/history
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

当前阶段不接真实短信、邮件、资讯采集或 AI 服务；数据来自 Flyway V2 种子数据。

## Smoke Check

启动 MySQL 和后端后，可用以下接口做冒烟检查：

```powershell
Invoke-RestMethod http://localhost:8080/api/categories
Invoke-RestMethod http://localhost:8080/api/articles/home
Invoke-RestMethod http://localhost:8080/api/digests/today
```
