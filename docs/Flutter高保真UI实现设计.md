# PulseBrief Flutter 高保真 UI 实现设计

## 背景与现状

PulseBrief 当前 Flutter 工程仍是默认 starter。用户已提供 7 张“脉闻 PulseBrief”高保真移动端原型图，要求先完成 Flutter 前端页面复现，当前阶段只使用 mock 数据，不接真实 Spring Boot API。

## 目标与非目标

目标：

1. 用 Flutter 原生页面复现首页、分类、订阅、资讯详情、每日简报、语音播放器、我的 7 个页面。
2. 建立正式项目结构，避免超长 `main.dart`。
3. 抽取 theme token、通用模型、mock 数据和共享组件。
4. 页面结构预留后续接入 Spring Boot API 的数据边界。
5. 实现分类切换、订阅切换、收藏切换、播放暂停、推送开关等基础状态。

非目标：

1. 不接真实后端接口。
2. 不实现真实登录、支付、会员、推送和 TTS。
3. 不展示新闻全文。
4. 不使用 H5 或 WebView。

## 影响范围

新增或调整：

1. `mobile/lib/app`
2. `mobile/lib/shared/theme`
3. `mobile/lib/shared/models`
4. `mobile/lib/shared/widgets`
5. `mobile/lib/features/*`
6. `mobile/lib/mock`
7. `mobile/assets/images`
8. `mobile/pubspec.yaml`
9. `mobile/test/widget_test.dart`

## 数据模型或权限模型

本阶段只定义前端 mock 模型：

1. `Article`
2. `NewsCategory`
3. `Digest`
4. `SubscriptionTopic`
5. `UserProfile`

权限模型不实现，只保留游客态 UI。

## 后端实现方案

本次不修改后端。Flutter 页面通过 mock 文件消费数据，后续可替换为 Repository / API Client。

## 前端影响

页面拆分：

1. `HomePage`：主入口，展示今日简报、快捷分类、投行观点、资讯流。
2. `CategoryPage`：按分类切换 mock 资讯。
3. `SubscriptionPage`：订阅标签和推送偏好状态。
4. `ArticleDetailPage`：结构化摘要、要点、影响和相关资讯。
5. `DigestPage`：简报卡片、分类简报、热点清单。
6. `PlayerPage`：资讯播报播放器，不做音乐化设计。
7. `MinePage`：用户信息、订阅、收藏、设置和协议入口。

共享组件：

1. `NewsCard`
2. `CategoryChip`
3. `DigestCard`
4. `MiniPlayer`
5. `PulseBottomNav`
6. `SourceBadge`
7. `SectionHeader`
8. `EmptyState`
9. `LoadingState`

## 测试与回归方案

本阶段执行：

```powershell
.\scripts\use-flutter.ps1
cd mobile
flutter analyze
flutter test
```

验收重点：

1. 7 个页面可打开。
2. 首页、分类、简报、我的底部导航可切换。
3. 订阅、分类、收藏、播放、推送开关有状态变化。
4. 列表底部不被导航栏和播放器遮挡。
5. Flutter analyze 无严重错误。

## 风险与分阶段落地建议

风险：

1. 原型图中的图片素材来自截图裁切，后续正式版本需要替换为版权清晰的内容图。
2. 当前未做真实设备截图对比，视觉仍需二轮微调。
3. 当前播放器只是 UI 状态，后续接入系统 TTS 时需补生命周期管理。

建议：

1. 本阶段先完成 Flutter 静态高保真 UI。
2. 下一阶段补 UI 截图验收和关键页面 widget 测试。
3. 再接入后端 API 与真实 TTS。
