package com.omniflow.core.domain

import com.omniflow.core.domain.model.Budget
import com.omniflow.core.domain.model.Category
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.TransactionListItem
import com.omniflow.core.domain.model.TransactionType
import com.omniflow.core.domain.model.buildBudgetProgress
import com.omniflow.core.domain.model.calculateDailyAllowance
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BudgetProgressTest {
    private val categories = listOf(
        category("food", null, "餐饮"),
        category("food-out", "food", "外卖"),
        category("transit", null, "交通"),
    )

    @Test
    fun overallBudgetSumsEveryCountableExpense() {
        val progress = buildBudgetProgress(
            budgets = listOf(budget("b1", null, 100_000)),
            categories = categories,
            monthTransactions = listOf(
                transaction("food", 30_000, TransactionType.EXPENSE),
                transaction("transit", 20_000, TransactionType.EXPENSE),
                transaction("food", 50_000, TransactionType.INCOME),
            ),
        ).single()

        assertEquals("总预算", progress.name)
        assertEquals(Money(50_000), progress.spent)
        assertEquals(Money(50_000), progress.remaining)
        assertFalse(progress.isOverspent)
        assertEquals(0.5f, progress.ratio)
    }

    @Test
    fun secondaryCategorySpendCountsTowardItsPrimaryBudget() {
        val progress = buildBudgetProgress(
            budgets = listOf(budget("b1", "food", 40_000)),
            categories = categories,
            monthTransactions = listOf(
                transaction("food-out", 25_000, TransactionType.EXPENSE),
                transaction("food", 10_000, TransactionType.EXPENSE),
                transaction("transit", 90_000, TransactionType.EXPENSE),
            ),
        ).single()

        assertEquals("餐饮", progress.name)
        assertEquals(Money(35_000), progress.spent)
    }

    @Test
    fun excludedTransactionsAreIgnoredAndOverspendIsFlagged() {
        val progress = buildBudgetProgress(
            budgets = listOf(budget("b1", null, 10_000)),
            categories = categories,
            monthTransactions = listOf(
                transaction("food", 12_000, TransactionType.EXPENSE),
                transaction("food", 90_000, TransactionType.EXPENSE, excluded = true),
            ),
        ).single()

        assertEquals(Money(12_000), progress.spent)
        assertTrue(progress.isOverspent)
        assertEquals(Money(-2_000), progress.remaining)
    }

    @Test
    fun skipsOrphanedBudgetsWhoseCategoryWasDeleted() {
        val progress = buildBudgetProgress(
            budgets = listOf(
                budget("b1", null, 100_000),
                budget("b2", "deleted-cat", 20_000),
            ),
            categories = categories,
            monthTransactions = emptyList(),
        )
        assertEquals(1, progress.size)
        assertEquals("总预算", progress.single().name)
    }

    @Test
    fun calculateDailyAllowanceCalculatesCorrectlyForMidMonth() {
        // 11 月有 30 天，11 月 14 日还剩 30 - 14 + 1 = 17 天
        val allowance = calculateDailyAllowance(
            remaining = Money(170_000), // 1700 元
            today = LocalDate(2026, 11, 14),
        )
        assertEquals(17, allowance.remainingDays)
        assertEquals(Money(10_000), allowance.dailyAllowance) // 100 元/天
    }

    @Test
    fun calculateDailyAllowanceOnLastDayOfMonth() {
        // 11 月 30 日最后一天，剩余 1 天
        val allowance = calculateDailyAllowance(
            remaining = Money(5_000),
            today = LocalDate(2026, 11, 30),
        )
        assertEquals(1, allowance.remainingDays)
        assertEquals(Money(5_000), allowance.dailyAllowance)
    }

    @Test
    fun calculateDailyAllowanceWhenOverspentOrZeroReturnsZero() {
        val overspent = calculateDailyAllowance(
            remaining = Money(-20_000),
            today = LocalDate(2026, 11, 14),
        )
        assertEquals(17, overspent.remainingDays)
        assertEquals(Money.Zero, overspent.dailyAllowance)

        val zero = calculateDailyAllowance(
            remaining = Money.Zero,
            today = LocalDate(2026, 11, 14),
        )
        assertEquals(17, zero.remainingDays)
        assertEquals(Money.Zero, zero.dailyAllowance)
    }

    private fun budget(id: String, categoryId: String?, minor: Long) =
        Budget(id = id, ledgerId = "ledger", categoryId = categoryId, amount = Money(minor))

    private fun category(id: String, parentId: String?, name: String) = Category(
        id = id,
        ledgerId = "ledger",
        parentId = parentId,
        name = name,
        iconKey = null,
        type = TransactionType.EXPENSE,
    )

    private fun transaction(
        categoryId: String,
        minor: Long,
        type: TransactionType,
        excluded: Boolean = false,
    ) = TransactionListItem(
        id = "t-$categoryId-$minor",
        ledgerId = "ledger",
        ledgerName = "账本",
        accountId = "account",
        accountName = "现金",
        categoryId = categoryId,
        categoryName = categoryId,
        primaryCategoryName = categoryId,
        categoryIconKey = null,
        amount = Money(minor),
        type = type,
        occurredAt = Instant.fromEpochMilliseconds(1_000),
        note = null,
        isExcluded = excluded,
        source = null,
    )
}
