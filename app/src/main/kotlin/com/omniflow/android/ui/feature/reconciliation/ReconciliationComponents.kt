package com.omniflow.android.ui.feature.reconciliation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.OmniText
import com.omniflow.android.ui.asRmb
import com.omniflow.android.ui.expenseColor
import com.omniflow.android.ui.incomeColor
import com.omniflow.core.domain.model.AccountRole
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.NetWorthPoint
import com.omniflow.core.domain.model.PeriodReconciliationSnapshot

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
                            drawCircle(
                                color = if (isPeak) Color(0xFFE53935) else primaryColor,
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

/**
 * 月度核心指标看板
 */
@Composable
internal fun MonthlyScorecardHero(
    snapshot: PeriodReconciliationSnapshot,
    onEditIncome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Period badge & edit trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = "${snapshot.period} 对账看板",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.clickable(onClick = onEditIncome),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "录入收入",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = "录入本月收入",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            // Big Net Savings Hero
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "本月净存（结余）",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = snapshot.netSavings.asRmb(),
                        style = OmniText.amountHero,
                        color = if (snapshot.netSavings.minor >= 0) incomeColor() else expenseColor(),
                    )
                    if (snapshot.savingsRatePercent > 0) {
                        Surface(
                            shape = CircleShape,
                            color = incomeColor().copy(alpha = 0.15f),
                        ) {
                            Text(
                                text = "储蓄率 ${String.format("%.1f%%", snapshot.savingsRatePercent)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = incomeColor(),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
            }

            // Three core columns: Income, Derived Expense, Net Assets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "本月总收入",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = snapshot.meta.totalIncome.asRmb(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = incomeColor(),
                    )
                }

                Column {
                    Text(
                        text = "倒推当月实际支出",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = snapshot.derivedExpense.asRmb(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = expenseColor(),
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "月末净资产",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = snapshot.netAssets.asRmb(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

/**
 * 角色收支与资产对比卡片（丈夫 vs 妻子）
 */
@Composable
internal fun RoleBreakdownCard(
    snapshot: PeriodReconciliationSnapshot,
    modifier: Modifier = Modifier,
) {
    val h = snapshot.husbandSummary
    val w = snapshot.wifeSummary

    OutlinedCard(
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "家庭角色对账明细",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            // Two columns for husband & wife
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Husband Card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = AccountRole.HUSBAND.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "收入: ${h.income.asRmb()}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "存款: ${h.totalAssets.asRmb()}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (h.investmentValue.minor > 0) {
                        Text(
                            text = "股票: ${h.investmentValue.asRmb()}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text(
                        text = "欠款: ${h.totalLiabilities.asRmb()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (h.totalLiabilities.minor > 0) expenseColor() else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "净值: ${h.netAssets.asRmb()}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                // Wife Card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = AccountRole.WIFE.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    Text(
                        text = "收入: ${w.income.asRmb()}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "存款: ${w.totalAssets.asRmb()}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (w.investmentValue.minor > 0) {
                        Text(
                            text = "股票: ${w.investmentValue.asRmb()}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text(
                        text = "欠款: ${w.totalLiabilities.asRmb()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (w.totalLiabilities.minor > 0) expenseColor() else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "净值: ${w.netAssets.asRmb()}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }
    }
}

/**
 * 移动端年度财富台账卡片（替代传统宽表格）
 */
@Composable
internal fun ExpandableMonthLedgerCard(
    snapshot: PeriodReconciliationSnapshot,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    OutlinedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
                    Text(
                        text = snapshot.period,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    if (snapshot.meta.notes.isNotBlank()) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Text(
                                text = "有备忘",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "净资产: ${snapshot.netAssets.asRmb()}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // Summary row: Income, Expense, Net Savings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "收入: ${snapshot.meta.totalIncome.asRmb()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = incomeColor(),
                )
                Text(
                    text = "支出: ${snapshot.derivedExpense.asRmb()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = expenseColor(),
                )
                Text(
                    text = "净存: ${snapshot.netSavings.asRmb()}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (snapshot.meta.notes.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "备忘: ${snapshot.meta.notes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(8.dp),
                            )
                        }
                    }

                    // Role details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "${AccountRole.HUSBAND.label}存款: ${snapshot.husbandSummary.totalAssets.asRmb()} (欠款 ${snapshot.husbandSummary.totalLiabilities.asRmb()})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "${AccountRole.WIFE.label}存款: ${snapshot.wifeSummary.totalAssets.asRmb()} (欠款 ${snapshot.wifeSummary.totalLiabilities.asRmb()})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Text(
                        text = "查看该月深度看板 →",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .align(Alignment.End)
                            .clickable(onClick = onSelect)
                            .padding(top = 4.dp),
                    )
                }
            }
        }
    }
}
