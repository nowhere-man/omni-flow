package com.omniflow.android.ui.feature.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.asRmb
import com.omniflow.android.ui.designsystem.components.SideBySideBarChart
import com.omniflow.android.ui.designsystem.components.SideBySideBarPoint
import com.omniflow.android.ui.designsystem.tokens.expenseColor
import com.omniflow.android.ui.designsystem.tokens.incomeColor
import com.omniflow.core.domain.model.AppTimeZone
import com.omniflow.core.domain.model.ChartData
import com.omniflow.core.domain.model.ChartPoint
import com.omniflow.core.domain.model.TimeGranularity
import kotlinx.datetime.toLocalDateTime

@Composable
fun TrendBarChartCard(
    trendData: ChartData,
    rangeMode: AnalyticsRangeMode,
    modifier: Modifier = Modifier,
    onViewStatementTable: (() -> Unit)? = null,
    onPointClick: ((ChartPoint) -> Unit)? = null,
) {
    var selectedPointId by remember { mutableStateOf<String?>(null) }

    val barPoints = remember(trendData.points) {
        trendData.points.mapIndexed { index, point ->
            val balance = point.income - point.expense
            SideBySideBarPoint(
                id = index.toString(),
                label = when (trendData.granularity) {
                    TimeGranularity.DAY -> {
                        val dt = point.start.toLocalDateTime(AppTimeZone)
                        "${dt.dayOfMonth}日"
                    }
                    TimeGranularity.WEEK -> point.label
                    TimeGranularity.MONTH -> {
                        val dt = point.start.toLocalDateTime(AppTimeZone)
                        "${dt.monthNumber}月"
                    }
                },
                primaryValue = point.income.minor,
                secondaryValue = point.expense.minor,
                primaryFormatted = point.income.asRmb(),
                secondaryFormatted = point.expense.asRmb(),
                balanceFormatted = balance.asRmb(),
                contentDescription = "${point.label}，收入${point.income.asRmb()}，支出${point.expense.asRmb()}",
            )
        }
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header Row: Title + Action (Annual Statement Sheet button if YEAR)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "收支趋势",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = when (trendData.granularity) {
                            TimeGranularity.DAY -> "每日收支并排对比"
                            TimeGranularity.WEEK -> "每周收支并排对比"
                            TimeGranularity.MONTH -> "每月收支并排对比"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (rangeMode == AnalyticsRangeMode.YEAR && onViewStatementTable != null) {
                    TextButton(onClick = onViewStatementTable) {
                        Text(
                            text = "年度对账表",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }

            // Side-by-side Chart
            SideBySideBarChart(
                points = barPoints,
                primaryColor = incomeColor(),
                secondaryColor = expenseColor(),
                selectedPointId = selectedPointId,
                onPointSelect = { point ->
                    selectedPointId = point?.id
                },
                onTooltipClick = { barPoint ->
                    val index = barPoint.id.toIntOrNull()
                    if (index != null && index in trendData.points.indices) {
                        onPointClick?.invoke(trendData.points[index])
                    }
                },
            )
        }
    }
}
