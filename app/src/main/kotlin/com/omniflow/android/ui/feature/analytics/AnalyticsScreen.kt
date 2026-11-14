package com.omniflow.android.ui.feature.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.readableContentWidth
import com.omniflow.core.domain.model.ChartPoint
import com.omniflow.core.domain.model.DateRange
import com.omniflow.core.domain.model.LedgerScope
import com.omniflow.core.domain.model.TransactionType

@Composable
fun AnalyticsScreen(
    state: AnalyticsUiState,
    onScopeChange: (LedgerScope) -> Unit,
    onRangeModeChange: (AnalyticsRangeMode) -> Unit,
    onShiftRange: (Long) -> Unit,
    onResetRange: () -> Unit,
    onCustomRangeSelected: (DateRange) -> Unit,
    onCategoryShareTypeChange: (TransactionType) -> Unit,
    onRankingTypeChange: (TransactionType) -> Unit,
    onCategoryClick: (String) -> Unit,
    onTransactionClick: (String) -> Unit,
    onSummaryClick: (TransactionType?) -> Unit,
    onViewStatementTable: (Int) -> Unit,
    onDismissStatementTable: () -> Unit,
    onPointClick: ((ChartPoint) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val dashboard = state.dashboard

    if (state.isLoading && dashboard == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (state.error != null && dashboard == null) {
        Box(
            modifier = modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "加载统计失败: ${state.error}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .readableContentWidth()
            .fillMaxHeight()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 1. A1: Control Header (Ledger Scope Pill + Compact Date Range Controller)
        item {
            AnalyticsControlHeader(
                scope = state.scope,
                ledgers = state.ledgers,
                rangeMode = state.rangeMode,
                range = state.range,
                onScopeChange = onScopeChange,
                onRangeModeChange = onRangeModeChange,
                onShiftRange = onShiftRange,
                onResetRange = onResetRange,
                onCustomRangeSelected = onCustomRangeSelected,
            )
        }

        if (dashboard != null) {
            // 2. A2: Hero Insight Card
            item {
                FinancialInsightCard(
                    insight = dashboard.insight,
                )
            }

            // 3. A3: Health Summary Card
            item {
                HealthSummaryCard(
                    healthSummary = dashboard.healthSummary,
                    onSummaryClick = onSummaryClick,
                )
            }

            // 4. A4: Trend Bar Chart Card (Adaptive Side-by-side with Tooltip)
            item {
                TrendBarChartCard(
                    trendData = dashboard.trend,
                    rangeMode = state.rangeMode,
                    onViewStatementTable = {
                        onViewStatementTable(dashboard.yearStatement.year)
                    },
                    onPointClick = onPointClick,
                )
            }

            // 5. A5: Category Expense Diff Card (Horizontal Diverging Bars)
            item {
                CategoryDiffCard(
                    categoryDiffs = dashboard.categoryDiffs,
                    currentSummary = dashboard.summary,
                    previousSummary = dashboard.previousSummary,
                    onCategoryClick = onCategoryClick,
                )
            }

            // 6. A6: Category Share Card (Donut + ListItem Breakdown)
            item {
                CategoryShareCard(
                    categoryBreakdowns = dashboard.categoryBreakdowns,
                    selectedType = state.categoryShareType,
                    onTypeChange = onCategoryShareTypeChange,
                    onCategoryClick = onCategoryClick,
                )
            }

            // 7. A7: Top Expenses Ranking Card
            item {
                TopExpensesCard(
                    rankingItems = dashboard.ranking,
                    selectedType = state.rankingType,
                    onTypeChange = onRankingTypeChange,
                    onTransactionClick = onTransactionClick,
                )
            }
        }
    }

    // Modal Statement Table Sheet
    state.statementTable?.let { table ->
        StatementTableSheet(
            table = table,
            onYearChange = onViewStatementTable,
            onDismiss = onDismissStatementTable,
        )
    }
}
