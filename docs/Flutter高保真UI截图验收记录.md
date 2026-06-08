# Flutter 高保真 UI 截图验收记录

## 验收背景

本轮验收基于 Android 模拟器 `PulseBrief_Pixel`，分辨率 `1080x2400`，密度 `420`。目标是确认 Flutter 版本 8 个核心页面在真实移动端比例下接近原型，没有明显溢出、遮挡或错位，并记录后续精修事项。

截图目录：`D:\Projects\PulseBrief\.codex\screenshots`

## 验收结果

| 页面 | 截图文件 | 结论 | 问题等级 | 处理状态 |
| --- | --- | --- | --- | --- |
| 登录页 | `01_login.png` | 视觉层级、表单卡片、游客入口和协议区正常。 | 无 | 通过 |
| 首页 | `02_home.png` | 首页主卡、分类入口、资讯卡片和底部导航正常。首屏下方内容被底栏截断属于可滚动区域表现。 | P3 | 记录 |
| 分类页 | `03_category.png` | 分类切换、说明卡片、重点模块和底部导航正常。 | 无 | 通过 |
| 订阅页 | `04_subscription_after.png` | 推荐订阅标签修复后不再省略，保存按钮和底部导航正常。 | P2 | 已修复 |
| 资讯详情页 | `05_article_detail.png` | 标题、AI 摘要、核心要点、可能影响和底部操作栏正常。 | 无 | 通过 |
| 每日简报页 | `06_digest_after.png` | 简报主卡和分类列表正常，卡片描述修复后可读性提升。 | P2 | 已修复 |
| 语音播放器页 | `07_player.png` | 封面卡、播放进度、控制区和播报要点正常。 | 无 | 通过 |
| 我的页面 | `08_mine.png` | 会员入口、功能分组、简报偏好和底部导航正常。 | 无 | 通过 |

## 已修复问题

1. 订阅页推荐订阅标签在三列布局下出现 `全球...`、`财经...` 等省略展示。
   - 修复方式：`CategoryChip` 使用 `FittedBox` 做窄宽度内的轻量缩放，保持单行完整标签。

2. 每日简报页分类卡片描述在模拟器宽度下过早截断。
   - 修复方式：`DigestCard` 调整紧凑宽度阈值、按钮间距、图标尺寸和描述行数，保证关键文案可读。

## 暂不处理事项

1. Android 状态栏与 iOS 原型状态栏存在系统差异，当前不做仿 iOS 状态栏伪装。
2. 部分首屏截图底部会截到下一张卡片或被底部导航覆盖，但页面可滚动且安全区正常，暂按 P3 记录。
3. 当前插图为本地生成透明 PNG，已经移除矩形边界问题；后续如有品牌级 3D 资产，可进一步替换。

## 验证命令

```powershell
flutter analyze
flutter test
flutter build apk --debug --target-platform android-x64
```

验证结果：

- `flutter analyze`：No issues found
- `flutter test`：3 个测试全部通过
- `flutter build apk --debug --target-platform android-x64`：构建成功

