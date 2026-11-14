package com.omniflow.core.domain.model

import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals

class LedgerAggregatesTest {

    private fun createTx(
        id: String,
        amount: Money,
        type: TransactionType,
        isExcluded: Boolean = false,
        deleted: Boolean = false,
    ) = Transaction(
        id = id,
        ledgerId = "ledger-1",
        accountId = "acc-1",
        categoryId = "cat-1",
        amount = amount,
        type = type,
        occurredAt = Clock.System.now(),
        note = null,
        isExcluded = isExcluded,
        source = null,
        externalId = null,
        deletedAt = if (deleted) Clock.System.now() else null,
    )

    @Test
    fun calculateOverview_aggregatesIncomeExpenseAndExcludesTransfers() {
        val txs = listOf(
            createTx("1", Money(3500), TransactionType.EXPENSE), // 35.00
            createTx("2", Money(1500), TransactionType.EXPENSE), // 15.00
            createTx("3", Money(10000), TransactionType.INCOME), // 100.00
            createTx("4", Money(5000), TransactionType.TRANSFER), // 内部转账：不计入收支
            createTx("5", Money(2000), TransactionType.EXPENSE, isExcluded = true), // 排除的交易
            createTx("6", Money(8000), TransactionType.INCOME, deleted = true), // 软删除的交易
        )

        val overview = LedgerAggregator.calculateOverview(txs)

        // 有效交易笔数：1, 2, 3, 4 (转账计入笔数，但收支不计入)
        assertEquals(4, overview.transactionCount)
        assertEquals(Money(10000), overview.totalIncome)
        assertEquals(Money(5000), overview.totalExpense)
        assertEquals(Money(5000), overview.netBalance) // 100.00 - 50.00 = 50.00
    }

    @Test
    fun calculateOverview_emptyTransactionsReturnsZeros() {
        val overview = LedgerAggregator.calculateOverview(emptyList())
        assertEquals(0, overview.transactionCount)
        assertEquals(Money.Zero, overview.totalIncome)
        assertEquals(Money.Zero, overview.totalExpense)
        assertEquals(Money.Zero, overview.netBalance)
    }
}
