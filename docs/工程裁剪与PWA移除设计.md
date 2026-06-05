# PulseBrief React/Vite PWA MVP 移除设计

## 背景与现状

PulseBrief 已确定按 Flutter + Spring Boot + MySQL + React Admin 的完整 V1.0 技术路线推进。仓库中的 `apps/web` React/Vite PWA MVP 和 `services/mock-api` Node mock API 是早期用于验证产品流程的临时实现。

当前用户明确要求：原先的 React/Vite PWA MVP 不再保留。旧 MVP 的 UI 风格也不作为新 Flutter UI 的参考。

## 目标与非目标

目标：

1. 从当前主线仓库移除 `apps/web` 和 `services/mock-api`。
2. 移除仅服务旧 PWA 的根目录 `package.json` 和 `package-lock.json`。
3. 更新 README、架构说明、测试方案和完整技术路线文档。
4. 将正式用户端唯一入口明确为 `mobile/` Flutter App。
5. 将旧 MVP 的参考价值降级为 Git 历史，不再占用当前目录和 GitHub 语言统计。

非目标：

1. 本次不实现新的 Flutter UI。
2. 本次不创建 React Admin 正式工程。
3. 本次不删除 Git 历史中的旧 MVP 提交。
4. 本次不修改后端数据模型或 API 表结构。

## 影响范围

删除目录与文件：

1. `apps/web/`
2. `services/mock-api/`
3. `package.json`
4. `package-lock.json`

更新文档：

1. `README.md`
2. `docs/完整技术路线落地设计.md`
3. `docs/测试方案.md`
4. `docs/architecture/README.md`
5. `admin/README.md`

保留目录：

1. `mobile/`：Flutter 用户端正式主线。
2. `backend/`：Spring Boot 后端正式主线。
3. `admin/`：React 后台管理端占位，后续独立创建。
4. `deploy/`：MySQL 与部署配置。
5. `scripts/`：JDK、Flutter 与环境检查脚本。

## 数据模型或权限模型

本次裁剪不修改数据库表结构、Flyway migration、用户权限或后台权限模型。

后续 Flutter UI 将直接面向正式后端 API 设计，不再依赖 Node mock API 的数据结构作为契约来源。

## 后端实现方案

后端不做功能改动。

本次只保留 Spring Boot 工程和 `/api/health` 测试基线。后续分类、文章、简报 API 将通过 `backend/` 实现，并用正式 Controller / Service / Repository 测试覆盖。

## 前端影响

用户端：

1. 正式用户端只保留 `mobile/` Flutter。
2. Flutter 默认 starter 后续将替换为主流移动资讯 App UI。
3. 旧 PWA 不再作为视觉参考，只能通过 Git 历史查看早期流程。

后台端：

1. `admin/` 仍保留为正式后台入口。
2. 后台将独立使用 React + Vite + TypeScript 创建，不复用旧 PWA。

## 测试与回归方案

删除旧 PWA 后，根目录不再提供 `npm test`、`npm run build` 或 `npm run api`。

当前回归命令调整为：

```powershell
.\scripts\check-env.ps1

.\scripts\use-jdk17.ps1
cd backend
.\mvnw.cmd test

cd ..\mobile
.\..\scripts\use-flutter.ps1
flutter test

cd ..
docker compose -f deploy/docker-compose.yml config
```

预期结果：

1. 后端单元测试通过。
2. Flutter widget 测试通过。
3. Docker Compose 配置有效。
4. 仓库中不再出现 `apps/web`、`services/mock-api`、根目录 Node PWA 脚本。

## 风险与分阶段落地建议

风险：

1. 旧 MVP 删除后，无法在当前工作树直接查看早期交互原型。
2. README 和设计文档如果未同步更新，容易误导后续开发继续使用旧 PWA。
3. 新 Flutter UI 尚未落地前，用户端暂时仍是默认 starter 页面。

缓解：

1. 旧 MVP 可通过 Git 历史恢复或对照。
2. 当前提交同步更新所有主文档。
3. 下一阶段优先推进 Flutter 主流 UI 重设计和核心页面实现。

分阶段建议：

1. 本阶段完成工程裁剪和文档同步。
2. 下一阶段完成 Flutter UI 设计文档、视觉方向选择和页面实现计划。
3. 再后续接入 Spring Boot 分类、文章、简报 API。
