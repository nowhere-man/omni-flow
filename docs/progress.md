# OmniFlow 实现进度

更新时间：2026-08-16

本文档记录 Android-only 双模块重构后的工作区状态；需求与验收范围仍以 `docs/1.Requirements.md`、`docs/2.Architecture.md`、`docs/3.TechDesign.md` 和 `docs/4.Goal.md` 为准。

## 当前结论

- 项目采用 `:core` Android library + `:app` Android application。
- 业务源码与测试已迁移到 `com.omniflow.core`；UI 仍使用 `com.omniflow.android`。
- 解析器实现已合并到 core 单一 `src/main` source set，JDBC 驱动仅用于测试。

## 已实现范围

### Core

- SQLDelight 领域存储、迁移、软删除、账户余额校准与交易余额联动。
- 账本、账户、分类、标签、规则、提醒和应用偏好管理。
- 首页、日期明细、搜索、统计、导入预览、规则、分类记忆、去重和青子互通业务能力。
- 支付宝、微信、京东、美团、建设银行和青子账单解析，以及 WebDAV/备份同步能力。

### App

- Jetpack Compose + Material 3 五项主导航：首页、统计、记账、搜索、更多。
- 交易编辑、管理页、提醒、导入预览、青子导出、WebDAV 备份恢复、应用锁和外观偏好。

## 验证记录

- `./gradlew :core:testDebugUnitTest`
- `./gradlew :core:verifySqlDelightMigration`
- `./gradlew :app:compileDebugKotlin`

## 首版外范围

- 本次不构建 APK；后续只维护 Android 双模块结构。
- 抖音 PDF 账单导入、撤销上一次导入、预算、多币种和复杂 RRULE。
