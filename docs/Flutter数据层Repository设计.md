# PulseBrief Flutter Repository 数据层设计

## 背景与现状

Flutter 移动端已完成 8 个高保真 mock 页面，Spring Boot 后端已完成 V1 用户端 API 骨架。当前页面直接引用 `mock/` 数据文件，下一步需要新增 Repository 层，保证默认 mock 体验不变，同时为后续切换 Spring Boot API 做好结构准备。

## 目标与非目标

目标：

1. 新增统一 `PulseRepository` 接口，覆盖登录、分类、首页资讯、文章详情、简报、订阅、收藏、播放历史。
2. 默认使用 `MockPulseRepository`，保持当前 UI 和测试稳定。
3. 新增 `ApiPulseRepository`，通过 `--dart-define` 切换到 Spring Boot API。
4. 页面不直接散落 HTTP 调用。
5. API DTO 到 UI model 做映射，避免后端字段直接污染 widget。

非目标：

1. 本阶段不重做页面视觉。
2. 不引入复杂状态管理库。
3. 不实现生产登录、刷新 token、加密存储或离线缓存。
4. 不移除现有 mock 数据文件。

## 影响范围

Flutter 新增目录：

```text
mobile/lib/core/network/
mobile/lib/shared/repositories/
mobile/lib/shared/data/
```

Flutter 调整：

1. `PulseBriefApp` 注入 repository。
2. 登录页通过 repository 执行 mock 登录和游客模式。
3. 首页、分类页、简报页、订阅页优先从 repository 取初始数据。
4. 保留本地收藏、播放、订阅交互状态，后续再做完整服务端同步。

## 数据模型与权限模型

新增轻量模型：

1. `AuthSession`：登录 token、token 类型、用户信息。
2. `HomeFeed`：首页简报主卡、投行观点推荐、文章列表。
3. `TodayDigestFeed`：日期、主简报、简报列表、热点清单。
4. `SubscriptionSnapshot`：订阅主题、推送偏好、今日匹配数。

权限：

1. mock repository 不校验 token。
2. API repository 登录后保存内存态 token。
3. 订阅、收藏、播放历史调用 API 时带 `Authorization: Bearer <token>`。
4. token 持久化后置，不在本阶段实现。

## API 切换方案

默认 mock：

```powershell
flutter run
```

切换 API：

```powershell
flutter run --dart-define=PULSEBRIEF_DATA_SOURCE=api --dart-define=PULSEBRIEF_API_BASE_URL=http://10.0.2.2:8080/api
```

常量：

```text
PULSEBRIEF_DATA_SOURCE=mock|api
PULSEBRIEF_API_BASE_URL=http://10.0.2.2:8080/api
```

## 测试与回归方案

自动化测试：

1. `MockPulseRepository` 返回当前 UI 所需 mock 数据。
2. `PulseRepositoryFactory` 默认返回 mock，dart-define 为 api 时返回 API repository。
3. `ApiPulseRepository` 能把 V1 API 响应映射为 UI model。
4. 现有 widget 测试保持通过。

手工回归：

1. mock 模式下 8 个页面正常打开。
2. API 模式下登录、首页、分类、简报和订阅初始数据可从后端返回。

## 风险与分阶段落地

风险：

1. Android 模拟器访问宿主机后端需要使用 `10.0.2.2`，不是 `localhost`。
2. 当前 API seed 的图片为空，Flutter 仍需使用本地图片兜底。
3. 后端时间是 ISO 字符串，当前 UI 显示相对时间，首轮映射为来源返回字符串或简化文案。

分阶段：

1. 新增 Repository 接口、mock 实现、API 实现和测试。
2. 登录页、首页、分类页、简报页、订阅页接入 repository 初始数据。
3. 保留详情页、播放器页通过路由参数承接现有 Article。
4. 后续再增加持久 token、错误重试和离线缓存。

