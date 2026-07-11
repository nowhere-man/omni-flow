package com.omniflow.shared.data

import com.omniflow.shared.data.facade.SqlDelightQingziInteropFacade
import com.omniflow.shared.data.local.createJvmDatabase
import com.omniflow.shared.parser.qingzi.QingziBillParser
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
}
