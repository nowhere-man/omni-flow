package com.omniflow.core.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

typealias BudgetId = String

/**
 * 每日预算建议额度。
 * [remainingDays] 当月包含今日在内的剩余天数（最少为 1）。
 * [dailyAllowance] 每日平均建议可花金额；当已超支或无结余时为零。
 */
data class DailyBudgetAllowance(
    val remainingDays: Int,
    val dailyAllowance: Money,
)

/**
 * 根据当月剩余预算与指定日期计算每日建议可花额度。
 */
fun calculateDailyAllowance(
    remaining: Money,
    today: LocalDate,
    timeZone: TimeZone = AppTimeZone,
): DailyBudgetAllowance {
    val jDate = java.time.LocalDate.of(today.year, today.monthNumber, today.dayOfMonth)
    val totalDaysInMonth = jDate.lengthOfMonth()
    val remainingDays = (totalDaysInMonth - today.dayOfMonth + 1).coerceAtLeast(1)
    val allowance = if (remaining.minor <= 0L) {
        Money.Zero
    } else {
        Money(remaining.minor / remainingDays)
    }
    return DailyBudgetAllowance(
        remainingDays = remainingDays,
        dailyAllowance = allowance,
    )
}

/**
 * 月度预算。[categoryId] 为 null 表示整个账本的总预算，否则是某个一级分类的预算。
 * 只做月度：周预算和年预算在记账场景里几乎没人用，先不引入周期字段增加复杂度。
 */
data class Budget(
    val id: BudgetId,
    val ledgerId: LedgerId,
    val categoryId: CategoryId?,
    val amount: Money,
    val deletedAt: Instant? = null,
) {
    val isOverall: Boolean get() = categoryId == null
}

data class BudgetProgress(
    val budget: Budget,
    val name: String,
    val iconKey: String?,
    val spent: Money,
) {
    val remaining: Money get() = budget.amount - spent
    val isOverspent: Boolean get() = spent.minor > budget.amount.minor

    /** 用掉的比例，超支时会大于 1，画进度条时自己 coerce。 */
    val ratio: Float
        get() = if (budget.amount.minor <= 0L) 0f else spent.minor.toFloat() / budget.amount.minor
}

/**
 * 用当月交易算预算进度。放在 domain 里是因为「哪些交易算进预算」是业务规则：
 * 只算支出、跳过不计入统计的、二级分类要归到它的一级分类头上。
 */
fun buildBudgetProgress(
    budgets: List<Budget>,
    categories: List<Category>,
    monthTransactions: List<TransactionListItem>,
): List<BudgetProgress> {
    val countable = monthTransactions.filterNot(TransactionListItem::isExcluded)
        .filter { it.type == TransactionType.EXPENSE }
    val totalSpent = countable.fold(Money.Zero) { total, item -> total + item.amount }
    val primaryOf = categories.associate { it.id to (it.parentId ?: it.id) }
    val spentByPrimary = countable
        .groupBy { primaryOf[it.categoryId] ?: it.categoryId }
        .mapValues { (_, items) -> items.fold(Money.Zero) { total, item -> total + item.amount } }

    return budgets.mapNotNull { budget ->
        val category = budget.categoryId?.let { id ->
            categories.firstOrNull { it.id == id } ?: return@mapNotNull null
        }
        BudgetProgress(
            budget = budget,
            name = category?.name ?: "总预算",
            iconKey = category?.iconKey,
            spent = if (budget.categoryId == null) {
                totalSpent
            } else {
                spentByPrimary[budget.categoryId] ?: Money.Zero
            },
        )
    }
}
