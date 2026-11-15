package com.omniflow.android.ui.feature.more

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.designsystem.tokens.themePrimaryColor
import com.omniflow.core.domain.model.ThemeColor

/**
 * 迷你实时预览卡片（假卡片 + 假按钮），即时响应当前选中的主题色与明暗模式。
 */
@Composable
internal fun MiniAppearanceLivePreview(
    themeColor: ThemeColor,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val primary = themePrimaryColor(themeColor, isDark)
    val cardBackground = if (isDark) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    val textColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = cardBackground,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "本月结余",
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.7f),
                )
                Text(
                    "¥ 12,580.00",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = textColor,
                )
            }

            // 假记账按钮，使用即时选中的主题强调色
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = primary,
            ) {
                Text(
                    "+ 记一笔",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )
            }
        }
    }
}

internal fun themeColorLabel(color: ThemeColor): String = when (color) {
    ThemeColor.MIST_BLUE -> "雾蓝"
    ThemeColor.SAGE -> "鼠尾草"
    ThemeColor.LAVENDER -> "薰衣草"
    ThemeColor.SOFT_CORAL -> "柔珊瑚"
    ThemeColor.WARM_AMBER -> "暖琥珀"
    ThemeColor.GRAPHITE -> "石墨灰"
}
