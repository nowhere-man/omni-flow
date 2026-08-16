package com.omniflow.core.domain

import com.omniflow.core.domain.model.Budget
import com.omniflow.core.domain.model.Category
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.TransactionListItem
import com.omniflow.core.domain.model.TransactionType
import com.omniflow.core.domain.model.buildBudgetProgress
import kotlinx.datetime.Instant
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
