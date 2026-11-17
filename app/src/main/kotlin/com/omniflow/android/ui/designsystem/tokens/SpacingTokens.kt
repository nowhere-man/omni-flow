package com.omniflow.android.ui.designsystem.tokens

import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive 间距令牌。
 *
 * 间距规格口径 (Jack 拍板 2026-11-15 修正决策，取代第五轮组内 0dp 贴合读数)：
 * - 页面横向外侧边距：16.dp (OmniSpacingTokens.pageHorizontal)
 * - 分组之间的纵向间距：16.dp (OmniSpacingTokens.groupSpacing)
 * - 分组内部行与行纵向间距：2.dp (OmniSpacingTokens.intraGroupSpacing)，
 *   组内每行为独立圆角卡片，透出底层页面背景，不使用分割线 (对齐 Google 账号页实测，2026-11-17)
 * - 列表行标准最小行高：72.dp (OmniSpacingTokens.settingsRowMinHeight)
 */
object OmniSpacingTokens {
    val none = 0.dp
    val extraSmall = 4.dp
    val small = 8.dp
    val mediumSmall = 12.dp
    val medium = 16.dp
    val large = 20.dp
    val extraLarge = 24.dp
    val xxl = 32.dp

    // --- 语义化间距绑定 ---
    /** 页面外侧横向边距: 16.dp */
    val pageHorizontal = 16.dp

    /** 页面顶部与底部留白: 16.dp */
    val pageVertical = 16.dp

    /** 分组卡片之间的纵向间距: 16.dp */
    val groupSpacing = 16.dp

    /** 同一分组内行与行的间距: 2.dp (透出页面背景，对齐 Google 账号页实测) */
    val intraGroupSpacing = 2.dp

    /** 设置行标准最小高度: 72.dp (M3 两行 ListItem 标准) */
    val settingsRowMinHeight = 72.dp
}
