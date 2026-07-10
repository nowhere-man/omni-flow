package com.omniflow.shared.data

import com.omniflow.shared.data.local.createJvmDatabase
import com.omniflow.shared.data.repository.SqlDelightTransactionRepository
import com.omniflow.shared.domain.model.Money
import com.omniflow.shared.domain.model.Transaction
import com.omniflow.shared.domain.model.TransactionType
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class SqlDelightTransactionRepositoryTest {
    @Test
    fun createsTransactionAndUpdatesAccountBalanceAtomically() = runBlocking {
        val database = createJvmDatabase()
        seed(database)
        val repository = SqlDelightTransactionRepository(database) { Instant.fromEpochMilliseconds(5_000) }

        repository.create(transaction("income", TransactionType.INCOME, Money(1_000)))
        repository.create(transaction("expense", TransactionType.EXPENSE, Money(240)))

        assertEquals(760L, database.accountQueries.activeAccounts().executeAsOne().balance_minor)
        assertEquals(2, database.transactionQueries.transactionsForLedger("ledger").executeAsList().size)
    }

    @Test
    fun rejectsCategoryOutsideTransactionLedgerWithoutWritingAnything() = runBlocking {
        val database = createJvmDatabase()
        seed(database)
        val repository = SqlDelightTransactionRepository(database)

        try {
            repository.create(transaction("wrong-category", TransactionType.EXPENSE, Money(100), "other-category"))
            fail("Expected a category validation failure")
        } catch (_: IllegalArgumentException) {
        }

        assertEquals(0, database.transactionQueries.transactionsForLedger("ledger").executeAsList().size)
        assertEquals(0L, database.accountQueries.activeAccounts().executeAsOne().balance_minor)
    }

    private fun seed(database: com.omniflow.shared.db.OmniFlowDatabase) {
        database.ledgerQueries.insertLedger("ledger", "账本", null, 1, 1)
        database.ledgerQueries.insertLedger("other-ledger", "其他账本", null, 1, 1)
        database.accountQueries.insertAccount(
            id = "account",
            name = "现金",
            type = "CASH",
            icon_key = "wallet",
            card_number = null,
            note = null,
            balance_minor = 0,
            include_in_total_assets = 1L,
            created_at = 1,
            updated_at = 1,
        )
        database.categoryQueries.insertCategory("category", "ledger", null, "餐饮", "utensils", "EXPENSE", 1, 1)
        database.categoryQueries.insertCategory("income-category", "ledger", null, "工资", "banknote", "INCOME", 1, 1)
        database.categoryQueries.insertCategory("other-category", "other-ledger", null, "餐饮", "utensils", "EXPENSE", 1, 1)
    }

    private fun transaction(
        id: String,
        type: TransactionType,
        amount: Money,
        categoryId: String = if (type == TransactionType.INCOME) "income-category" else "category",
    ) = Transaction(
        id = id,
        ledgerId = "ledger",
        accountId = "account",
        categoryId = categoryId,
        amount = amount,
        type = type,
        occurredAt = Instant.fromEpochMilliseconds(1_000),
        note = null,
        isExcluded = false,
        source = null,
        externalId = null,
    )
}
