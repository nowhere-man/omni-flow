package com.omniflow.android.ui.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.designsystem.tokens.OmniShapeTokens
import com.omniflow.android.ui.designsystem.tokens.OmniSpacingTokens

/**
 * Material 3 Expressive 全页统一空态占位。
 *
 * 规格契约：
 * - 96dp Canvas 绘制的低饱和几何粉彩插画，或 48dp 浅色图标徽章
 * - 现状句 (status line)
 * - 价值引导句 (value line)
 * - 胶囊 CTA 操作按钮 (可选)
 */
@Composable
fun OmniEmptyState(
    status: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val primary = MaterialTheme.colorScheme.primary
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(OmniSpacingTokens.pageHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(24.dp),
                )
            }
        } else {
            // 96dp Canvas 粉彩几何插画
            Canvas(modifier = Modifier.size(96.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                // 底层柔和光环
                drawCircle(
                    color = primaryContainer.copy(alpha = 0.6f),
                    radius = size.width * 0.42f,
                    center = center,
                )
                // 内层浮动几何方块/小圆点
                drawCircle(
                    color = primary.copy(alpha = 0.25f),
                    radius = size.width * 0.22f,
                    center = Offset(center.x - 14f, center.y - 12f),
                )
                // 装饰弧线
                drawArc(
                    color = primary,
                    startAngle = 40f,
                    sweepAngle = 200f,
                    useCenter = false,
                    topLeft = Offset(center.x - 28f, center.y - 28f),
                    size = Size(56f, 56f),
                    style = Stroke(width = 4f, cap = StrokeCap.Round),
                )
                // 轻盈小浮星
                drawCircle(
                    color = outlineVariant,
                    radius = 5f,
                    center = Offset(center.x + 24f, center.y + 18f),
                )
            }
        }

        Spacer(Modifier.height(OmniSpacingTokens.large))

        Text(
            text = status,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        if (value.isNotBlank()) {
            Spacer(Modifier.height(OmniSpacingTokens.extraSmall))

            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(OmniSpacingTokens.extraLarge))
            Button(
                onClick = onAction,
                shape = OmniShapeTokens.button,
            ) {
                Text(actionLabel)
            }
        }
    }
}

/**
 * 具有图标插画的空态便捷调用重载 (兼容 message / subtitle 命名)。
 */
@Composable
fun OmniEmptyState(
    icon: ImageVector,
    message: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) = OmniEmptyState(
    status = message,
    value = subtitle,
    modifier = modifier,
    icon = icon,
    actionLabel = actionLabel,
    onAction = onAction,
)

/**
 * 简洁单文案/双文案空态便捷重载。
 */
@Composable
fun OmniEmptyState(
    message: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) = OmniEmptyState(
    status = message,
    value = subtitle.orEmpty(),
    modifier = modifier,
    icon = icon,
    actionLabel = actionLabel,
    onAction = onAction,
)
