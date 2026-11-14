package com.omniflow.android.ui.designsystem.tokens

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Material 3 Expressive 低饱和粉彩图表分类调色板 (12 色)。
 * 适用于统计页 DonutChart、CategoryShareCard、HorizontalDivergingBarChart、SideBySideBarChart 等。
 */
object ChartPalette {
    val LightPalette = listOf(
        Color(0xFFE8938A), // 珊瑚桃
        Color(0xFFE0B579), // 沙金
        Color(0xFFA2C7A8), // 鼠尾草绿
        Color(0xFF90B9D6), // 薄雾蓝
        Color(0xFFBCA6D9), // 薰衣草紫
        Color(0xFF88C9BE), // 薄荷青
        Color(0xFFEBB090), // 暖杏粉
        Color(0xFFD69EB8), // 灰粉梅
        Color(0xFFB5C98A), // 柔和橄榄
        Color(0xFFA1B3D6), // 钢灰长春花
        Color(0xFFCEB085), // 暖赭石
        Color(0xFFC3B0A8), // 灰褐粉
    )

    val DarkPalette = listOf(
        Color(0xFFEFA59E),
        Color(0xFFEAC28A),
        Color(0xFFB4D4BA),
        Color(0xFFA4C7DF),
        Color(0xFFCAB8E2),
        Color(0xFF9DD4CB),
        Color(0xFFF0BF9F),
        Color(0xFFE0AFC5),
        Color(0xFFC4D59E),
        Color(0xFFB3C2DF),
        Color(0xFFDAC19C),
        Color(0xFFD0C0B9),
    )

    fun palette(isDark: Boolean): List<Color> = if (isDark) DarkPalette else LightPalette
}

@Composable
@ReadOnlyComposable
fun chartColors(): List<Color> =
    ChartPalette.palette(MaterialTheme.colorScheme.background.luminance() < 0.5f)
