# CI 验证流水线设计

## 背景与现状

PulseBrief 已完成阶段 20 三端收口：Spring Boot 后端、Flutter 用户端和 React Admin 后台都已经具备本地验证命令。当前问题是验证仍依赖开发者手动执行，代码推送到 GitHub 后缺少统一的自动化质量门禁。

现有稳定命令：

1. 后端：`cd backend; .\mvnw.cmd test`
2. Flutter：`cd mobile; flutter analyze; flutter test`
3. Admin：`cd admin; npm test -- --run; npm run lint; npm run build`
4. 部署配置：`docker compose -f deploy/docker-compose.yml config`

## 目标与非目标

目标：

1. 在 `main` 分支 push 和 pull request 时自动运行三端验证。
2. 后端 CI 使用 MySQL 服务容器，覆盖 Flyway migration、JPA validate 和 Spring Boot 测试。
3. Admin CI 使用 `npm ci`，保证依赖安装与 `package-lock.json` 一致。
4. Flutter CI 使用稳定 Flutter SDK，覆盖静态分析和单元/widget 测试。
5. Docker Compose 配置在 CI 中做语法验证，提前发现部署配置损坏。

非目标：

1. 本轮不构建 Android APK，也不做 iOS 构建。
2. 本轮不启动完整后端服务做 live API 测试。
3. 本轮不接入真实 RSS/API Provider、AI 服务、短信、TTS 或推送。
4. 本轮不发布构建产物、不部署环境、不创建 release。

## 影响范围

新增：

1. `.github/workflows/ci.yml`：GitHub Actions CI 工作流。
2. `docs/CI验证流水线设计.md`：本设计文档。

更新：

1. `docs/测试方案.md`：补充 CI 验证策略。
2. `docs/下一阶段任务清单.md`：新增阶段 21 记录。
3. `README.md`：更新下一阶段状态。

## 数据模型或权限模型

本轮不新增业务数据模型和用户权限模型。

CI 后端测试需要临时 MySQL 服务，使用 GitHub Actions service container 提供独立数据库：

1. 数据库名：`pulsebrief`
2. 用户名：`pulsebrief`
3. 密码：`pulsebrief_dev`
4. JDBC：`jdbc:mysql://127.0.0.1:3306/pulsebrief?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai`

这些值只用于 CI 测试环境，不代表生产密钥。真实 Provider、AI 服务和后台账号密钥仍不得提交到 Git。

## 后端实现方案

新增 `backend` job：

1. 使用 `actions/checkout@v4` 拉取代码。
2. 使用 `actions/setup-java@v4` 安装 Temurin JDK 17，并启用 Maven 缓存。
3. 启动 `mysql:8.4` service container。
4. 设置 `PULSEBRIEF_DB_URL`、`PULSEBRIEF_DB_USERNAME`、`PULSEBRIEF_DB_PASSWORD` 和 `PULSEBRIEF_INGESTION_ENABLED=false`。
5. 在 Linux runner 上执行 `chmod +x ./mvnw`，避免 Windows 工作区未保留可执行位导致 Maven Wrapper 无法启动。
6. 执行 `./mvnw test`。

后端 CI 不访问真实外网 Provider，采集开关保持关闭。

## 前端影响

Admin 新增 `admin` job：

1. 使用 Node.js 24。
2. 执行 `npm ci`。
3. 执行 `npm test -- --run`。
4. 执行 `npm run lint`。
5. 执行 `npm run build`。

Flutter 新增 `mobile` job：

1. 使用稳定 Flutter channel。
2. 执行 `flutter pub get`。
3. 执行 `flutter analyze`。
4. 执行 `flutter test`。

Flutter live API 测试按现有设计默认跳过，不在 CI 中启动后端联调。

## 测试与回归方案

本轮本地验证：

1. `cd backend; .\mvnw.cmd test`
2. `cd admin; npm test -- --run`
3. `cd admin; npm run lint`
4. `cd admin; npm run build`
5. `cd mobile; flutter analyze`
6. `cd mobile; flutter test`
7. `docker compose -f deploy/docker-compose.yml config`

CI 验收：

1. GitHub Actions 中 `backend`、`admin`、`mobile` 和 `deploy-config` jobs 均通过。
2. 任一端测试、构建或配置检查失败时，整条 workflow 失败。

## 风险与分阶段落地建议

风险：

1. GitHub runner 上 Flutter stable 版本可能高于本机版本，若 lint 行为变化，需根据 CI 输出修正。
2. 后端测试依赖 MySQL service 健康检查；如果镜像拉取或初始化慢，可能需要延长 health retry。
3. Admin 依赖 Vite 7 和 React 19，Node 版本过低会失败，因此 CI 固定 Node.js 24。

分阶段建议：

1. 阶段 21：先落地基础 CI，覆盖现有稳定命令。
2. 后续真实 Provider 接入前，增加环境变量示例检查和密钥缺失时的显式失败/跳过策略。
3. 移动端发布准备阶段，再新增 Android release 签名检查、APK/AAB 构建和产物上传。
