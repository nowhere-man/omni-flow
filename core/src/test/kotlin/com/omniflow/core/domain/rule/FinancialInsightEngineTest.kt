package com.omniflow.core.domain.rule

import com.omniflow.core.domain.model.CategoryBreakdownItem
import com.omniflow.core.domain.model.CategoryDiffItem
import com.omniflow.core.domain.model.FinancialHealthSummary
import com.omniflow.core.domain.model.InsightType
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.TransactionRankingItem
import com.omniflow.core.domain.model.TransactionSummary
import com.omniflow.core.domain.model.TransactionType
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FinancialInsightEngineTest {

    private fun rankingItem(amount: Money, categoryName: String): TransactionRankingItem =
        TransactionRankingItem(
            transactionId = "tx-1",
            ledgerId = "ledger-1",
            ledgerName = "默认账本",
            accountId = "acc-1",
            accountName = "银行卡",
            categoryId = "cat-1",
            categoryName = categoryName,
            occurredAt = Instant.fromEpochMilliseconds(1700000000000L),
            primaryCategoryName = categoryName,
            secondaryCategoryName = null,
            iconKey = null,
            note = null,
            type = TransactionType.EXPENSE,
            amount = amount,
            source = null,
        )

    private fun categoryBreakdown(name: String, amount: Money): CategoryBreakdownItem =
        CategoryBreakdownItem(
            primaryCategoryId = "cat-$name",
            primaryCategoryName = name,
            iconKey = null,
            amount = amount,
            secondaryCategories = emptyList(),
        )

    @Test
    fun rule1_anomalyIncrease_triggersWhenExpenseSpikesAndSingleCategoryDominatesIncrease() {
        // Previous expense: 10,000 (¥100.00). Current: 15,000 (¥150.00), +50% increase (>15%)
        // Diff = +5,000. Digital category increased by 4,000 (80% of total increase >= 40%)
        val previous = TransactionSummary(expenseTotal = Money(10_000), incomeTotal = Money(20_000))
        val current = TransactionSummary(expenseTotal = Money(15_000), incomeTotal = Money(20_000))
        val health = FinancialHealthSummary(income = Money(20_000), expense = Money(15_000), daysCount = 30)
        val diffs = listOf(
            CategoryDiffItem("1", "数码", null, Money(6_000), Money(2_000)), // diff = +4,000
            CategoryDiffItem("2", "餐饮", null, Money(9_000), Money(8_000)), // diff = +1,000
        )

        val insight = FinancialInsightEngine.evaluate(
            currentSummary = current,
            previousSummary = previous,
            healthSummary = health,
            categoryDiffs = diffs,
            ranking = emptyList(),
            categoryBreakdowns = emptyList(),
        )

        assertEquals(InsightType.ANOMALY_INCREASE, insight.type)
        assertEquals("超支异动预警", insight.title)
        assertTrue(insight.message.contains("数码"))
        assertTrue(insight.message.contains("¥50.00"))
        assertTrue(insight.message.contains("¥40.00"))
    }

    @Test
    fun rule2_savingsAchievement_triggersWhenExpenseDropsAndSavingsRateHigh() {
        // Previous: 20,000 (¥200.00). Current: 16,000 (¥160.00), -20% drop (>= 10%)
        // Income: 30,000 (¥300.00). Net savings: 14,000 (¥140.00), savings rate = 46.7% (>= 25%)
        val previous = TransactionSummary(expenseTotal = Money(20_000), incomeTotal = Money(30_000))
        val current = TransactionSummary(expenseTotal = Money(16_000), incomeTotal = Money(30_000))
        val health = FinancialHealthSummary(income = Money(30_000), expense = Money(16_000), daysCount = 30)
        val diffs = listOf(
            CategoryDiffItem("1", "外卖餐饮", null, Money(6_000), Money(10_000)), // diff = -4,000
        )

        val insight = FinancialInsightEngine.evaluate(
            currentSummary = current,
            previousSummary = previous,
            healthSummary = health,
            categoryDiffs = diffs,
            ranking = emptyList(),
            categoryBreakdowns = emptyList(),
        )

        assertEquals(InsightType.SAVINGS_ACHIEVEMENT, insight.type)
        assertEquals("省钱成就达成", insight.title)
        assertTrue(insight.message.contains("控制得力"))
        assertTrue(insight.message.contains("外卖餐饮"))
        assertTrue(insight.message.contains("¥40.00"))
    }

    @Test
    fun rule3_extremeLargeExpenseAlert_triggersWhenTop1Exceeds35Percent() {
        // Current expense: 10,000. Previous: 10,000 (no big increase/decrease).
        // Top 1 single transaction = 4,000 (40% >= 35%).
        val previous = TransactionSummary(expenseTotal = Money(10_000), incomeTotal = Money(15_000))
        val current = TransactionSummary(expenseTotal = Money(10_000), incomeTotal = Money(15_000))
        val health = FinancialHealthSummary(income = Money(15_000), expense = Money(10_000), daysCount = 30)
        val ranking = listOf(
            rankingItem(Money(4_000), "家居装修"),
        )

        val insight = FinancialInsightEngine.evaluate(
            currentSummary = current,
            previousSummary = previous,
            healthSummary = health,
            categoryDiffs = emptyList(),
            ranking = ranking,
            categoryBreakdowns = listOf(categoryBreakdown("家居装修", Money(4_000))),
        )

        assertEquals(InsightType.LARGE_EXPENSE_ALERT, insight.type)
        assertEquals("极端大额预警", insight.title)
        assertTrue(insight.message.contains("家居装修"))
        assertTrue(insight.message.contains("¥40.00"))
        assertTrue(insight.message.contains("40.0%"))
    }

    @Test
    fun rule4_concentratedCategory_triggersWhenSingleCategoryExceeds50Percent() {
        // Current expense: 10,000. Previous: 10,000.
        // Ranking top 1 = 2,000 (< 35%, does not trigger rule 3).
        // But category "住房房租" = 6,000 (60% > 50%).
        val previous = TransactionSummary(expenseTotal = Money(10_000), incomeTotal = Money(15_000))
        val current = TransactionSummary(expenseTotal = Money(10_000), incomeTotal = Money(15_000))
        val health = FinancialHealthSummary(income = Money(15_000), expense = Money(10_000), daysCount = 30)
        val ranking = listOf(
            rankingItem(Money(2_000), "住房房租"),
            rankingItem(Money(2_000), "住房房租"),
            rankingItem(Money(2_000), "住房房租"),
        )
        val breakdowns = listOf(
            categoryBreakdown("住房房租", Money(6_000)),
            categoryBreakdown("餐饮美食", Money(4_000)),
        )

        val insight = FinancialInsightEngine.evaluate(
            currentSummary = current,
            previousSummary = previous,
            healthSummary = health,
            categoryDiffs = emptyList(),
            ranking = ranking,
            categoryBreakdowns = breakdowns,
        )

        assertEquals(InsightType.CONCENTRATED_CATEGORY, insight.type)
        assertEquals("单一分类主导", insight.title)
        assertTrue(insight.message.contains("住房房租"))
        assertTrue(insight.message.contains("60.0%"))
    }

    @Test
    fun rule5_deficitWarning_triggersWhenExpenseExceedsIncome() {
        // Income = 10,000 (¥100.00), Expense = 12,000 (¥120.00). Deficit = 2,000.
        val previous = TransactionSummary(expenseTotal = Money(12_000), incomeTotal = Money(10_000))
        val current = TransactionSummary(expenseTotal = Money(12_000), incomeTotal = Money(10_000))
        val health = FinancialHealthSummary(income = Money(10_000), expense = Money(12_000), daysCount = 30)

        val insight = FinancialInsightEngine.evaluate(
            currentSummary = current,
            previousSummary = previous,
            healthSummary = health,
            categoryDiffs = emptyList(),
            ranking = emptyList(),
            categoryBreakdowns = emptyList(),
        )

        assertEquals(InsightType.DEFICIT_WARNING, insight.type)
        assertEquals("赤字预警", insight.title)
        assertTrue(insight.message.contains("¥20.00"))
        assertTrue(insight.message.contains("120.0%"))
    }

    @Test
    fun rule6_balancedDefault_triggersWhenMetricsAreSteady() {
        // Income = 20,000, Expense = 15,000, previous = 15,000 (0% change).
        // Savings rate = 25% (but decrease is 0%, so rule 2 not triggered).
        val previous = TransactionSummary(expenseTotal = Money(15_000), incomeTotal = Money(20_000))
        val current = TransactionSummary(expenseTotal = Money(15_000), incomeTotal = Money(20_000))
        val health = FinancialHealthSummary(income = Money(20_000), expense = Money(15_000), daysCount = 30)

        val insight = FinancialInsightEngine.evaluate(
            currentSummary = current,
            previousSummary = previous,
            healthSummary = health,
            categoryDiffs = emptyList(),
            ranking = emptyList(),
            categoryBreakdowns = emptyList(),
        )

        assertEquals(InsightType.BALANCED_DEFAULT, insight.type)
        assertEquals("收支节奏平稳", insight.title)
        assertTrue(insight.message.contains("25.0%"))
        assertTrue(insight.message.contains("¥5.00")) // 15000 / 30 = 500 minor = ¥5.00
    }
}
