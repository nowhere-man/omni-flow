# OmniFlow

[![Build Three Platforms](https://github.com/nowhere-man/OmniFlow/actions/workflows/release.yml/badge.svg)](https://github.com/nowhere-man/OmniFlow/actions/workflows/release.yml)

OmniFlow 是一款面向 Android、iOS 和 macOS 的本地优先记账应用。它通过导入支付平台账单代替繁琐的逐笔录入，并使用平台原生界面提供一致、流畅的多端体验。

## 获取构建

- [GitHub Releases](https://github.com/nowhere-man/OmniFlow/releases)：下载版本标签对应的三端产物；
- [GitHub Actions](https://github.com/nowhere-man/OmniFlow/actions/workflows/release.yml)：下载最新提交的 Android、iOS Simulator 和 macOS 构建产物。

## 核心优势

### 1. 导入账单，不必逐笔记账

直接导入常用支付平台和银行账单，OmniFlow 会完成来源识别、字段解析、规则匹配、分类记忆和重复检测。用户只需检查导入预览并确认入账。

| 来源 | 文件格式 | 支持情况 |
| --- | --- | --- |
| 支付宝 | CSV（GBK） | 支持 |
| 微信 | XLSX | 支持 |
| 京东 | CSV（UTF-8 BOM） | 支持 |
| 美团 | CSV（UTF-8 BOM） | 支持 |
| 建设银行 | XLS（Excel 97-2003） | 支持 |
| 青子记账 | JSON | 支持导入与导出 |

导入流程支持：

- 自动识别账单来源和账户；
- 自定义规则与历史分类记忆；
- 外部订单号绝对去重和相似交易检测；
- 导入前逐条编辑、批量分类和批量排除；
- 单个数据库事务入账，失败时完整回滚。

### 2. 本地优先，多端同步

数据默认保存在设备本地，没有网络也能正常记账、搜索和统计。配置同步目标后，可自动或手动创建完整备份，并在其他设备恢复。

- iOS、macOS：iCloud、WebDAV；
- Android：WebDAV；
- Android、iOS、macOS 可通过 WebDAV 使用同一套备份；
- 支持自动延迟备份、手动备份、历史记录和覆盖恢复；
- 同步目标由用户管理，不依赖 OmniFlow 自建服务器。

> 当前版本的远端备份数据未加密，请仅使用可信的 iCloud 或 WebDAV 服务。

### 3. 三端原生应用

OmniFlow 共享业务逻辑，但不使用跨平台 UI。每个平台都采用原生技术栈和交互方式：

| 平台 | UI 技术 | 平台体验 |
| --- | --- | --- |
| Android | Kotlin、Jetpack Compose、Material 3 | Android 原生导航、文件选择、通知与 Keystore |
| iOS | SwiftUI | iOS 原生 Tab、文件选择、通知、Keychain 与应用锁 |
| macOS | SwiftUI | Sidebar、菜单、快捷键、设置窗口与桌面布局 |

共享层使用 Kotlin Multiplatform，确保账单解析、规则、去重、统计和同步在三端保持一致。

## 功能

- 多账本、账户、分类、标签和资产管理；
- 收入、支出和不计入收支交易；
- 首页月度日历、日期明细和交易筛选；
- 趋势、同比、环比、分类占比、排行、资产分布和年度账单；
- 关键词、金额、日期、账户、分类和标签组合搜索；
- 导入规则、分类记忆和重复检测；
- 还款与订阅提醒；
- 青子记账 JSON 导入与导出；
- iCloud/WebDAV 备份与恢复；
- 系统外观、应用锁和敏感凭据安全存储。

## 技术架构

```text
                  iCloud / WebDAV
                         ▲
                         │
              Kotlin Multiplatform
       领域模型 · SQLDelight · 导入 · 统计 · 同步
                    ┌────┴────┐
                    │         │
             SwiftUI App   Android App
              iOS/macOS    Jetpack Compose
```

主要技术：

- Kotlin Multiplatform；
- SwiftUI；
- Jetpack Compose + Material 3；
- SQLDelight + SQLite；
- Kotlin Coroutines、Flow 和 kotlinx.serialization；
- Apache POI 与 Apple 原生账单解析实现。

## 项目结构

```text
OmniFlow/
├── androidApp/   # Android 原生应用
├── appleApp/     # iOS 与 macOS SwiftUI 工程
├── shared/       # KMP 共享业务、存储、解析和同步
├── assets/       # 跨端 SVG 图标资源
├── docs/         # 需求、架构、技术设计和验收目标
└── .github/      # 三端 GitHub Actions 构建
```

## 构建环境

- JDK 17；
- Android SDK 35；
- Android Studio，或可用的 Gradle/Android SDK 环境；
- 构建 Apple 应用需要完整 Xcode；
- Android 最低版本：Android 8.0（API 26）；
- iOS 最低版本：iOS 16；
- macOS 最低版本：macOS 13。

## 构建 Android

```bash
./gradlew :androidApp:assembleDebug
```

APK 输出位置：

```text
androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

## 构建 iOS Simulator

Apple Silicon：

```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64

xcodebuild build \
  -project appleApp/OmniFlow.xcodeproj \
  -scheme OmniFlow-iOS \
  -configuration Debug \
  -sdk iphonesimulator \
  -destination "generic/platform=iOS Simulator" \
  -derivedDataPath build/ios-derived-data \
  ARCHS=arm64 \
  ONLY_ACTIVE_ARCH=YES \
  FRAMEWORK_SEARCH_PATHS="$PWD/shared/build/bin/iosSimulatorArm64/debugFramework" \
  CODE_SIGNING_ALLOWED=NO
```

Intel Mac 将框架任务改为 `linkDebugFrameworkIosX64`，并将架构和框架目录改为 `x86_64` 与 `iosX64`。

## 构建 macOS

Apple Silicon：

```bash
./gradlew :shared:linkDebugFrameworkMacosArm64

xcodebuild build \
  -project appleApp/OmniFlow.xcodeproj \
  -scheme OmniFlow-macOS \
  -configuration Debug \
  -sdk macosx \
  -derivedDataPath build/macos-derived-data \
  ARCHS=arm64 \
  ONLY_ACTIVE_ARCH=YES \
  FRAMEWORK_SEARCH_PATHS="$PWD/shared/build/bin/macosArm64/debugFramework" \
  CODE_SIGNING_ALLOWED=NO
```

Intel Mac 将框架任务改为 `linkDebugFrameworkMacosX64`，并将架构和框架目录改为 `x86_64` 与 `macosX64`。

## GitHub Actions

[`Build Three Platforms`](https://github.com/nowhere-man/OmniFlow/actions/workflows/release.yml) 会在提交到 `master`、Pull Request、版本标签和手动触发时构建三端产物：

- Android 签名 Release APK；
- iOS Simulator App 压缩包；
- macOS App 压缩包。

推送 `v*` 标签后，三个产物会自动附加到对应的 GitHub Release。

Android 签名 Release 构建需要在仓库 Secrets 中配置：

- `ANDROID_KEYSTORE_BASE64`；
- `ANDROID_KEYSTORE_PASSWORD`；
- `ANDROID_KEY_ALIAS`；
- `ANDROID_KEY_PASSWORD`。

所有 Pull Request 的 Android Job 均构建 Debug APK，不读取签名 Secrets；iOS 和 macOS Job 仍正常构建。

## 文档

- [产品需求](docs/1.Requirements.md)
- [技术架构](docs/2.Architecture.md)
- [技术设计](docs/3.TechDesign.md)
- [验收目标](docs/4.Goal.md)
- [实现进度](docs/progress.md)

## 贡献

提交改动前，请至少确认对应平台能够编译，并保持账单解析、规则、去重、统计和同步逻辑位于共享层。Bug 报告和功能建议可通过 GitHub Issues 提交。
