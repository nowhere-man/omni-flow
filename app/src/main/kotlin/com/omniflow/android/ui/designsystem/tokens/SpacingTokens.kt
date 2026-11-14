package com.omniflow.android.ui.designsystem.tokens

import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive 间距令牌。
 *
 * 依据参考图实测：
 * - 深色 Google 账号页 (google-account-m3e-dark.jpg, 1206x2613 px, @3x 比例 1dp=3px)
 *   页面横向边距：50-51px -> 16.dp
 *   分组卡片纵向间距：50-51px -> 16.dp
 *   组内列表行最小行高：213px -> 71dp (M3 ListItem 标准 72.dp)
 *   组内行与行贴合排列：0.dp (仅细分割线隔开)
 *   分割线缩进：起始 72.dp (避开 16dp 左边距 + 40dp 徽章 + 16dp 间隙)，末尾 16.dp
 * - 浅色 Gmail 设置页 (gmail-m3e-settings.jpg)
 *   同样呈现页面左右边距 16dp、组内贴合无额外间隙、组间大间距的层次体系。
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

    /** 同一分组卡片内行与行的间距: 0.dp (组内行贴合排列) */
    val intraGroupSpacing = 0.dp

    /** 设置行标准最小高度: 72.dp (M3 两行 ListItem 标准) */
    val settingsRowMinHeight = 72.dp

    /** 分割线文字起始缩进: 72.dp */
    val dividerIndentStart = 72.dp

    /** 分割线末尾缩进: 16.dp */
    val dividerIndentEnd = 16.dp
}
