package com.omniflow.core.data

import com.omniflow.core.data.facade.SqlDelightAnalyticsFacade
import com.omniflow.core.data.facade.SqlDelightHomeFacade
import com.omniflow.core.data.local.createJvmDatabase
import com.omniflow.core.data.repository.SqlDelightTransactionRepository
import com.omniflow.core.domain.model.AnalyticsQuery
import com.omniflow.core.domain.model.DateRange
import com.omniflow.core.domain.model.HomeQuery
import com.omniflow.core.domain.model.LedgerScope
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.Transaction
import com.omniflow.core.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class SqlDelightAnalyticsTransferExclusionTest {
    @Test
    fun transfersAreExcludedFromIncomeAndExpenseStatsAndBreakdowns() = runBlocking {
        val database = createJvmDatabase()
        database.ledgerQueries.insertLedger("ledger", "日常账本", null, 1, 1)
        database.accountQueries.insertAccount("acc-1", "银行卡", "DEBIT_CARD", "landmark", null, null, 10000, 1, 1, 1)
        database.accountQueries.insertAccount("acc-2", "微信", "E_WALLET", "wallet", null, null, 2000, 1, 1, 1)
        database.categoryQueries.insertCategory("cat-expense", "ledger", null, "餐饮", "utensils", "EXPENSE", 1, 1)
        database.categoryQueries.insertCategory("cat-income", "ledger", null, "工资", "banknote", "INCOME", 1, 1)

        val repository = SqlDelightTransactionRepository(database)

        // Normal expense 500, normal income 2000
        repository.create(
            Transaction(
                id = "tx-expense",
                ledgerId = "ledger",
                accountId = "acc-1",
                categoryId = "cat-expense",
                amount = Money(500),
                type = TransactionType.EXPENSE,
                occurredAt = Instant.fromEpochMilliseconds(2_000),
                note = "午餐",
                isExcluded = false,
                source = null,
                externalId = null,
            )
        )
        repository.create(
            Transaction(
                id = "tx-income",
                ledgerId = "ledger",
                accountId = "acc-1",
                categoryId = "cat-income",
                amount = Money(2000),
                type = TransactionType.INCOME,
                occurredAt = Instant.fromEpochMilliseconds(2_100),
                note = "工资",
                isExcluded = false,
                source = null,
                externalId = null,
            )
        )

        // Transfer 3000 from acc-1 to acc-2
        repository.create(
            Transaction(
                id = "tx-transfer",
                ledgerId = "ledger",
                accountId = "acc-1",
                targetAccountId = "acc-2",
                categoryId = "",
                amount = Money(3000),
                type = TransactionType.TRANSFER,
                occurredAt = Instant.fromEpochMilliseconds(2_200),
                note = "充值微信",
                isExcluded = false,
                source = null,
                externalId = null,
            )
        )

        val range = DateRange(Instant.fromEpochMilliseconds(1_000), Instant.fromEpochMilliseconds(5_000))
        val analyticsFacade = SqlDelightAnalyticsFacade(database)

        val dashboard = analyticsFacade.observeDashboard(
            AnalyticsQuery(
                scope = LedgerScope.All,
                range = range,
            )
        ).first().getOrThrow()

        assertEquals(Money(500), dashboard.summary.expenseTotal, "Transfer must not count as expense")
        assertEquals(Money(2000), dashboard.summary.incomeTotal, "Transfer must not count as income")
        assertEquals(Money(1500), dashboard.summary.netIncome)

        assertEquals(1, dashboard.ranking.size)
        assertEquals("tx-expense", dashboard.ranking.first().transactionId)

        assertEquals(1, dashboard.categoryBreakdowns.size)
        assertEquals("餐饮", dashboard.categoryBreakdowns.first().primaryCategoryName)

        assertEquals(Money(500), dashboard.healthSummary.expense)
        assertEquals(Money(2000), dashboard.healthSummary.income)
        assertEquals(Money(1500), dashboard.healthSummary.netSavings)

        val homeFacade = SqlDelightHomeFacade(database)
        val homeState = homeFacade.observeHome(HomeQuery(LedgerScope.All, range)).first().getOrThrow()
        assertEquals(Money(500), homeState.summary.expenseTotal)
        assertEquals(Money(2000), homeState.summary.incomeTotal)

        // Balances: acc-1: 10000 - 500 + 2000 - 3000 = 8500. acc-2: 2000 + 3000 = 5000
        val acc1Balance = database.accountQueries.activeAccountBalance("acc-1").executeAsOne()
        val acc2Balance = database.accountQueries.activeAccountBalance("acc-2").executeAsOne()
        assertEquals(8500L, acc1Balance)
        assertEquals(5000L, acc2Balance)
    }
}
