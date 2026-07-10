package com.omniflow.shared.data.repository

import com.omniflow.shared.db.OmniFlowDatabase
import com.omniflow.shared.domain.model.CategoryId
import com.omniflow.shared.domain.model.LedgerId
import com.omniflow.shared.domain.repository.CategoryMemoryRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class SqlDelightCategoryMemoryRepository(
    private val database: OmniFlowDatabase,
    private val now: () -> Instant = { Clock.System.now() },
) : CategoryMemoryRepository {
    override suspend fun categoryId(ledgerId: LedgerId, memoryKey: String): CategoryId? = database.categoryMemoryQueries
        .categoryMemory(ledgerId, memoryKey)
        .executeAsOneOrNull()

    override suspend fun remember(ledgerId: LedgerId, memoryKey: String, categoryId: CategoryId) {
        database.categoryMemoryQueries.upsertCategoryMemory(
            ledger_id = ledgerId,
            memory_key = memoryKey,
            category_id = categoryId,
            updated_at = now().toEpochMilliseconds(),
        )
    }
}
