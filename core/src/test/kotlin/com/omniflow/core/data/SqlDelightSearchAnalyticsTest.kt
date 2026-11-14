package com.omniflow.core.data

import com.omniflow.core.data.facade.SqlDelightAnalyticsFacade
import com.omniflow.core.data.local.createJvmDatabase
import com.omniflow.core.data.usecase.SqlDelightSearchTransactionsUseCase
import com.omniflow.core.domain.model.AnalyticsQuery
import com.omniflow.core.domain.model.AppTimeZone
import com.omniflow.core.domain.model.DateRange
import com.omniflow.core.domain.model.LedgerScope
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.TimeGranularity
import com.omniflow.core.domain.model.TransactionSearchQuery
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.math.abs

class SqlDelightSearchAnalyticsTest {
    @Test
    fun searchAndAnalyticsUseDocumentedFiltersAndExclusions() = runBlocking {
        val database = createJvmDatabase()
        database.ledgerQueries.insertLedger("ledger", "日常", null, 1, 1)
        database.accountQueries.insertAccount("account", "现金", "CASH", "banknote", null, null, 1_200, 1, 1, 1)
        database.categoryQueries.insertCategory("food", "ledger", null, "餐饮", "utensils", "EXPENSE", 1, 1)
        database.categoryQueries.insertCategory("restaurant", "ledger", "food", "餐厅", null, "EXPENSE", 1, 1)
        database.categoryQueries.insertCategory("salary", "ledger", null, "工资", "banknote", "INCOME", 1, 1)
        database.tagQueries.insertTag("work", "ledger", "工作", 1, 1)
        insert(database, "expense", "restaurant", 500, "EXPENSE", 1_000, "团队午餐", excluded = false)
        insert(database, "expense-2", "restaurant", 300, "EXPENSE", 1_500, "晚餐", excluded = false)
        insert(database, "excluded", "food", 200, "EXPENSE", 2_000, "报销", excluded = true)
        insert(database, "income", "salary", 1_000, "INCOME", 3_000, "七月工资", excluded = false)
        database.tagQueries.insertTransactionTag("expense", "work")

        val search = SqlDelightSearchTransactionsUseCase(database)(TransactionSearchQuery(keyword = "工作")).getOrThrow()
        assertEquals(listOf("expense"), search.items.map { it.transaction.id })
        assertEquals(Money(500), search.summary.expenseTotal)
        assertEquals("utensils", search.items.single().transaction.categoryIconKey)

        val textSearch = SqlDelightSearchTransactionsUseCase(database)(
            TransactionSearchQuery(
                primaryCategoryText = "餐饮",
                secondaryCategoryText = "餐厅",
                noteText = "晚餐",
            ),
        ).getOrThrow()
        assertEquals(listOf("expense-2"), textSearch.items.map { it.transaction.id })

        val scopeSearchQuery = TransactionSearchQuery(scope = LedgerScope.Single("ledger"))
        kotlin.test.assertTrue(scopeSearchQuery.hasFilters)
        val scopeSearch = SqlDelightSearchTransactionsUseCase(database)(scopeSearchQuery).getOrThrow()
        assertEquals(4, scopeSearch.items.size)

        val dashboard = SqlDelightAnalyticsFacade(database).observeDashboard(
            AnalyticsQuery(
                scope = LedgerScope.All,
                range = DateRange(Instant.fromEpochMilliseconds(0), Instant.fromEpochMilliseconds(10_000)),
            ),
        ).first().getOrThrow()
        assertEquals(Money(800), dashboard.summary.expenseTotal)
        assertEquals(Money(1_000), dashboard.summary.incomeTotal)
        assertEquals(Money.Zero, dashboard.previousSummary.expenseTotal)
        assertEquals(listOf("expense", "expense-2"), dashboard.ranking.map { it.transactionId })
        assertEquals(listOf(Money(500), Money(300)), dashboard.ranking.map { it.amount })
        assertEquals("utensils", dashboard.ranking.first().iconKey)
        assertEquals(listOf("工作", "无标签"), dashboard.tagAnalysis.map { it.tagName })
        assertEquals(listOf(Money(500), Money(300)), dashboard.tagAnalysis.map { it.amount })
        assertEquals(listOf(1, 1), dashboard.tagAnalysis.map { it.transactionCount })
        assertEquals(listOf("food"), dashboard.categoryBreakdowns.map { it.primaryCategoryId })
        assertEquals(listOf("restaurant"), dashboard.categoryBreakdowns.single().secondaryCategories.map { it.categoryId })
        assertEquals(Money(800), dashboard.yearStatement.months.first().expense)
        assertEquals(Money(1_000), dashboard.yearStatement.months.first().income)
    }

    @Test
    fun analyticsRanksTransactionsAndLimitsRankingToTenItems() = runBlocking {
        val database = createJvmDatabase()
        database.ledgerQueries.insertLedger("ledger", "日常", null, 1, 1)
        database.accountQueries.insertAccount("account", "现金", "CASH", "banknote", null, null, 0, 1, 1, 1)
        (1..11).forEach { index ->
            database.categoryQueries.insertCategory(
                "primary-$index", "ledger", null, "分类$index", "folder", "EXPENSE", index.toLong(), 1,
            )
            insert(database, "expense-$index", "primary-$index", index * 100L, "EXPENSE", 1_000, "", excluded = false)
        }
        database.categoryQueries.insertCategory("secondary", "ledger", "primary-1", "子类", "tag", "EXPENSE", 12, 1)
        insert(database, "secondary-1", "secondary", 600, "EXPENSE", 1_100, "", excluded = false)
        insert(database, "secondary-2", "secondary", 700, "EXPENSE", 1_200, "", excluded = false)

        val dashboard = SqlDelightAnalyticsFacade(database).observeDashboard(
            AnalyticsQuery(
                scope = LedgerScope.All,
                range = DateRange(Instant.fromEpochMilliseconds(0), Instant.fromEpochMilliseconds(10_000)),
            ),
        ).first().getOrThrow()

        assertEquals(10, dashboard.ranking.size)
        assertEquals("expense-11", dashboard.ranking.first().transactionId)
        assertEquals(Money(1_100), dashboard.ranking.first().amount)
        assertEquals("分类11", dashboard.ranking.first().categoryDisplayName)
        assertEquals(true, dashboard.ranking.any { it.transactionId == "secondary-2" && it.amount == Money(700) })
        val primary = dashboard.categoryBreakdowns.first { it.primaryCategoryId == "primary-1" }
        assertEquals(Money(1_400), primary.amount)
        assertEquals(listOf("子类"), primary.secondaryCategories.map { it.categoryName })
        assertEquals(Money(1_300), primary.secondaryCategories.single().amount)
    }

    @Test
    fun analyticsMergesMatchingCategoryPathsAcrossLedgers() = runBlocking {
        val database = createJvmDatabase()
        database.ledgerQueries.insertLedger("ledger-1", "日常", null, 1, 1)
        database.ledgerQueries.insertLedger("ledger-2", "旅行", null, 2, 1)
        database.accountQueries.insertAccount("account-1", "现金", "CASH", "banknote", null, null, 0, 1, 1, 1)
        database.accountQueries.insertAccount("account-2", "银行卡", "BANK", "credit-card", null, null, 0, 2, 1, 1)
        database.categoryQueries.insertCategory("food-1", "ledger-1", null, "餐饮", "utensils", "EXPENSE", 1, 1)
        database.categoryQueries.insertCategory("restaurant-1", "ledger-1", "food-1", "餐厅", null, "EXPENSE", 2, 1)
        database.categoryQueries.insertCategory("food-2", "ledger-2", null, "餐饮", "utensils", "EXPENSE", 1, 1)
        database.categoryQueries.insertCategory("restaurant-2", "ledger-2", "food-2", "餐厅", null, "EXPENSE", 2, 1)
        insert(database, "expense-1", "restaurant-1", 500, "EXPENSE", 1_000, "", excluded = false, ledgerId = "ledger-1", accountId = "account-1")
        insert(database, "expense-2", "restaurant-2", 700, "EXPENSE", 1_100, "", excluded = false, ledgerId = "ledger-2", accountId = "account-2")

        val dashboard = SqlDelightAnalyticsFacade(database).observeDashboard(
            AnalyticsQuery(
                scope = LedgerScope.All,
                range = DateRange(Instant.fromEpochMilliseconds(0), Instant.fromEpochMilliseconds(10_000)),
            ),
        ).first().getOrThrow()

        assertEquals(listOf("expense-2", "expense-1"), dashboard.ranking.map { it.transactionId })
        assertEquals(listOf(Money(700), Money(500)), dashboard.ranking.map { it.amount })
        assertEquals(listOf("餐饮"), dashboard.categoryBreakdowns.map { it.primaryCategoryName })
        assertEquals(Money(1_200), dashboard.categoryBreakdowns.single().amount)
        assertEquals(Money(1_200), dashboard.categoryBreakdowns.single().secondaryCategories.single().amount)
    }

    @Test
    fun dashboardYearStatementKeepsMonthsAvailableThroughCurrentMonth() = runBlocking {
        val database = createJvmDatabase()
        database.ledgerQueries.insertLedger("ledger", "日常", null, 1, 1)
        database.accountQueries.insertAccount("account", "现金", "CASH", "banknote", null, null, 0, 1, 1, 1)
        database.categoryQueries.insertCategory("food", "ledger", null, "餐饮", "utensils", "EXPENSE", 1, 1)
        val zone = AppTimeZone
        val now = Clock.System.now().toLocalDateTime(zone)
        val januaryStart = LocalDate(now.year, 1, 1).atStartOfDayIn(zone)
        val februaryStart = LocalDate(now.year, 2, 1).atStartOfDayIn(zone)
        insert(database, "january", "food", 500, "EXPENSE", januaryStart.toEpochMilliseconds(), "", excluded = false)

        val dashboard = SqlDelightAnalyticsFacade(database).observeDashboard(
            AnalyticsQuery(
                scope = LedgerScope.All,
                range = DateRange(januaryStart, februaryStart),
            ),
        ).first().getOrThrow()

        assertEquals((1..now.monthNumber).toList(), dashboard.yearStatement.months.map { it.month })
        assertEquals(Money(500), dashboard.yearStatement.months.first().expense)
    }

    @Test
    fun analyticsBuildsPreviousSummaryAndContinuousTrend() = runBlocking {
        val database = createJvmDatabase()
        database.ledgerQueries.insertLedger("ledger", "日常", null, 1, 1)
        database.accountQueries.insertAccount("account", "现金", "CASH", "banknote", null, null, 0, 1, 1, 1)
        database.categoryQueries.insertCategory("food", "ledger", null, "餐饮", "utensils", "EXPENSE", 1, 1)
        val zone = AppTimeZone
        val start = LocalDate(2025, 1, 1).atStartOfDayIn(zone)
        val secondDay = LocalDate(2025, 1, 2).atStartOfDayIn(zone)
        val thirdDay = LocalDate(2025, 1, 3).atStartOfDayIn(zone)
        val end = LocalDate(2025, 1, 4).atStartOfDayIn(zone)
        val previous = LocalDate(2024, 12, 31).atStartOfDayIn(zone)
        insert(database, "previous", "food", 200, "EXPENSE", previous.toEpochMilliseconds(), "", excluded = false)
        insert(database, "first", "food", 300, "EXPENSE", start.toEpochMilliseconds(), "", excluded = false)
        insert(database, "third", "food", 500, "INCOME", thirdDay.toEpochMilliseconds(), "", excluded = false)

        val dashboard = SqlDelightAnalyticsFacade(database).observeDashboard(
            AnalyticsQuery(
                scope = LedgerScope.All,
                range = DateRange(start, end),
                trendGranularity = TimeGranularity.DAY,
            ),
        ).first().getOrThrow()

        assertEquals(Money(200), dashboard.previousSummary.expenseTotal)
        assertEquals(listOf(start, secondDay, thirdDay), dashboard.trend.points.map { it.start })
        assertEquals(listOf(Money(300), Money.Zero, Money.Zero), dashboard.trend.points.map { it.expense })
        assertEquals(listOf(Money.Zero, Money.Zero, Money(500)), dashboard.trend.points.map { it.income })
    }

    @Test
    fun dashboardPopulatesHealthSummaryCategoryDiffsAndInsight() = runBlocking {
        val database = createJvmDatabase()
        database.ledgerQueries.insertLedger("ledger", "日常", null, 1, 1)
        database.accountQueries.insertAccount("account", "现金", "CASH", "banknote", null, null, 0, 1, 1, 1)
        database.categoryQueries.insertCategory("food", "ledger", null, "餐饮", "utensils", "EXPENSE", 1, 1)
        database.categoryQueries.insertCategory("tech", "ledger", null, "数码", "laptop", "EXPENSE", 2, 1)

        val range = DateRange.ofClosedDates(
            start = kotlinx.datetime.LocalDate(2026, 10, 1),
            endInclusive = kotlinx.datetime.LocalDate(2026, 10, 10),
        )
        val prevRange = DateRange.ofClosedDates(
            start = kotlinx.datetime.LocalDate(2026, 9, 21),
            endInclusive = kotlinx.datetime.LocalDate(2026, 9, 30),
        )
        // Previous period
        insert(database, "prev-food", "food", 2_000, "EXPENSE", prevRange.startInclusive.toEpochMilliseconds() + 1000, "", excluded = false)
        // Current period: food=3000, tech=5000, income=12000
        insert(database, "curr-food", "food", 3_000, "EXPENSE", range.startInclusive.toEpochMilliseconds() + 1000, "", excluded = false)
        insert(database, "curr-tech", "tech", 5_000, "EXPENSE", range.startInclusive.toEpochMilliseconds() + 2000, "", excluded = false)
        insert(database, "curr-income", "food", 12_000, "INCOME", range.startInclusive.toEpochMilliseconds() + 3000, "", excluded = false)

        val dashboard = SqlDelightAnalyticsFacade(database).observeDashboard(
            AnalyticsQuery(
                scope = LedgerScope.All,
                range = range,
            ),
        ).first().getOrThrow()

        // Health Summary checks
        assertEquals(Money(12_000), dashboard.healthSummary.income)
        assertEquals(Money(8_000), dashboard.healthSummary.expense)
        assertEquals(Money(4_000), dashboard.healthSummary.netSavings)
        assertEquals(10, dashboard.healthSummary.daysCount)
        assertEquals(33.33, kotlin.math.round(dashboard.healthSummary.savingsRatePercentage!! * 100) / 100)
        assertEquals(Money(800), dashboard.healthSummary.dailyAverageExpense)

        // Category Diffs checks
        // tech: current=5000, prev=0 -> diff=+5000
        // food: current=3000, prev=2000 -> diff=+1000
        assertEquals(2, dashboard.categoryDiffs.size)
        assertEquals("数码", dashboard.categoryDiffs[0].categoryName)
        assertEquals(Money(5_000), dashboard.categoryDiffs[0].diffAmount)
        assertEquals(true, dashboard.categoryDiffs[0].isIncrease)
        assertEquals("餐饮", dashboard.categoryDiffs[1].categoryName)
        assertEquals(Money(1_000), dashboard.categoryDiffs[1].diffAmount)

        // Insight check
        // Total expense jumped from 2000 to 8000 (+6000 >= 15%), tech contributed 5000 (83% of increase >= 40%) -> ANOMALY_INCREASE
        assertEquals(com.omniflow.core.domain.model.InsightType.ANOMALY_INCREASE, dashboard.insight.type)
        assertEquals("超支异动预警", dashboard.insight.title)
    }

    @Test
    fun analyticsCategoryBreakdownsCalculatesShareRatioAndSumEqualsOne() = runBlocking {
        val database = createJvmDatabase()
        database.ledgerQueries.insertLedger("ledger", "日常", null, 1, 1)
        database.accountQueries.insertAccount("account", "现金", "CASH", "banknote", null, null, 0, 1, 1, 1)
        database.categoryQueries.insertCategory("food", "ledger", null, "餐饮", "utensils", "EXPENSE", 1, 1)
        database.categoryQueries.insertCategory("lunch", "ledger", "food", "午餐", null, "EXPENSE", 1, 1)
        database.categoryQueries.insertCategory("tech", "ledger", null, "数码", "laptop", "EXPENSE", 2, 1)
        database.categoryQueries.insertCategory("daily", "ledger", null, "日用", "cart", "EXPENSE", 3, 1)
        database.categoryQueries.insertCategory("salary", "ledger", null, "工资", "banknote", "INCOME", 4, 1)

        // Expense: lunch 5000 + food 3000 = 8000 (40%), tech 8000 (40%), daily 4000 (20%) -> Total 20000
        insert(database, "t1", "lunch", 5_000, "EXPENSE", 1_000, "午餐", excluded = false)
        insert(database, "t2", "food", 3_000, "EXPENSE", 2_000, "食材", excluded = false)
        insert(database, "t3", "tech", 8_000, "EXPENSE", 3_000, "键盘", excluded = false)
        insert(database, "t4", "daily", 4_000, "EXPENSE", 4_000, "纸巾", excluded = false)
        // Excluded transaction should not count towards breakdown or total
        insert(database, "t5", "daily", 2_000, "EXPENSE", 5_000, "公司报销", excluded = true)
        // Income transaction should not count towards EXPENSE breakdown
        insert(database, "t6", "salary", 50_000, "INCOME", 6_000, "工资", excluded = false)

        val dashboard = SqlDelightAnalyticsFacade(database).observeDashboard(
            AnalyticsQuery(
                scope = LedgerScope.All,
                range = DateRange(Instant.fromEpochMilliseconds(0), Instant.fromEpochMilliseconds(10_000)),
            ),
        ).first().getOrThrow()

        val breakdowns = dashboard.categoryBreakdowns
        assertEquals(3, breakdowns.size)

        // Sum of primary shareRatio must be approximately 1.0
        val sumRatio = breakdowns.sumOf { it.shareRatio }
        assertTrue(abs(sumRatio - 1.0) < 0.0001, "Expected sum of shareRatio to be ~1.0, was $sumRatio")

        val food = breakdowns.first { it.primaryCategoryId == "food" }
        assertEquals(Money(8_000), food.amount)
        assertTrue(abs(food.shareRatio - 0.40) < 0.0001)

        // Secondary category ratio must be relative to the same total (5000 / 20000 = 0.25)
        assertEquals(1, food.secondaryCategories.size)
        val lunch = food.secondaryCategories.single()
        assertEquals(Money(5_000), lunch.amount)
        assertTrue(abs(lunch.shareRatio - 0.25) < 0.0001)

        val tech = breakdowns.first { it.primaryCategoryId == "tech" }
        assertEquals(Money(8_000), tech.amount)
        assertTrue(abs(tech.shareRatio - 0.40) < 0.0001)

        val daily = breakdowns.first { it.primaryCategoryId == "daily" }
        assertEquals(Money(4_000), daily.amount)
        assertTrue(abs(daily.shareRatio - 0.20) < 0.0001)
    }

    @Test
    fun analyticsCategoryBreakdownsHandlesZeroTotalWithoutCrash() = runBlocking {
        val database = createJvmDatabase()
        database.ledgerQueries.insertLedger("ledger", "日常", null, 1, 1)
        database.accountQueries.insertAccount("account", "现金", "CASH", "banknote", null, null, 0, 1, 1, 1)
        database.categoryQueries.insertCategory("food", "ledger", null, "餐饮", "utensils", "EXPENSE", 1, 1)

        // Case 1: No expense transactions at all
        val dashboardEmpty = SqlDelightAnalyticsFacade(database).observeDashboard(
            AnalyticsQuery(
                scope = LedgerScope.All,
                range = DateRange(Instant.fromEpochMilliseconds(0), Instant.fromEpochMilliseconds(10_000)),
            ),
        ).first().getOrThrow()
        assertTrue(dashboardEmpty.categoryBreakdowns.isEmpty())

        // Case 2: Zero-amount expense transaction
        insert(database, "t-zero", "food", 0, "EXPENSE", 1_000, "零元交易", excluded = false)
        val dashboardZero = SqlDelightAnalyticsFacade(database).observeDashboard(
            AnalyticsQuery(
                scope = LedgerScope.All,
                range = DateRange(Instant.fromEpochMilliseconds(0), Instant.fromEpochMilliseconds(10_000)),
            ),
        ).first().getOrThrow()

        assertEquals(1, dashboardZero.categoryBreakdowns.size)
        val item = dashboardZero.categoryBreakdowns.single()
        assertEquals(Money.Zero, item.amount)
        assertEquals(0.0, item.shareRatio)
    }

    private fun insert(
        database: com.omniflow.core.db.OmniFlowDatabase,
        id: String,
        categoryId: String,
        amount: Long,
        type: String,
        occurredAt: Long,
        note: String,
        excluded: Boolean,
        ledgerId: String = "ledger",
        accountId: String = "account",
    ) {
        database.transactionQueries.insertTransaction(
            id, ledgerId, accountId, categoryId, amount, type, occurredAt, note,
            if (excluded) 1 else 0, null, null, occurredAt, occurredAt,
        )
    }
}
