package com.omniflow.android.ui.feature.reconciliation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.OmniText
import com.omniflow.android.ui.asRmb
import com.omniflow.android.ui.expenseColor
import com.omniflow.android.ui.incomeColor
import com.omniflow.core.domain.model.AccountRole
import com.omniflow.core.domain.model.PeriodReconciliationSnapshot
import com.omniflow.core.domain.model.RoleReconciliationSummary

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
                // C1: 胶囊误用修正：CircleShape -> RoundedCornerShape(8.dp)
                Surface(
                    shape = RoundedCornerShape(8.dp),
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
                        // C1: 胶囊误用修正：CircleShape -> RoundedCornerShape(8.dp)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
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
 * 角色收支与资产对比卡片（丈夫 vs 妻子，以及家庭公共）
 */
@Composable
internal fun RoleBreakdownCard(
    snapshot: PeriodReconciliationSnapshot,
    modifier: Modifier = Modifier,
) {
    val h = snapshot.husbandSummary
    val w = snapshot.wifeSummary
    val f = snapshot.familySummary
    val hasFamily = f.totalAssets.minor > 0 || f.totalLiabilities.minor > 0

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

            // Columns for husband & wife
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RoleSummaryColumn(
                    summary = h,
                    title = AccountRole.HUSBAND.label,
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    titleColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )

                RoleSummaryColumn(
                    summary = w,
                    title = AccountRole.WIFE.label,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                    titleColor = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f),
                )
            }

            // A4: 显式处理 FAMILY 家庭公共角色
            if (hasFamily) {
                RoleSummaryColumn(
                    summary = f,
                    title = AccountRole.FAMILY.label,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                    titleColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun RoleSummaryColumn(
    summary: RoleReconciliationSummary,
    title: String,
    containerColor: androidx.compose.ui.graphics.Color,
    titleColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = titleColor,
        )
        if (summary.income.minor > 0) {
            Text(
                text = "收入: ${summary.income.asRmb()}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
            )
        }
        Text(
            text = "存款: ${summary.totalAssets.asRmb()}",
            style = MaterialTheme.typography.bodySmall,
        )
        if (summary.investmentValue.minor > 0) {
            Text(
                text = "股票: ${summary.investmentValue.asRmb()}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            text = "欠款: ${summary.totalLiabilities.asRmb()}",
            style = MaterialTheme.typography.bodySmall,
            color = if (summary.totalLiabilities.minor > 0) expenseColor() else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "净值: ${summary.netAssets.asRmb()}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = titleColor,
        )
    }
}
