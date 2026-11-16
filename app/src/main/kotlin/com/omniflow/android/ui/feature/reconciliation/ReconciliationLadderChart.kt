package com.omniflow.android.ui.feature.reconciliation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.asRmb
import com.omniflow.android.ui.expenseColor
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.NetWorthPoint

/**
 * 资产天梯（净资产攀升平滑曲线图）
 * 专为移动端小屏幕打造：支持贝塞尔平滑曲线与优雅渐变色填充。
 */
@Composable
internal fun NetWorthLadderChart(
    points: List<NetWorthPoint>,
    modifier: Modifier = Modifier,
) {
    if (points.isEmpty()) return

    val validPoints = points.filter { it.netAssets.minor > 0 }
    if (validPoints.size < 2) return

    val minVal = validPoints.minOf { it.netAssets.minor }
    val maxVal = validPoints.maxOf { it.netAssets.minor }
    val range = (maxVal - minVal).coerceAtLeast(1L).toFloat()

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val peakDotColor = expenseColor()

    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Text(
                        text = "财富攀升天梯",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                ) {
                    Text(
                        text = "峰值 ${Money(maxVal).asRmb()}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            // Canvas Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                    val w = size.width
                    val h = size.height
                    val paddingX = 24.dp.toPx()
                    val paddingBottom = 28.dp.toPx()
                    val paddingTop = 16.dp.toPx()
                    val chartHeight = h - paddingTop - paddingBottom
                    val stepX = (w - paddingX * 2) / (validPoints.size - 1).coerceAtLeast(1)

                    val coords = validPoints.mapIndexed { index, p ->
                        val x = paddingX + index * stepX
                        val normalized = (p.netAssets.minor - minVal).toFloat() / range
                        val y = h - paddingBottom - (normalized * chartHeight)
                        Offset(x, y)
                    }

                    // 1. Draw gradient fill under line
                    val fillPath = Path().apply {
                        moveTo(coords.first().x, h - paddingBottom)
                        coords.forEach { lineTo(it.x, it.y) }
                        lineTo(coords.last().x, h - paddingBottom)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.28f),
                                primaryColor.copy(alpha = 0.02f),
                            ),
                            startY = paddingTop,
                            endY = h - paddingBottom,
                        ),
                    )

                    // 2. Draw smooth stroke curve
                    val strokePath = Path().apply {
                        coords.forEachIndexed { i, pt ->
                            if (i == 0) moveTo(pt.x, pt.y)
                            else {
                                val prev = coords[i - 1]
                                val cx = (prev.x + pt.x) / 2
                                cubicTo(cx, prev.y, cx, pt.y, pt.x, pt.y)
                            }
                        }
                    }

                    drawPath(
                        path = strokePath,
                        color = primaryColor,
                        style = Stroke(width = 3.dp.toPx()),
                    )

                    // 3. Draw key milestone dots
                    coords.forEachIndexed { index, pt ->
                        val isPeak = validPoints[index].netAssets.minor == maxVal
                        val isStart = index == 0
                        val isEnd = index == coords.size - 1

                        if (isPeak || isStart || isEnd) {
                            drawCircle(
                                color = surfaceColor,
                                radius = 6.dp.toPx(),
                                center = pt,
                            )
                            // C2: expenseColor() instead of hardcoded error color
                            drawCircle(
                                color = if (isPeak) peakDotColor else primaryColor,
                                radius = 4.dp.toPx(),
                                center = pt,
                            )
                        }
                    }
                }
            }

            // Month labels row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = validPoints.first().label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = validPoints[validPoints.size / 2].label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = validPoints.last().label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
