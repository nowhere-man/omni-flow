package com.omniflow.core.domain.model

import kotlinx.datetime.Instant

enum class TimeGranularity { DAY, WEEK, MONTH }

data class AnalyticsQuery(
    val scope: LedgerScope,
    val range: DateRange,
    val rankingType: TransactionType = TransactionType.EXPENSE,
    val categoryShareType: TransactionType = TransactionType.EXPENSE,
    val tagAnalysisType: TransactionType = TransactionType.EXPENSE,
    val trendGranularity: TimeGranularity = TimeGranularity.DAY,
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

data class TransactionRankingItem(
    val transactionId: TransactionId,
    val ledgerId: LedgerId,
    val ledgerName: String,
    val accountId: AccountId,
    val accountName: String,
    val categoryId: CategoryId,
    val categoryName: String,
    val occurredAt: Instant,
    val primaryCategoryName: String,
    val secondaryCategoryName: String?,
    val iconKey: String?,
    val note: String?,
    val type: TransactionType,
    val amount: Money,
    val source: TransactionSource?,
) {
    val categoryDisplayName: String
        get() = secondaryCategoryName?.let { "$primaryCategoryName - $it" } ?: primaryCategoryName
}

data class TagAnalysisItem(
    val tagId: String,
    val tagName: String,
    val amount: Money,
    val transactionCount: Int,
)

data class CategoryShareItem(
    val categoryId: CategoryId,
    val categoryName: String,
    val iconKey: String?,
    val amount: Money,
    val shareRatio: Double = 0.0,
)

data class CategoryBreakdownItem(
    val primaryCategoryId: CategoryId,
    val primaryCategoryName: String,
    val iconKey: String?,
    val amount: Money,
    val secondaryCategories: List<CategoryShareItem>,
    val shareRatio: Double = 0.0,
)

data class CategoryDiffItem(
    val categoryId: CategoryId,
    val categoryName: String,
    val iconKey: String?,
    val currentAmount: Money,
    val previousAmount: Money,
) {
    val diffAmount: Money get() = currentAmount - previousAmount
    val isIncrease: Boolean get() = currentAmount > previousAmount
    val isDecrease: Boolean get() = currentAmount < previousAmount
    val absoluteDiffAmount: Money get() = if (diffAmount.minor >= 0L) diffAmount else Money(-diffAmount.minor)
}

data class FinancialHealthSummary(
    val income: Money,
    val expense: Money,
    val daysCount: Int,
) {
    val netSavings: Money get() = income - expense
    val savingsRatePercentage: Double?
        get() = if (income.minor > 0L) {
            (netSavings.minor.toDouble() / income.minor.toDouble()) * 100.0
        } else {
            null
        }
    val dailyAverageExpense: Money
        get() = if (daysCount > 0) Money(expense.minor / daysCount) else Money.Zero
}

enum class InsightType {
    ANOMALY_INCREASE,
    SAVINGS_ACHIEVEMENT,
    LARGE_EXPENSE_ALERT,
    CONCENTRATED_CATEGORY,
    DEFICIT_WARNING,
    BALANCED_DEFAULT,
}

data class FinancialInsight(
    val type: InsightType,
    val title: String,
    val message: String,
    val highlightCategory: String? = null,
    val highlightAmount: Money? = null,
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
    val previousSummary: TransactionSummary,
    val trend: ChartData,
    val ranking: List<TransactionRankingItem>,
    val categoryBreakdowns: List<CategoryBreakdownItem>,
    val tagAnalysis: List<TagAnalysisItem>,
    val yearStatement: StatementTable,
    val healthSummary: FinancialHealthSummary = FinancialHealthSummary(
        income = summary.incomeTotal,
        expense = summary.expenseTotal,
        daysCount = query.range.daysCount(),
    ),
    val categoryDiffs: List<CategoryDiffItem> = emptyList(),
    val insight: FinancialInsight = FinancialInsight(
        type = InsightType.BALANCED_DEFAULT,
        title = "收支节奏平稳",
        message = "收支节奏平稳。",
    ),
)
