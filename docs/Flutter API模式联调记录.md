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
11. `GET /api/user/profile`
12. `GET /api/user/favorites`
13. `POST /api/user/read-history`
14. `GET /api/user/read-history`
15. `GET /api/playback/history`
16. `DELETE /api/user/read-history`
17. `DELETE /api/playback/history`

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
4. 用户中心资料接口已接入，API 模式“我的”页资料与统计不再直接依赖本地 `mockUser`。
5. 用户收藏、阅读历史和播放历史列表已接入，API 模式“我的”页三个入口可打开真实列表。
6. 用户列表接口已切换分页响应，Flutter API Repository 已兼容 `data.items`。
7. Android 模拟器 API 模式已完成 8 页面页面级截图验收。

## Android 模拟器页面级验收

运行命令：

```powershell
flutter build apk --debug --target-platform android-x64 --dart-define=PULSEBRIEF_DATA_SOURCE=api --dart-define=PULSEBRIEF_API_BASE_URL=http://10.0.2.2:8080/api
adb install -r .\build\app\outputs\flutter-apk\app-debug.apk
adb shell pm clear com.pulsebrief.pulsebrief
adb shell monkey -p com.pulsebrief.pulsebrief 1
```

截图目录：

```text
D:\Projects\PulseBrief\.codex\screenshots\api-mode
```

截图清单：

1. `01_login_api.png`：登录页。
2. `02_home_api.png`：首页。
3. `03_category_api.png`：分类页。
4. `04_subscription_api.png`：订阅页。
5. `05_article_detail_api.png`：资讯详情页。
6. `06_digest_api.png`：每日简报页。
7. `07_player_api.png`：语音播放器页。
8. `08_mine_api.png`：我的页面。

验收结果：

1. P0：未发现页面无法打开、白屏、崩溃、严重遮挡。
2. P1：未发现主导航、主按钮、详情页底部操作栏不可用问题。
3. P2：已修复 API `publishTime` 原始 ISO 字符串展示问题，改为 `昨天 09:30` 等短时间文案。
4. P2：已修复 API 模式分类首屏默认选中空分类导致页面大面积留白的问题，改为优先选择有内容的分类，并为无内容分类提供空状态。
5. P2：已修复 API 数字 digest id 导致简报列表图标无法区分的问题，改为按标题识别午间、晚间、AI、投行等类型。
6. P3：我的页用户统计已通过 `GET /api/user/profile` 替换为 API 数据；阅读历史统计当前按设计返回 `0`，后续阅读历史模块补齐。

## 用户中心资料联调补充

本轮新增接口：

```text
GET /api/user/profile
```

验证结果：

1. 未登录访问返回 401。
2. 登录后返回 `nickname`、`bio`、`subscriptionCount`、`favoriteCount`、`readCount` 和 `playCount`。
3. `subscriptionCount`、`favoriteCount`、`playCount` 来自 MySQL 真实表统计。
4. Flutter `ApiPulseRepository.getUserProfile()` live API 测试通过。
5. API 模式 Android debug APK 构建通过。

## 用户历史列表联调补充

本轮新增接口：

```text
GET  /api/user/favorites
POST /api/user/read-history
GET  /api/user/read-history
GET  /api/playback/history
DELETE /api/user/read-history
DELETE /api/playback/history
```

验证结果：

1. Flutter live API 测试已覆盖收藏写入后读取收藏列表。
2. Flutter live API 测试已覆盖阅读历史写入后读取阅读历史列表。
3. Flutter live API 测试已覆盖播放历史写入后读取播放历史列表。
4. “我的收藏”“阅读历史”“播放历史”三个页面在 mock 模式 widget 测试中可打开。
5. Flutter live API 测试已覆盖阅读历史和播放历史清空接口。
6. Flutter Repository 测试已覆盖分页响应 `items` 解析。
