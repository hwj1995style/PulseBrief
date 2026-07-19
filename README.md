# PulseBrief

PulseBrief 是一个面向全球热点、财经市场、科技趋势和投行公开观点的每日资讯简报产品，核心能力包括全球热点聚合、AI 摘要、分类订阅、每日简报和语音播报。

项目已切换为正式工程路线：

```text
Flutter 用户端 APP
+ Spring Boot 后端 API
+ MySQL 数据库
+ React 后台管理端
```

原先的 React/Vite PWA MVP 和 Node mock API 已不再保留。正式 V1 将按 `mobile/`、`backend/`、`admin/`、`deploy/` 分阶段落地。

## 当前状态

已完成：

1. 原始产品设计文档。
2. 工程落地设计与测试方案。
3. 完整技术路线落地设计：Flutter + Spring Boot + MySQL + React Admin。
4. React/Vite PWA MVP 从主线移除，旧实现仅通过 Git 历史保留。
5. GitHub 仓库初始化与同步。
6. JDK 17 和 Flutter SDK 本地可用。
7. Spring Boot 3 后端骨架和 `/api/health` 测试。
8. Flutter 移动端 Android/iOS 工程骨架。
9. Docker Compose MySQL 开发配置。
10. Flutter 高保真移动端 UI 首版：登录、首页、分类、订阅、资讯详情、每日简报、语音播放器、我的。
11. 移动端主题 tokens、通用组件、mock 数据和统一路由。
12. Android Studio、Android SDK 和 Android 模拟器已按 D 盘路径配置。
13. 已完成 8 个 Flutter 页面模拟器截图验收和首轮 P2 排版修正。
14. 已补充 Flutter 本机预览、APK 构建和透明插图资产生成说明。
15. 已完成 Spring Boot V1 用户端 API、Flutter API 模式联调、用户中心真实数据、历史列表、分页规范和列表管理。
16. 已接入 OpenAPI/Swagger 在线接口文档。
17. 已完成真实资讯采集基础链路、候选资讯生成、Admin 审核发布、每日简报管理和运营监控。
18. 已完成 React Admin 独立后台：候选审核、简报管理、采集任务监控、采集源启停、发布操作日志和异常数据检测。
19. 已补充 GitHub Actions 基础 CI：后端测试、Admin 测试/构建、Flutter analyze/test 和 Docker Compose 配置检查。

下一阶段：

1. 补齐生产级 Admin 登录、RBAC、操作身份和密钥管理。
2. 将真实 AI Provider 扩展到模型分类，并完善成本、限流和告警。
3. 完成移动端签名、隐私合规材料、推送和应用商店上架流程。

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
```

## 环境说明

当前本机检查结果：

1. 默认 Java 是 Java 8，保持不变。
2. JDK 17 已安装在 `D:\Dev\jdk\jdk-17`。
3. Flutter SDK 已安装在 `D:\Dev\flutter`。
4. Android SDK 已安装在 `D:\Dev\Android\Sdk`。
5. Android 模拟器数据目录使用 `D:\Dev\Android\Avd`。
6. `mvn` 不在 PATH，但后端已使用 Maven Wrapper。
7. Docker 可用。

推荐安装路径：

```text
D:\Dev\jdk\jdk-17
D:\Dev\flutter
D:\Dev\Android\Sdk
D:\Dev\Android\Avd
D:\Dev\Gradle
```

已新增脚本：

```powershell
.\scripts\use-jdk17.ps1
.\scripts\use-flutter.ps1
```

脚本只影响当前 PowerShell 会话，不修改系统全局 Java 8。

## 正式工程运行

检查本地环境：

```powershell
.\scripts\check-env.ps1
```

检查真实 Provider 环境变量和密钥配置。默认采集关闭时会通过；启用真实采集前必须先通过该检查：

```powershell
.\scripts\check-provider-env.ps1
.\scripts\check-provider-env.ps1 -EnvFile .\.env.example
```

启动 PulseBrief MySQL。本项目使用宿主机 `3307` 端口，避免和本机已有 MySQL 的 `3306` 冲突：

```powershell
docker compose -f deploy/docker-compose.yml up -d
```

后端测试：

```powershell
.\scripts\use-jdk17.ps1
cd backend
.\mvnw.cmd test
```

Flutter 测试：

```powershell
.\scripts\use-flutter.ps1
cd mobile
flutter test
```

Admin 测试与构建：

```powershell
cd admin
npm test -- --run
npm run lint
npm run build
```

启动后端并查看 Swagger：

```powershell
.\scripts\use-jdk17.ps1
cd backend
.\mvnw.cmd spring-boot:run
```

访问：

```text
http://localhost:8080/swagger-ui.html
http://localhost:8080/v3/api-docs
```

Flutter 本机预览：

```powershell
cd mobile
$env:JAVA_HOME='D:\Dev\jdk\jdk-17'
$env:ANDROID_HOME='D:\Dev\Android\Sdk'
$env:ANDROID_SDK_ROOT='D:\Dev\Android\Sdk'
$env:ANDROID_USER_HOME='D:\Dev\Android\.android'
$env:ANDROID_AVD_HOME='D:\Dev\Android\Avd'
$env:GRADLE_USER_HOME='D:\Dev\Gradle'
$env:Path='D:\Dev\flutter\bin;D:\Dev\jdk\jdk-17\bin;D:\Dev\Android\Sdk\platform-tools;D:\Dev\Android\Sdk\emulator;D:\Dev\Android\Sdk\cmdline-tools\latest\bin;' + $env:Path
flutter devices
flutter run -d emulator-5554
```

构建 Android debug APK：

```powershell
cd mobile
flutter build apk --debug --target-platform android-x64
```

APK 输出路径：

```text
mobile\build\app\outputs\flutter-apk\app-debug.apk
```

## 正式 V1 分阶段计划

第一阶段：工程底座。

1. JDK 17 与 Flutter SDK 环境脚本。已完成。
2. Spring Boot 3 后端工程。已完成。
3. Flutter 移动端工程。已完成。
4. Docker Compose MySQL。已完成。
5. Maven Wrapper。已完成。

第二阶段：后端核心 API。

1. 数据库表结构和 Flyway migration。
2. 分类、文章、简报种子数据。
3. 首页、分类、详情、简报 API。
4. 收藏、订阅、播放历史 API。

第三阶段：Flutter 核心体验。

1. 登录、首页、分类、资讯详情、简报、我的。已完成 UI 首版。
2. 订阅页、语音播放器页。已完成 UI 首版。
3. 底部导航、底部迷你播放器、收藏、播放和订阅状态。已完成 mock 交互。
4. 客户端 TTS。
5. 登录和兴趣选择。

第四阶段：后台管理。

1. 数据看板占位。
2. 候选资讯审核。已完成。
3. 每日简报管理。已完成。
4. 采集任务监控、采集源启停、发布操作日志和异常数据检测。已完成。
5. 文章管理、分类管理和推送任务管理仍保留为后续增强入口。

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
5. [React/Vite PWA MVP 移除设计](./docs/工程裁剪与PWA移除设计.md)
6. [Flutter UI 设计方向稿](./docs/UI设计方向稿.md)
7. [Flutter 高保真 UI 实现设计](./docs/Flutter高保真UI实现设计.md)
8. [Flutter 高保真 UI 逐页精修设计](./docs/Flutter高保真UI逐页精修设计.md)
9. [Flutter 高保真 UI 截图验收记录](./docs/Flutter高保真UI截图验收记录.md)
10. [下一阶段任务清单](./docs/下一阶段任务清单.md)
11. [阶段 20 三端收口与发布前检查记录](./docs/阶段20三端收口与发布前检查记录.md)
12. [CI 验证流水线设计](./docs/CI验证流水线设计.md)
13. [真实 Provider 环境配置与密钥检查设计](./docs/真实Provider环境配置与密钥检查设计.md)
14. [真实 OpenAI 摘要 Provider 接入说明](./docs/真实OpenAI摘要Provider接入说明.md)

## 合规边界

PulseBrief 不做新闻全文搬运。首版只展示标题、来源、发布时间、短摘要、AI 解读和原文链接。投行公开观点只做短摘要，不展示完整报告或付费内容。
