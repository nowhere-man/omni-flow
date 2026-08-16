package com.omniflow.core.data.facade

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.omniflow.core.db.OmniFlowDatabase
import com.omniflow.core.domain.facade.BudgetFacade
import com.omniflow.core.domain.model.Budget
import com.omniflow.core.domain.model.BudgetId
import com.omniflow.core.domain.model.CategoryId
import com.omniflow.core.domain.model.LedgerId
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.util.UuidGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class SqlDelightBudgetFacade(
    private val database: OmniFlowDatabase,
    private val ids: UuidGenerator = UuidGenerator(),
    private val now: () -> Instant = { Clock.System.now() },
) : BudgetFacade {
    override fun observe(ledgerId: LedgerId): Flow<Result<List<Budget>>> =
        database.budgetQueries.budgetsForLedger(ledgerId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows ->
                runCatching {
                    rows.map { row ->
                        Budget(
                            id = row.id,
                            ledgerId = row.ledger_id,
                            categoryId = row.category_id,
                            amount = Money(row.amount_minor),
                            deletedAt = row.deleted_at?.let(Instant::fromEpochMilliseconds),
                        )
                    }
                }
            }

    override suspend fun save(
        id: BudgetId?,
        ledgerId: LedgerId,
        categoryId: CategoryId?,
        amount: Money,
    ): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            require(amount.minor > 0) { "预算金额需要大于 0" }
            val timestamp = now().toEpochMilliseconds()
            val existing = database.budgetQueries.budgetsForLedger(ledgerId).executeAsList()
            // 同一个范围（总预算或某个一级分类）只保留一条，重复新建当成改额度
            val target = id ?: existing.firstOrNull { it.category_id == categoryId }?.id
            if (target == null) {
                database.budgetQueries.insertBudget(
                    id = ids.next(),
                    ledger_id = ledgerId,
                    category_id = categoryId,
                    amount_minor = amount.minor,
                    created_at = timestamp,
                    updated_at = timestamp,
                    deleted_at = null,
                )
            } else {
                database.budgetQueries.updateBudget(
                    amount_minor = amount.minor,
                    updated_at = timestamp,
                    id = target,
                )
            }
            Unit
        }
    }

    override suspend fun delete(id: BudgetId): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            val timestamp = now().toEpochMilliseconds()
            database.budgetQueries.deleteBudget(deleted_at = timestamp, updated_at = timestamp, id = id)
            Unit
        }
    }
}
