# PulseBrief OpenAPI 与 Swagger 接口文档设计

## 背景与现状

PulseBrief 后端 V1 核心接口已覆盖登录、首页资讯、分类、简报、订阅、用户资料、收藏、阅读历史和播放历史。当前接口契约主要依赖 `docs/api/V1核心接口设计.md` 和 Flutter live API 测试维护，`docs/api/README.md` 中已经预留 `http://localhost:8080/swagger-ui.html`，但后端尚未接入 `springdoc-openapi`，无法在本地直接查看在线接口文档或导出 OpenAPI JSON。

## 目标与非目标

目标：

1. 接入 Spring Boot 3 兼容的 `springdoc-openapi`。
2. 本地启动后可访问 Swagger UI：`http://localhost:8080/swagger-ui.html`。
3. 本地启动后可访问 OpenAPI JSON：`http://localhost:8080/v3/api-docs`。
4. OpenAPI 文档只收录 `/api/**` 下的 PulseBrief V1 接口。
5. 文档中声明 V1 API 基础信息和本地开发服务地址。
6. 文档中声明 `Authorization: Bearer dev-token-1` 的开发期 Bearer Token 安全方案，方便后续接口调试。
7. 增加后端测试，确保 `/v3/api-docs` 可生成并包含关键接口路径。

非目标：

1. 不在本轮逐个接口补齐完整 `@Operation`、`@Schema` 示例。
2. 不引入生产级鉴权或 Spring Security。
3. 不生成离线静态 OpenAPI 文件。
4. 不调整现有接口路径、请求体或响应体结构。

## 影响范围

后端：

1. `backend/pom.xml` 新增 `springdoc-openapi-starter-webmvc-ui` 依赖。
2. `backend/src/main/resources/application.yml` 新增 springdoc 路径、扫描范围和 Swagger UI 排序配置。
3. 新增 OpenAPI 基础配置类，声明 API 信息、服务地址、标签和 Bearer Token 安全方案。
4. 新增后端测试验证 OpenAPI JSON。

文档：

1. 更新 `docs/api/README.md`，将预留地址改为已接入地址。
2. 更新 `backend/README.md`，补充 Swagger UI 访问方式。
3. 更新 `docs/下一阶段任务清单.md`，记录本阶段完成项并给出下一步建议。

## 数据模型或权限模型

本轮不新增业务数据表，不修改权限模型。

OpenAPI 文档层面新增安全方案：

```text
bearerAuth
type: http
scheme: bearer
bearerFormat: dev-token
```

当前后端仍沿用开发期 token：

```text
Authorization: Bearer dev-token-1
```

Swagger UI 只提供调试入口，不改变接口实际鉴权逻辑。

## 后端实现方案

1. 在 `pom.xml` 中固定 `springdoc.version` 为 `2.8.17`，并添加：

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>${springdoc.version}</version>
</dependency>
```

2. 在 `application.yml` 中配置：

```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    operations-sorter: method
    tags-sorter: alpha
  packages-to-scan: com.pulsebrief
  paths-to-match: /api/**
```

3. 新增 `OpenApiConfig`：

```java
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "PulseBrief V1 API",
                version = "v1.0.0",
                description = "PulseBrief mobile app backend API for global news brief, digest, subscription, and user history flows."
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Local development")
        },
        tags = {
                @Tag(name = "Health", description = "Service health check"),
                @Tag(name = "Auth", description = "Mock login and guest session"),
                @Tag(name = "Articles", description = "Home feed and article detail"),
                @Tag(name = "Categories", description = "News categories"),
                @Tag(name = "Digests", description = "Daily digest and audio briefing"),
                @Tag(name = "Subscriptions", description = "User subscription settings"),
                @Tag(name = "User", description = "User profile, favorites, and reading history"),
                @Tag(name = "Playback", description = "Audio playback history")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "dev-token"
)
public class OpenApiConfig {
}
```

## 前端影响

Flutter 代码不需要变更。开发联调时可通过 Swagger UI 查看请求体、响应结构和接口路径；真实调试仍以 Flutter live API 测试为准。

## 测试与回归方案

后端新增 `OpenApiDocumentationTest`：

1. 请求 `GET /v3/api-docs` 返回 200。
2. 校验 `$.openapi` 存在。
3. 校验 `$.info.title` 为 `PulseBrief V1 API`。
4. 校验路径包含 `/api/health`、`/api/auth/login`、`/api/articles/home`。
5. 校验 `$.components.securitySchemes.bearerAuth.scheme` 为 `bearer`。

回归命令：

```powershell
.\mvnw.cmd test
flutter analyze
flutter test
```

如后端服务已启动，再用浏览器或命令访问：

```text
http://localhost:8080/swagger-ui.html
http://localhost:8080/v3/api-docs
```

## 风险与分阶段落地建议

1. 首轮 OpenAPI 主要依赖 Spring MVC 方法签名自动生成，个别字段说明不够精细；后续可逐模块补 `@Operation`、`@Parameter` 和 `@Schema`。
2. 当前 `Authorization` 仍是开发期 token，Swagger UI 的 Authorize 入口仅用于本地调试；正式登录鉴权落地后需要同步更新安全方案。
3. 如果未来引入后台管理 API，建议按分组拆分为 `mobile-v1` 和 `admin-v1` 两个 OpenAPI Group。
