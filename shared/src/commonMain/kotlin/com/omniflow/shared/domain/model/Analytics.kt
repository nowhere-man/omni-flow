package com.omniflow.shared.domain.model

import kotlinx.datetime.Instant

enum class TimeGranularity { DAY, WEEK, MONTH }

data class AnalyticsQuery(
    val scope: LedgerScope,
    val range: DateRange,
    val rankingType: TransactionType = TransactionType.EXPENSE,
    val categoryShareType: TransactionType = TransactionType.EXPENSE,
)

data class ChartPoint(
    val start: Instant,
    val label: String,
    val expense: Money,
    val income: Money,
)

data class ChartData(
    val range: DateRange,
    val granularity: TimeGranularity,
    val points: List<ChartPoint>,
)

data class CategoryRankingItem(
    val categoryId: CategoryId,
    val primaryCategoryName: String,
    val secondaryCategoryName: String?,
    val iconKey: String?,
    val amount: Money,
) {
    val categoryDisplayName: String
        get() = secondaryCategoryName?.let { "$primaryCategoryName - $it" } ?: primaryCategoryName
}

data class CategoryShareItem(
    val categoryId: CategoryId,
    val categoryName: String,
    val iconKey: String?,
    val amount: Money,
)

data class CategoryBreakdownItem(
    val primaryCategoryId: CategoryId,
    val primaryCategoryName: String,
    val iconKey: String?,
    val amount: Money,
    val secondaryCategories: List<CategoryShareItem>,
)

data class StatementMonth(
    val month: Int,
    val expense: Money,
    val income: Money,
) {
    val netIncome: Money get() = income - expense
}

data class StatementTable(
    val year: Int,
    val months: List<StatementMonth>,
    val total: TransactionSummary,
)

data class AnalyticsDashboardState(
    val query: AnalyticsQuery,
    val summary: TransactionSummary,
    val ranking: List<CategoryRankingItem>,
    val categoryBreakdowns: List<CategoryBreakdownItem>,
    val yearStatement: StatementTable,
)
