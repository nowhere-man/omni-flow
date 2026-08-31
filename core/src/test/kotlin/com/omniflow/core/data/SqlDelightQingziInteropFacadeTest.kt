package com.omniflow.core.data

import com.omniflow.core.data.facade.SqlDelightQingziInteropFacade
import com.omniflow.core.data.local.createJvmDatabase
import com.omniflow.core.domain.model.QingziExportRequest
import com.omniflow.core.domain.model.TransactionType
import com.omniflow.core.parser.qingzi.QingziBillParser
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class SqlDelightQingziInteropFacadeTest {
    @Test
    fun exportsDataThatTheQingziParserCanRead() = runBlocking {
        val database = createJvmDatabase()
        database.ledgerQueries.insertLedger("ledger", "日常", null, 1, 1)
        database.accountQueries.insertAccount("account", "现金", "CASH", "banknote", null, null, 0, 1, 1, 1)
        database.categoryQueries.insertCategory("category", "ledger", null, "餐饮", "utensils", "EXPENSE", 1, 1)
        database.transactionQueries.insertTransaction(
            "transaction", "ledger", "account", "category", 1_234, "EXPENSE", 1_000,
            "午餐", 0, null, null, 1, 1,
        )

        val exported = SqlDelightQingziInteropFacade(database).export().getOrThrow()
        val imported = QingziBillParser().parse(exported.payload).getOrThrow()

        assertEquals(1, exported.exportedTransactions)
        assertEquals(1, imported.transactions.size)
        assertEquals(1_234, imported.transactions.single().amount.minor)
    }

    @Test
    fun filtersExportByLedgerAndTransactionType() = runBlocking {
        val database = createJvmDatabase()
        database.ledgerQueries.insertLedger("ledger", "日常", null, 1, 1)
        database.ledgerQueries.insertLedger("other-ledger", "旅行", null, 1, 1)
        database.accountQueries.insertAccount("account", "现金", "CASH", "banknote", null, null, 0, 1, 1, 1)
        database.categoryQueries.insertCategory("expense", "ledger", null, "餐饮", "utensils", "EXPENSE", 1, 1)
        database.categoryQueries.insertCategory("income", "ledger", null, "工资", "banknote", "INCOME", 1, 1)
        database.categoryQueries.insertCategory("other-expense", "other-ledger", null, "交通", "car", "EXPENSE", 1, 1)
        database.transactionQueries.insertTransaction(
            "expense", "ledger", "account", "expense", 100, "EXPENSE", 1_000,
            null, 0, null, null, 1, 1,
        )
        database.transactionQueries.insertTransaction(
            "income", "ledger", "account", "income", 200, "INCOME", 1_000,
            null, 0, null, null, 1, 1,
        )
        database.transactionQueries.insertTransaction(
            "other-expense", "other-ledger", "account", "other-expense", 300, "EXPENSE", 1_000,
            null, 0, null, null, 1, 1,
        )

        val exported = SqlDelightQingziInteropFacade(database).export(
            QingziExportRequest(ledgerIds = setOf("ledger"), type = TransactionType.INCOME),
        ).getOrThrow()

        assertEquals(1, exported.exportedTransactions)
        assertEquals(true, exported.payload.contains("\"id\":\"income\""))
        assertEquals(false, exported.payload.contains("\"id\":\"expense\""))
        assertEquals(false, exported.payload.contains("\"id\":\"other-expense\""))
    }
}
