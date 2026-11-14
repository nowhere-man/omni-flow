package com.omniflow.core.data.facade

import com.omniflow.core.db.OmniFlowDatabase
import com.omniflow.core.domain.facade.BatchTransactionFacade
import com.omniflow.core.domain.model.AccountId
import com.omniflow.core.domain.model.AppTimeZone
import com.omniflow.core.domain.model.BatchOperationResult
import com.omniflow.core.domain.model.CategoryId
import com.omniflow.core.domain.model.LedgerId
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.Transaction
import com.omniflow.core.domain.model.TransactionId
import com.omniflow.core.domain.model.TransactionSource
import com.omniflow.core.domain.model.TransactionType
import com.omniflow.core.domain.util.UuidGenerator
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

class SqlDelightBatchTransactionFacade(
    private val database: OmniFlowDatabase,
    private val ids: UuidGenerator = UuidGenerator(),
    private val now: () -> Instant = { Clock.System.now() },
) : BatchTransactionFacade {

    override suspend fun delete(transactionIds: List<TransactionId>): Result<BatchOperationResult> = runCatching {
        if (transactionIds.isEmpty()) return@runCatching BatchOperationResult(0, 0, 0)
        val timestamp = now().toEpochMilliseconds()
        database.transaction {
            transactionIds.forEach { id ->
                val existing = findActiveTransaction(id) ?: error("交易不存在或已删除: $id")
                revertBalanceEffects(existing, timestamp)
                database.transactionQueries.archiveTransaction(timestamp, id)
                database.tagQueries.deleteTransactionTags(id)
            }
        }
        BatchOperationResult(
            totalCount = transactionIds.size,
            processedCount = transactionIds.size,
            skippedCount = 0,
        )
    }

    override suspend fun updateAccount(
        transactionIds: List<TransactionId>,
        targetAccountId: AccountId,
    ): Result<BatchOperationResult> = runCatching {
        if (transactionIds.isEmpty()) return@runCatching BatchOperationResult(0, 0, 0)
        val timestamp = now().toEpochMilliseconds()
        var processed = 0
        var skipped = 0

        database.transaction {
            val targetAccount = database.accountQueries.activeAccountId(targetAccountId).executeAsOneOrNull()
                ?: error("目标账户不存在或已删除: $targetAccountId")

            transactionIds.forEach { id ->
                val existing = findActiveTransaction(id) ?: error("交易不存在或已删除: $id")
                if (existing.type == TransactionType.TRANSFER) {
                    skipped++
                    return@forEach
                }

                if (existing.accountId != targetAccount) {
                    revertBalanceEffects(existing, timestamp)
                    val updated = existing.copy(accountId = targetAccount)
                    applyBalanceEffects(updated, timestamp)
                    saveTransactionRecord(updated, timestamp)
                }
                processed++
            }
        }

        BatchOperationResult(
            totalCount = transactionIds.size,
            processedCount = processed,
            skippedCount = skipped,
        )
    }

    override suspend fun updateCategory(
        transactionIds: List<TransactionId>,
        targetCategoryId: CategoryId,
    ): Result<BatchOperationResult> = runCatching {
        if (transactionIds.isEmpty()) return@runCatching BatchOperationResult(0, 0, 0)
        val timestamp = now().toEpochMilliseconds()
        var processed = 0
        var skipped = 0

        database.transaction {
            val targetCategory = database.categoryQueries.activeCategory(targetCategoryId).executeAsOneOrNull()
                ?: error("目标分类不存在或已删除: $targetCategoryId")

            transactionIds.forEach { id ->
                val existing = findActiveTransaction(id) ?: error("交易不存在或已删除: $id")
                if (existing.type == TransactionType.TRANSFER) {
                    skipped++
                    return@forEach
                }

                if (existing.type.name != targetCategory.type) {
                    skipped++
                    return@forEach
                }

                if (existing.categoryId != targetCategory.id) {
                    val updated = existing.copy(categoryId = targetCategory.id)
                    // 改分类不影响任何账户余额
                    saveTransactionRecord(updated, timestamp)
                }
                processed++
            }
        }

        BatchOperationResult(
            totalCount = transactionIds.size,
            processedCount = processed,
            skippedCount = skipped,
        )
    }

    override suspend fun updateLedger(
        transactionIds: List<TransactionId>,
        targetLedgerId: LedgerId,
    ): Result<BatchOperationResult> = runCatching {
        if (transactionIds.isEmpty()) return@runCatching BatchOperationResult(0, 0, 0)
        val timestamp = now().toEpochMilliseconds()
        var processed = 0

        database.transaction {
            database.ledgerQueries.activeLedgerId(targetLedgerId).executeAsOneOrNull()
                ?: error("目标账本不存在或已删除: $targetLedgerId")

            transactionIds.forEach { id ->
                val existing = findActiveTransaction(id) ?: error("交易不存在或已删除: $id")
                if (existing.ledgerId == targetLedgerId) {
                    processed++
                    return@forEach
                }

                val resolvedCategoryId = if (existing.type == TransactionType.TRANSFER) {
                    findOrCreateTransferCategory(targetLedgerId, timestamp)
                } else {
                    resolveCategoryInLedger(existing, targetLedgerId, timestamp)
                }

                val targetTagIds = resolveTagsInLedger(existing.tagIds, targetLedgerId, timestamp)
                val updated = existing.copy(
                    ledgerId = targetLedgerId,
                    categoryId = resolvedCategoryId,
                    tagIds = targetTagIds,
                )

                // 改账本不影响任何账户余额
                saveTransactionRecord(updated, timestamp)
                replaceTags(updated.id, targetTagIds)
                processed++
            }
        }

        BatchOperationResult(
            totalCount = transactionIds.size,
            processedCount = processed,
            skippedCount = 0,
        )
    }

    private fun resolveCategoryInLedger(
        existing: Transaction,
        targetLedgerId: String,
        timestamp: Long,
    ): String {
        val originalCategory = database.categoryQueries.activeCategory(existing.categoryId).executeAsOneOrNull()
        val targetCategories = database.categoryQueries.activeCategoriesForLedger(targetLedgerId).executeAsList()
        val matchByName = targetCategories.firstOrNull {
            it.name == originalCategory?.name && it.type == existing.type.name
        }
        if (matchByName != null) return matchByName.id

        val fallback = targetCategories.firstOrNull { it.parent_id == null && it.type == existing.type.name }
        if (fallback != null) return fallback.id

        val newId = ids.next()
        database.categoryQueries.insertCategory(
            id = newId,
            ledger_id = targetLedgerId,
            parent_id = null,
            name = originalCategory?.name ?: if (existing.type == TransactionType.EXPENSE) "支出" else "收入",
            icon_key = originalCategory?.icon_key ?: "tag",
            type = existing.type.name,
            created_at = timestamp,
            updated_at = timestamp,
        )
        return newId
    }

    private fun resolveTagsInLedger(
        tagIds: Set<String>,
        targetLedgerId: String,
        timestamp: Long,
    ): Set<String> {
        if (tagIds.isEmpty()) return emptySet()
        return tagIds.mapNotNull { tagId ->
            val tag = database.tagQueries.activeTag(tagId).executeAsOneOrNull() ?: return@mapNotNull null
            val existingInTarget = database.tagQueries.activeTagIdByName(targetLedgerId, tag.name).executeAsOneOrNull()
            if (existingInTarget != null) {
                existingInTarget
            } else {
                val newTagId = ids.next()
                database.tagQueries.insertTag(
                    id = newTagId,
                    ledger_id = targetLedgerId,
                    name = tag.name,
                    created_at = timestamp,
                    updated_at = timestamp,
                )
                newTagId
            }
        }.toSet()
    }

    private fun findOrCreateTransferCategory(ledgerId: String, timestamp: Long): String {
        val existing = database.categoryQueries.activeCategoriesForLedger(ledgerId)
            .executeAsList()
            .firstOrNull { it.type == TransactionType.TRANSFER.name }
        if (existing != null) return existing.id
        val newId = ids.next()
        database.categoryQueries.insertCategory(
            id = newId,
            ledger_id = ledgerId,
            parent_id = null,
            name = "内部转账",
            icon_key = "arrow-left-right",
            type = TransactionType.TRANSFER.name,
            created_at = timestamp,
            updated_at = timestamp,
        )
        return newId
    }

    private fun findActiveTransaction(transactionId: TransactionId): Transaction? {
        val row = database.transactionQueries.activeTransaction(transactionId).executeAsOneOrNull() ?: return null
        return Transaction(
            id = row.id,
            ledgerId = row.ledger_id,
            accountId = row.account_id,
            categoryId = row.category_id,
            amount = Money(row.amount_minor),
            type = TransactionType.valueOf(row.type),
            occurredAt = Instant.fromEpochMilliseconds(row.occurred_at),
            note = row.note,
            isExcluded = row.is_excluded != 0L,
            source = row.external_source?.let(TransactionSource::valueOf),
            externalId = row.external_id,
            tagIds = database.tagQueries.tagsForTransaction(transactionId).executeAsList().toSet(),
            deletedAt = row.deleted_at?.let(Instant::fromEpochMilliseconds),
            targetAccountId = row.target_account_id,
        )
    }

    private fun saveTransactionRecord(transaction: Transaction, timestamp: Long) {
        database.transactionQueries.updateTransaction(
            ledger_id = transaction.ledgerId,
            account_id = transaction.accountId,
            category_id = transaction.categoryId,
            amount_minor = transaction.amount.minor,
            type = transaction.type.name,
            occurred_at = transaction.occurredAt.toEpochMilliseconds(),
            note = transaction.note,
            is_excluded = if (transaction.isExcluded) 1L else 0L,
            external_source = transaction.source?.name,
            external_id = transaction.externalId,
            updated_at = timestamp,
            target_account_id = transaction.targetAccountId,
            id = transaction.id,
        )
    }

    private fun replaceTags(transactionId: TransactionId, tagIds: Set<String>) {
        database.tagQueries.deleteTransactionTags(transactionId)
        tagIds.forEach { tagId -> database.tagQueries.insertTransactionTag(transactionId, tagId) }
    }

    private fun applyBalanceEffects(transaction: Transaction, timestamp: Long) {
        when (transaction.type) {
            TransactionType.INCOME -> adjustBalance(transaction.accountId, transaction.amount.minor, timestamp)
            TransactionType.EXPENSE -> adjustBalance(transaction.accountId, -transaction.amount.minor, timestamp)
            TransactionType.TRANSFER -> {
                adjustBalance(transaction.accountId, -transaction.amount.minor, timestamp)
                transaction.targetAccountId?.let { adjustBalance(it, transaction.amount.minor, timestamp) }
            }
        }
    }

    private fun revertBalanceEffects(transaction: Transaction, timestamp: Long) {
        when (transaction.type) {
            TransactionType.INCOME -> adjustBalance(transaction.accountId, -transaction.amount.minor, timestamp)
            TransactionType.EXPENSE -> adjustBalance(transaction.accountId, transaction.amount.minor, timestamp)
            TransactionType.TRANSFER -> {
                adjustBalance(transaction.accountId, transaction.amount.minor, timestamp)
                transaction.targetAccountId?.let { adjustBalance(it, -transaction.amount.minor, timestamp) }
            }
        }
    }

    private fun adjustBalance(accountId: String, delta: Long, timestamp: Long) {
        if (delta == 0L) return
        database.accountQueries.updateBalance(delta, timestamp, accountId)
        val balance = database.accountQueries.accountBalance(accountId).executeAsOneOrNull()
            ?: error("关联账户不存在: $accountId")
        database.accountQueries.insertAccountBalanceRecord(
            id = ids.next(),
            account_id = accountId,
            date = dayStart(timestamp),
            balance_minor = balance,
            delta_minor = delta,
            created_at = timestamp,
        )
    }

    private fun dayStart(timestamp: Long): Long = Instant.fromEpochMilliseconds(timestamp)
        .toLocalDateTime(AppTimeZone)
        .date
        .atStartOfDayIn(AppTimeZone)
        .toEpochMilliseconds()
}
