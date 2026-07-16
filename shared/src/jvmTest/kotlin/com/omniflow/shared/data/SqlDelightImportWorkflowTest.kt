package com.omniflow.shared.data

import com.omniflow.shared.data.facade.SqlDelightImportWorkflow
import com.omniflow.shared.data.local.createJvmDatabase
import com.omniflow.shared.data.repository.SqlDelightAccountRepository
import com.omniflow.shared.data.repository.SqlDelightCategoryMemoryRepository
import com.omniflow.shared.data.repository.SqlDelightCategoryRepository
import com.omniflow.shared.data.repository.SqlDelightImportSessionRepository
import com.omniflow.shared.data.repository.SqlDelightRuleRepository
import com.omniflow.shared.data.repository.SqlDelightTransactionDedupeRepository
import com.omniflow.shared.data.repository.SqlDelightTransactionRepository
import com.omniflow.shared.parser.ImportFormat
import kotlinx.coroutines.runBlocking
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
}
