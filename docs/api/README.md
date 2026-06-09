# PulseBrief API 文档

PulseBrief 后端 API 以 Spring Boot 实现，当前同时维护手写契约文档和 springdoc-openapi 在线文档。手写文档用于说明业务语义和阶段边界，在线文档用于本地联调、请求体查看和 OpenAPI JSON 验证。

## 文档索引

1. [V1 核心接口设计](./V1核心接口设计.md)

## 在线文档地址

启动本地后端后访问：

```text
http://localhost:8080/swagger-ui.html
http://localhost:8080/v3/api-docs
```

需要登录的接口可在 Swagger UI 中使用开发态 token：

```text
Authorization: Bearer dev-token-1
```
