# PulseBrief Mobile

`mobile/` 是 PulseBrief 正式用户端 Flutter APP，不使用 H5 或 WebView。当前已完成 8 个高保真 mock 页面：

1. 登录页
2. 首页
3. 分类页
4. 订阅页
5. 资讯详情页
6. 每日简报页
7. 语音播放器页
8. 我的页面

页面使用统一主题 tokens、通用组件、mock 数据和路由配置，后续会通过 Repository 层切换到 Spring Boot API。

## 本机环境

当前推荐把移动端工具链放在 D 盘：

```text
D:\Dev\flutter
D:\Dev\jdk\jdk-17
D:\Dev\Android\Sdk
D:\Dev\Android\Avd
D:\Dev\Gradle
```

常用环境变量：

```powershell
$env:JAVA_HOME='D:\Dev\jdk\jdk-17'
$env:ANDROID_HOME='D:\Dev\Android\Sdk'
$env:ANDROID_SDK_ROOT='D:\Dev\Android\Sdk'
$env:ANDROID_USER_HOME='D:\Dev\Android\.android'
$env:ANDROID_AVD_HOME='D:\Dev\Android\Avd'
$env:GRADLE_USER_HOME='D:\Dev\Gradle'
$env:Path='D:\Dev\flutter\bin;D:\Dev\jdk\jdk-17\bin;D:\Dev\Android\Sdk\platform-tools;D:\Dev\Android\Sdk\emulator;D:\Dev\Android\Sdk\cmdline-tools\latest\bin;' + $env:Path
```

也可以先使用根目录脚本加载 Flutter：

```powershell
..\scripts\use-flutter.ps1
```

## 模拟器预览

启动已配置的 Android 模拟器：

```powershell
& 'D:\Dev\Android\Sdk\emulator\emulator.exe' -avd PulseBrief_Pixel
```

查看设备：

```powershell
flutter devices
```

运行 APP：

```powershell
flutter run -d emulator-5554
```

如果只想安装已构建 APK：

```powershell
flutter build apk --debug --target-platform android-x64
& 'D:\Dev\Android\Sdk\platform-tools\adb.exe' install -r .\build\app\outputs\flutter-apk\app-debug.apk
& 'D:\Dev\Android\Sdk\platform-tools\adb.exe' shell monkey -p com.pulsebrief.pulsebrief 1
```

APK 输出路径：

```text
D:\Projects\PulseBrief\mobile\build\app\outputs\flutter-apk\app-debug.apk
```

## 质量检查

```powershell
flutter analyze
flutter test
flutter build apk --debug --target-platform android-x64
```

当前验证结果记录在：

```text
D:\Projects\PulseBrief\docs\Flutter高保真UI截图验收记录.md
```

## 透明插图资产

当前主视觉插图为本地生成的透明 PNG，用于避免原始 `art_*` 素材的矩形边界。

生成脚本：

```powershell
python ..\scripts\generate_clean_art_assets.py
```

输出目录：

```text
D:\Projects\PulseBrief\mobile\assets\images
```

已使用的主要资产：

```text
art_clean_global.png
art_clean_finance.png
art_clean_digest.png
art_clean_player.png
art_clean_subscription.png
```

## 当前边界

1. 当前阶段只使用 mock 数据，不接真实后端。
2. 不实现真实登录、支付、研报下载或新闻全文展示。
3. Android 模拟器是当前主要验收环境，Flutter Web 只作为辅助预览。
4. 后续进入 Spring Boot API 后，页面不直接调用接口，统一从 Repository 层获取数据。
