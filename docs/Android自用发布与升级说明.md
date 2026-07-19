# Android 自用发布与升级说明

## 当前范围

PulseBrief 当前仅供个人使用，不接入 Google Play、App Store、FCM 或 APNs。Android 使用本机独立 release 证书签名，通过 APK 侧载安装。

## 本机目录

以下内容已被 Git 忽略，不会提交到仓库：

```text
D:\Projects\PulseBrief\.local-secrets\android\pulsebrief-self-use.jks
D:\Projects\PulseBrief\mobile\android\key.properties
D:\Projects\PulseBrief\artifacts\mobile\
```

必须把 JKS 和 `key.properties` 一起备份到加密 U 盘或其他加密磁盘。丢失其中任一文件后，新 APK 将无法作为原应用的升级包安装。

## 构建

首次配置签名：

```powershell
.\scripts\setup-android-self-signing.ps1
```

构建内置 mock 数据的自用版：

```powershell
.\scripts\build-android-self-release.ps1 -BuildName 1.0.0 -DataSource mock
```

连接局域网内的本机 API：

```powershell
.\scripts\build-android-self-release.ps1 `
  -BuildName 1.0.1 `
  -DataSource api `
  -ApiBaseUrl http://<电脑局域网IP>:8080/api
```

真机不能使用模拟器专用的 `10.0.2.2`。如果使用 HTTP API，还需要单独确认 Android 明文流量策略；优先使用局域网 HTTPS。

脚本会自动生成递增的 `versionCode`，并在 `artifacts/mobile/` 输出：

- 版本化 APK；
- `.sha256` 校验文件；
- `.json` 构建元数据。

## 安装与升级

连接 Android 设备并开启 USB 调试后：

```powershell
& 'D:\Dev\Android\Sdk\platform-tools\adb.exe' devices
& 'D:\Dev\Android\Sdk\platform-tools\adb.exe' install -r .\artifacts\mobile\PulseBrief-<version>-<build>-mock-release.apk
```

如果设备已经安装旧的 debug 签名版本，第一次切换到自用 release 签名时会提示签名不一致。需要先备份应用内数据，再手动卸载旧版并安装 release 版；此后只要保留同一 JKS，即可使用 `install -r` 覆盖升级。

## 验证

APK 构建后应验证：

1. `apksigner verify --verbose --print-certs` 返回成功；
2. APK SHA-256 与同名 `.sha256` 文件一致；
3. `versionCode` 高于设备上的旧版本；
4. 真机完成启动、登录、文章、简报和播放基础冒烟。
