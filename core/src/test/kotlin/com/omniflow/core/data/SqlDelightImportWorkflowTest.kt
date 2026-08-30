package com.omniflow.core.data

import com.omniflow.core.data.facade.SqlDelightImportWorkflow
import com.omniflow.core.data.local.createJvmDatabase
import com.omniflow.core.data.repository.SqlDelightAccountRepository
import com.omniflow.core.data.repository.SqlDelightCategoryMemoryRepository
import com.omniflow.core.data.repository.SqlDelightCategoryRepository
import com.omniflow.core.data.repository.SqlDelightImportSessionRepository
import com.omniflow.core.data.repository.SqlDelightRuleRepository
import com.omniflow.core.data.repository.SqlDelightTransactionDedupeRepository
import com.omniflow.core.data.repository.SqlDelightTransactionRepository
import com.omniflow.core.domain.model.ImportCategoryBatchEdit
import com.omniflow.core.domain.model.ImportCategoryOrigin
import com.omniflow.core.domain.model.ImportPreviewItem
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.TransactionType
import com.omniflow.core.parser.ImportFormat
import com.omniflow.core.parser.RawTransaction
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlin.test.assertEquals
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SqlDelightImportWorkflowTest {
    @Test
    fun cancellingImportDeletesPreviewSession() = runBlocking {
        val database = createJvmDatabase()
        database.ledgerQueries.insertLedger("ledger", "日常", null, 1, 1)
        val sessions = SqlDelightImportSessionRepository(database)
        sessions.create("session", "ledger", ImportFormat.ALIPAY, emptyList())
        val workflow = SqlDelightImportWorkflow(
            sessions = sessions,
            commits = SqlDelightTransactionRepository(database),
            accounts = SqlDelightAccountRepository(database),
            categories = SqlDelightCategoryRepository(database),
            rules = SqlDelightRuleRepository(database),
            categoryMemory = SqlDelightCategoryMemoryRepository(database),
            dedupe = SqlDelightTransactionDedupeRepository(database),
        )

        assertTrue(workflow.cancel("session").isSuccess)
        assertNull(sessions.state("session"))
    }

    @Test
    fun changingCategoryDirectionAlsoChangesTransactionTypeBeforeCommit() = runBlocking {
        val database = createJvmDatabase()
        database.ledgerQueries.insertLedger("ledger", "日常", null, 1, 1)
        database.accountQueries.insertAccount("account", "现金", "CASH", "wallet", null, null, 0, 1, 1, 1)
        database.categoryQueries.insertCategory("expense", "ledger", null, "餐饮", "utensils", "EXPENSE", 1, 1)
        database.categoryQueries.insertCategory("income", "ledger", null, "理财", "savings", "INCOME", 1, 1)

        val sessions = SqlDelightImportSessionRepository(database)
        sessions.create(
            sessionId = "session",
            ledgerId = "ledger",
            format = ImportFormat.MEITUAN,
            items = listOf(
                ImportPreviewItem(
                    id = "item",
                    raw = RawTransaction(
                        format = ImportFormat.MEITUAN,
                        occurredAt = Instant.fromEpochMilliseconds(1),
                        amount = Money(100),
                        type = TransactionType.EXPENSE,
                        isExcluded = false,
                        accountName = "现金",
                        note = "测试",
                        externalId = "external",
                        sourceCategory = null,
                    ),
                    type = TransactionType.EXPENSE,
                    categoryId = "expense",
                    categoryOrigin = ImportCategoryOrigin.NONE,
                    accountId = "account",
                    note = "测试",
                    tags = emptyList(),
                    isExcluded = false,
                    isSkipped = false,
                    duplicateStatus = com.omniflow.core.domain.model.ImportDuplicateStatus.NONE,
                ),
            ),
        )
        val workflow = SqlDelightImportWorkflow(
            sessions = sessions,
            commits = SqlDelightTransactionRepository(database),
            accounts = SqlDelightAccountRepository(database),
            categories = SqlDelightCategoryRepository(database),
            rules = SqlDelightRuleRepository(database),
            categoryMemory = SqlDelightCategoryMemoryRepository(database),
            dedupe = SqlDelightTransactionDedupeRepository(database),
        )

        val edited = workflow.editCategories(
            "session",
            ImportCategoryBatchEdit(setOf("item"), "income", TransactionType.INCOME),
        ).getOrThrow()

        assertEquals(TransactionType.INCOME, edited.items.single().type)
        assertTrue(workflow.commit("session").isSuccess)
        assertEquals(1, database.transactionQueries.transactionsForLedger("ledger").executeAsList().size)
    }
}
