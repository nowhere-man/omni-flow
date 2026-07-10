package com.omniflow.shared.data

import com.omniflow.shared.data.local.createJvmDatabase
import com.omniflow.shared.data.repository.SqlDelightInitialDataRepository
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SqlDelightInitialDataRepositoryTest {
    @Test
    fun seedsInitialLedgerCategoriesAndAccountsOnlyOnce() = runBlocking {
        val database = createJvmDatabase()
        val repository = SqlDelightInitialDataRepository(database) {
            Instant.fromEpochMilliseconds(1_000)
        }

        repository.seedIfNeeded()
        repository.seedIfNeeded()

        assertEquals(1, database.ledgerQueries.activeLedgers().executeAsList().size)
        assertEquals(16, database.categoryQueries.activeCategoriesForLedger(
            database.ledgerQueries.activeLedgers().executeAsOne().id,
        ).executeAsList().size)
        assertEquals(7, database.accountQueries.activeAccounts().executeAsList().size)
        assertNull(database.appPreferenceQueries.preference("default_ledger_id").executeAsOneOrNull())
    }
}
