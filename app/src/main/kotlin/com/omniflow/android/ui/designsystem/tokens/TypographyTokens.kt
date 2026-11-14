package com.omniflow.android.ui.designsystem.tokens

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object OmniTypographyTokens {
    /** 金额展示必须启用等宽数字（Tabular Figures），防止字符宽度抖动 */
    const val FontFeatureSettingsTnum = "tnum"

    /** 净资产、大额重点展示 (30sp/36sp, Bold, tnum) */
    val amountHero: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.headlineMedium.copy(
            fontSize = 30.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Bold,
            fontFeatureSettings = FontFeatureSettingsTnum,
        )

    /** 汇总卡数值、明细主金额 (22sp/26sp, Bold, tnum) */
    val amountPrimary: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.titleLarge.copy(
            fontSize = 22.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.Bold,
            fontFeatureSettings = FontFeatureSettingsTnum,
        )

    /** 紧凑卡片金额 (18sp/22sp, SemiBold, tnum) */
    val amountSecondary: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.titleMedium.copy(
            fontSize = 18.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.SemiBold,
            fontFeatureSettings = FontFeatureSettingsTnum,
        )

    /** 辅助金额 / 小标签金额 (14sp/18sp, Medium, tnum) */
    val amountSmall: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.bodyMedium.copy(
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium,
            fontFeatureSettings = FontFeatureSettingsTnum,
        )

    /** 设置分组标题 (14sp/20sp, Medium) 对齐 Google M3E 设置小节标签 */
    val settingsGroupTitle: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.titleSmall.copy(
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium,
        )

    /** 设置行标题 (16sp/24sp, Medium) 对齐 Google M3E 设置项标题 */
    val settingsRowTitle: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.titleMedium.copy(
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Medium,
        )

    /** 设置行副标题 (14sp/20sp, Normal) 对齐 Google M3E 辅助说明文字 */
    val settingsRowSubtitle: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.bodyMedium.copy(
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Normal,
        )

    /** 设置行紧凑辅助文字 (12sp/16sp, Normal) */
    val settingsRowSupporting: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.bodySmall.copy(
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Normal,
        )
}
