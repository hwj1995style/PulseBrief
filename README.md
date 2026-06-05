# PulseBrief

PulseBrief 是一个面向全球热点、财经市场、科技趋势和投行公开观点的每日资讯简报产品，核心能力包括全球热点聚合、AI 摘要、分类订阅、每日简报和语音播报。

项目已从早期 React/Vite PWA MVP 切换为正式工程路线：

```text
Flutter 用户端 APP
+ Spring Boot 后端 API
+ MySQL 数据库
+ React 后台管理端
```

当前 `apps/web` 和 `services/mock-api` 仍保留为产品流程参考原型，正式 V1 将按 `mobile/`、`backend/`、`admin/`、`deploy/` 分阶段落地。

## 当前状态

已完成：

1. 原始产品设计文档。
2. React/Vite PWA MVP，用于验证首页、分类、详情、简报、收藏和播报入口。
3. Node mock API，用于模拟核心接口数据。
4. 工程落地设计与测试方案。
5. 完整技术路线落地设计：Flutter + Spring Boot + MySQL + React Admin。
6. GitHub 仓库初始化与同步。

下一阶段：

1. 配置 JDK 17 和 Flutter SDK。
2. 创建 Spring Boot 3 后端工程。
3. 创建 Flutter 移动端工程。
4. 创建 Docker Compose MySQL 开发环境。
5. 使用 Maven Wrapper 固定后端构建方式。

## 正式技术路线

后端：

1. Java 17
2. Spring Boot 3.x
3. Maven Wrapper
4. MySQL 8
5. Flyway
6. Spring Data JPA
7. springdoc-openapi

移动端：

1. Flutter
2. iOS / Android
3. 客户端系统 TTS
4. 后端 API 驱动

后台端：

1. React
2. Vite
3. TypeScript
4. 后台管理 API

本地开发：

1. 多 JDK 共存。
2. 保留全局 Java 8。
3. PulseBrief 后端通过脚本临时启用 Java 17。
4. Flutter SDK 独立安装。
5. MySQL 优先通过 Docker Compose 启动。

## 规划目录

```text
backend/              Spring Boot 3 后端
mobile/               Flutter 用户端 APP
admin/                React 后台管理端
deploy/               Docker Compose、MySQL、本地服务配置
scripts/              JDK/Flutter 环境脚本和开发启动脚本
docs/sql/             MySQL schema、种子数据和迁移说明
docs/api/             API 契约文档
docs/architecture/    架构图、环境说明和阶段计划
apps/web/             现有 PWA MVP，保留为参考原型
services/mock-api/    现有 mock API，后续由 backend 替换
```

## 环境说明

当前本机检查结果：

1. 默认 Java 是 Java 8。
2. Flutter SDK 尚未安装或未加入 PATH。
3. `mvn` 不在 PATH。
4. 已发现 IntelliJ 自带 Maven，但项目正式构建会使用 Maven Wrapper。

推荐安装路径：

```text
D:\Dev\jdk\jdk-17
D:\Dev\flutter
```

后续会新增脚本：

```powershell
.\scripts\use-jdk17.ps1
.\scripts\use-flutter.ps1
```

脚本只影响当前 PowerShell 会话，不修改系统全局 Java 8。

## 现有 MVP 运行

当前 PWA 原型仍可运行，用于查看已验证的产品流程。

安装依赖：

```bash
npm install
```

启动前端：

```bash
npm run dev
```

启动 mock API：

```bash
npm run api
```

验证：

```bash
npm test
npm run build
```

## 正式 V1 分阶段计划

第一阶段：工程底座。

1. JDK 17 与 Flutter SDK 环境脚本。
2. Spring Boot 3 后端工程。
3. Flutter 移动端工程。
4. Docker Compose MySQL。
5. Maven Wrapper。

第二阶段：后端核心 API。

1. 数据库表结构和 Flyway migration。
2. 分类、文章、简报种子数据。
3. 首页、分类、详情、简报 API。
4. 收藏、订阅、播放历史 API。

第三阶段：Flutter 核心体验。

1. 登录和兴趣选择。
2. 首页、分类、资讯详情、简报、我的。
3. 底部迷你播放器。
4. 客户端 TTS。

第四阶段：后台管理。

1. 数据看板。
2. 资讯源管理。
3. 文章管理。
4. 简报管理。
5. 推送任务管理。

第五阶段：内容链路。

1. RSS mock 采集。
2. RSS / GDELT 真实采集。
3. AI 摘要服务抽象。
4. 简报定时生成。

第六阶段：推送与上架准备。

1. APNs / FCM 接入。
2. 用户协议与隐私政策。
3. 内容来源说明。
4. App Store / Google Play 上架材料。

## 文档

1. [产品设计文档](./每日全球热点资讯APP产品设计文档.md)
2. [工程落地设计](./docs/工程落地设计.md)
3. [测试方案](./docs/测试方案.md)
4. [完整技术路线落地设计](./docs/完整技术路线落地设计.md)

## 合规边界

PulseBrief 不做新闻全文搬运。首版只展示标题、来源、发布时间、短摘要、AI 解读和原文链接。投行公开观点只做短摘要，不展示完整报告或付费内容。
