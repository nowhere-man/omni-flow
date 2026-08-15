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
import com.omniflow.core.parser.ImportFormat
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
