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
import com.omniflow.core.domain.model.DateRange
import com.omniflow.core.domain.model.ImportCategoryBatchEdit
import com.omniflow.core.domain.model.ImportCategoryOrigin
import com.omniflow.core.domain.model.ImportDuplicateStatus
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

    @Test
    fun commitOnlyImportsTransactionsInsideDateRange() = runBlocking {
        val database = seededDatabase()
        val sessions = SqlDelightImportSessionRepository(database)
        sessions.create(
            sessionId = "session",
            ledgerId = "ledger",
            format = ImportFormat.MEITUAN,
            items = listOf(
                previewItem("inside", Instant.parse("2026-01-06T08:00:00Z")),
                previewItem("outside", Instant.parse("2026-01-05T08:00:00Z")),
            ),
        )
        val workflow = importWorkflow(database, sessions)

        val result = workflow.commit(
            "session",
            DateRange(Instant.parse("2026-01-06T00:00:00Z"), Instant.parse("2026-01-07T00:00:00Z")),
        ).getOrThrow()

        assertEquals(1, result.importedCount)
        assertEquals(1, result.excludedCount, "区间外的可入账条目按跳过计")
        val rows = database.transactionQueries.transactionsForLedger("ledger").executeAsList()
        assertEquals(1, rows.size)
        assertEquals("inside", rows.single().external_id)
    }

    @Test
    fun outOfRangePendingItemDoesNotBlockCommit() = runBlocking {
        // 区间外的待分类明细用户在预览页看不见，不该堵死入账
        val database = seededDatabase()
        val sessions = SqlDelightImportSessionRepository(database)
        val outside = previewItem("outside", Instant.parse("2026-01-05T08:00:00Z"))
        sessions.create(
            sessionId = "session",
            ledgerId = "ledger",
            format = ImportFormat.MEITUAN,
            items = listOf(
                previewItem("inside", Instant.parse("2026-01-06T08:00:00Z")),
                outside.copy(type = null, categoryId = null),
            ),
        )
        val workflow = importWorkflow(database, sessions)

        val result = workflow.commit(
            "session",
            DateRange(Instant.parse("2026-01-06T00:00:00Z"), Instant.parse("2026-01-07T00:00:00Z")),
        ).getOrThrow()

        assertEquals(1, result.importedCount)
        assertEquals(1, database.transactionQueries.transactionsForLedger("ledger").executeAsList().size)
    }

    private fun seededDatabase() = createJvmDatabase().apply {
        ledgerQueries.insertLedger("ledger", "日常", null, 1, 1)
        accountQueries.insertAccount("account", "现金", "CASH", "wallet", null, null, 0, 1, 1, 1)
        categoryQueries.insertCategory("expense", "ledger", null, "餐饮", "utensils", "EXPENSE", 1, 1)
    }

    private fun importWorkflow(database: com.omniflow.core.db.OmniFlowDatabase, sessions: SqlDelightImportSessionRepository) =
        SqlDelightImportWorkflow(
            sessions = sessions,
            commits = SqlDelightTransactionRepository(database),
            accounts = SqlDelightAccountRepository(database),
            categories = SqlDelightCategoryRepository(database),
            rules = SqlDelightRuleRepository(database),
            categoryMemory = SqlDelightCategoryMemoryRepository(database),
            dedupe = SqlDelightTransactionDedupeRepository(database),
        )

    private fun previewItem(id: String, occurredAt: Instant) = ImportPreviewItem(
        id = id,
        raw = RawTransaction(
            format = ImportFormat.MEITUAN,
            occurredAt = occurredAt,
            amount = Money(100),
            type = TransactionType.EXPENSE,
            isExcluded = false,
            accountName = "现金",
            note = id,
            externalId = id,
            sourceCategory = null,
        ),
        type = TransactionType.EXPENSE,
        categoryId = "expense",
        categoryOrigin = ImportCategoryOrigin.NONE,
        accountId = "account",
        note = id,
        tags = emptyList(),
        isExcluded = false,
        isSkipped = false,
        duplicateStatus = ImportDuplicateStatus.NONE,
    )
}
