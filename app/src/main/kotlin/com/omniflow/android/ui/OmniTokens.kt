package com.omniflow.android.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 全局设计 token。
 *
 * 之前的问题是层级全靠颜色堆：支出红、收入绿、主题色紫、error、tertiary 同屏打架，
 * 而字号只差 2-4sp，表面又全是同一个 surfaceVariant 加描边。这里把层级交给
 * 「表面明度 + 字号字重」，颜色只留给真正有语义的地方。
 */

/** 圆角只留三档，避免出现 4/6/8/12/18/20/24 混用。 */
object OmniRadius {
    val small = 10.dp
    val medium = 16.dp
    val large = 22.dp
}

object OmniSpace {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
}

@Composable
internal fun isDarkUi(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

/** 页面底色。 */
@Composable
internal fun surfaceBase(): Color = MaterialTheme.colorScheme.background

/** 卡片色，比页面底亮一档。 */
@Composable
internal fun surfaceCard(): Color = MaterialTheme.colorScheme.surface

/** 卡片内嵌块、chip 轨道，比卡片再亮一档。 */
@Composable
internal fun surfaceInset(): Color = MaterialTheme.colorScheme.surfaceVariant

/** 次要文字统一走这个，不要再各处手写 alpha。 */
@Composable
internal fun mutedContent(): Color = MaterialTheme.colorScheme.onSurfaceVariant

/**
 * 字体阶梯。对标产品的金额和标签是 2 倍字号差，这里照着拉开，
 * 不再让金额和标题挤在同一档。
 */
object OmniText {
    /** 净资产、详情页主数字。 */
    val amountHero: TextStyle
        @Composable get() = MaterialTheme.typography.headlineMedium.copy(
            fontSize = 30.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Bold,
        )

    /** 汇总卡数值、明细行金额。 */
    val amountPrimary: TextStyle
        @Composable get() = MaterialTheme.typography.titleLarge.copy(
            fontSize = 22.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.Bold,
        )

    /** 卡片标题、列表行标题。 */
    val titleRow: TextStyle
        @Composable get() = MaterialTheme.typography.titleMedium.copy(
            fontSize = 16.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
        )

    val bodyRow: TextStyle
        @Composable get() = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 18.sp)

    /** 标签、时间、说明、分组标题。 */
    val caption: TextStyle
        @Composable get() = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 15.sp)
}

/**
 * 图表调色板。原来的图表直接用主题色和 error/tertiary，饱和度高、互相打架；
 * 换成低饱和的沙 / 鼠尾草 / 珊瑚 / 雾蓝一组。
 */
@Composable
internal fun chartPalette(): List<Color> = if (isDarkUi()) {
    listOf(
        Color(0xFFD9CBB0),
        Color(0xFF97B3A4),
        Color(0xFFD98A80),
        Color(0xFF8FA6BD),
        Color(0xFFC2A9C8),
        Color(0xFFC9B98C),
    )
} else {
    listOf(
        Color(0xFFE5D9C3),
        Color(0xFFA8C0B0),
        Color(0xFFE8938A),
        Color(0xFF9BB2C9),
        Color(0xFFCDB6D3),
        Color(0xFFD8C79A),
    )
}
