# PulseBrief Flutter API 模式联调记录

## 背景与目标

Flutter 已新增 Repository 数据层，后端已完成 Spring Boot + MySQL V1 用户端接口骨架。本轮目标是验证 Flutter `ApiPulseRepository` 能访问本机 Spring Boot API，并覆盖登录、首页、分类、详情、简报、订阅、收藏和播放历史的主链路。

## 联调环境

本机服务：

1. MySQL：`pulsebrief-mysql`，端口 `3307`。
2. Spring Boot：`http://localhost:8080/api`。
3. Android 模拟器访问宿主机后端时使用：`http://10.0.2.2:8080/api`。

Flutter API 模式参数：

```powershell
--dart-define=PULSEBRIEF_DATA_SOURCE=api
--dart-define=PULSEBRIEF_API_BASE_URL=http://10.0.2.2:8080/api
```

本机测试 API Repository 时使用：

```powershell
flutter test --dart-define=PULSEBRIEF_LIVE_API=true --dart-define=PULSEBRIEF_API_BASE_URL=http://localhost:8080/api test/live_api_repository_test.dart
```

## 已验证接口

已通过真实 HTTP 冒烟：

1. `POST /api/auth/login`
2. `GET /api/categories`
3. `GET /api/articles/home`
4. `GET /api/articles`
5. `GET /api/articles/{id}`
6. `GET /api/digests/today`
7. `GET /api/user/subscriptions`
8. `PUT /api/user/subscriptions`
9. `POST /api/articles/{id}/favorite`
10. `POST /api/playback/history`

## 本轮发现与修复

### 订阅保存唯一键冲突

现象：

真实 MySQL 下重复保存订阅时，`user_subscription.uk_user_category` 触发唯一键冲突。

原因：

`saveSubscriptions` 在同一事务中先 `deleteByUserId` 再插入新记录，但删除未及时 flush，后续插入撞到旧记录唯一键。

修复：

在删除后执行 `subscriptionRepository.flush()`，确保旧订阅行先写入数据库删除结果，再插入新订阅。

回归：

新增 `SubscriptionApplicationServiceTest`，验证保存订阅时执行顺序为 `deleteByUserId -> flush -> save`。

### Flutter JSON 请求体编码

现象：

PowerShell 冒烟脚本发送中文播放标题时出现非 UTF-8 请求体。Flutter 侧为避免平台差异，显式使用 `utf8.encode(jsonEncode(body))` 写入 JSON。

修复：

`HttpPulseApiTransport` 请求体写入改为 UTF-8 字节写入。

回归：

`live_api_repository_test` 使用中文 `playTitle` 写入播放历史，真实 API 测试通过。

## 当前结论

1. Flutter mock 模式仍为默认模式，本机预览不依赖后端。
2. Flutter API Repository 已可对接 Spring Boot V1 API。
3. 后端订阅保存与播放历史写入已通过真实 MySQL + Spring Boot 冒烟。
4. 下一步可进入 Android 模拟器 API 模式运行，做真实 APP 页面级端到端验收。

