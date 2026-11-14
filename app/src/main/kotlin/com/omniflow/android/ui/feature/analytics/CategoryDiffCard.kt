package com.omniflow.android.ui.feature.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.CategoryIcon
import com.omniflow.android.ui.asRmb
import com.omniflow.android.ui.designsystem.components.DivergingBarRowData
import com.omniflow.android.ui.designsystem.components.HorizontalDivergingBarChart
import com.omniflow.android.ui.designsystem.tokens.expenseColor
import com.omniflow.android.ui.designsystem.tokens.incomeColor
import com.omniflow.core.domain.model.CategoryDiffItem
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.TransactionSummary
import kotlin.math.abs

@Composable
fun CategoryDiffCard(
    categoryDiffs: List<CategoryDiffItem>,
    currentSummary: TransactionSummary,
    previousSummary: TransactionSummary,
    modifier: Modifier = Modifier,
    onCategoryClick: ((String) -> Unit)? = null,
) {
    val totalExpenseDiff = currentSummary.expenseTotal - previousSummary.expenseTotal
    val isMoreExpense = totalExpenseDiff.minor > 0L
    val isZeroPrev = previousSummary.expenseTotal.minor == 0L

    val top4Diffs = remember(categoryDiffs) {
        categoryDiffs.take(4)
    }

    val maxAbsDiff = remember(top4Diffs) {
        top4Diffs.maxOfOrNull { abs(it.diffAmount.minor) } ?: 1L
    }

    val chartItems = remember(top4Diffs) {
        top4Diffs.map { item ->
            val prefix = if (item.isIncrease) "+" else if (item.isDecrease) "-" else ""
            DivergingBarRowData(
                id = item.categoryId,
                name = item.categoryName,
                iconKey = item.iconKey,
                diffAmount = item.diffAmount.minor,
                isIncrease = item.isIncrease,
                formattedDiff = "$prefix${item.absoluteDiffAmount.asRmb()}",
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
            // Header: Title + Subtitle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "分类支出增减",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = "对比上期同期",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (isZeroPrev && top4Diffs.isEmpty()) {
                // Graceful fallback when no previous period data
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "暂无上期对比数据，下期此时将自动生成环比增减分析",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                // Hero diff summary
                val heroText = when {
                    totalExpenseDiff.minor > 0L -> "较上期多花 ${totalExpenseDiff.asRmb()}"
                    totalExpenseDiff.minor < 0L -> "较上期少花 ${Money(-totalExpenseDiff.minor).asRmb()}"
                    else -> "总支出与上期持平"
                }

                Text(
                    text = heroText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isMoreExpense) expenseColor() else incomeColor(),
                )

                // Diverging Bars
                HorizontalDivergingBarChart(
                    items = chartItems,
                    maxAbsDiff = maxAbsDiff,
                    increaseColor = expenseColor(),
                    decreaseColor = incomeColor(),
                    iconSlot = { iconKey ->
                        CategoryIcon(iconKey = iconKey, size = 20)
                    },
                    onItemClick = { item ->
                        onCategoryClick?.invoke(item.id)
                    },
                )
            }
        }
    }
}
