package com.omniflow.android.ui.designsystem.tokens

import androidx.compose.ui.unit.dp

/**
 * OmniFlow 全局图标设计规范（Material Icons 体系）。
 * 全应用统一采用 Material Icons（Filled）风格，禁止引入自绘 SVG 图标集。
 */
object OmniIconTokens {
    /** 紧凑图标 (16.dp): 辅助标签、状态徽章、输入框右侧辅助图标 */
    val small = 16.dp

    /** 标准图标 (24.dp): 顶部导航、按钮 Leading 图标、输入框主图标、底栏导航图标 */
    val medium = 24.dp

    /** 重点图标 (32.dp): 分类选择大图标、快捷入口、卡片醒目图标 */
    val large = 32.dp

    /** 巨型图标 (48.dp): 空状态插画、Hero 卡片主装饰 */
    val extraLarge = 48.dp

    /** Expressive 列表项图标徽章容器尺寸 (40.dp) */
    val badgeContainerSize = 40.dp

    /** Expressive 列表项图标徽章图标尺寸 (20.dp) */
    val badgeIconSize = 20.dp

    /** 分类圆形微图尺寸 */
    val categoryBadgeSize = 40.dp
    val categoryIconSize = 24.dp
}

