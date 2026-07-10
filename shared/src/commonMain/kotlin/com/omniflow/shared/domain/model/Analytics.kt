package com.omniflow.shared.domain.model

import kotlinx.datetime.Instant

enum class TimeGranularity { DAY, WEEK, MONTH }

enum class CategoryShareGranularity { PRIMARY, SECONDARY }

data class AnalyticsQuery(
    val scope: LedgerScope,
    val range: DateRange,
    val rankingType: TransactionType = TransactionType.EXPENSE,
    val categoryShareType: TransactionType = TransactionType.EXPENSE,
    val categoryShareGranularity: CategoryShareGranularity = CategoryShareGranularity.PRIMARY,
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

data class PeriodCompareResult(
    val current: TransactionSummary,
    val previous: TransactionSummary,
) {
    val expenseChange: Money get() = current.expenseTotal - previous.expenseTotal
    val incomeChange: Money get() = current.incomeTotal - previous.incomeTotal
    val netIncomeChange: Money get() = current.netIncome - previous.netIncome
}

data class TransactionRankingItem(
    val transaction: TransactionListItem,
    val tags: List<TransactionTag>,
)

data class CategoryShareItem(
    val categoryId: CategoryId,
    val categoryName: String,
    val iconKey: String?,
    val amount: Money,
)

data class TagSummaryItem(
    val tag: TransactionTag,
    val expense: Money,
    val income: Money,
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
    val previousPeriod: PeriodCompareResult,
    val trend: ChartData,
    val ranking: List<TransactionRankingItem>,
    val categoryShares: List<CategoryShareItem>,
    val tagSummary: List<TagSummaryItem>,
    val accountSummary: AccountSummary,
)
