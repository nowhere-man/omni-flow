package com.omniflow.core.data.repository

import com.omniflow.core.db.OmniFlowDatabase
import com.omniflow.core.domain.model.AppTimeZone
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.Transaction
import com.omniflow.core.domain.model.TransactionId
import com.omniflow.core.domain.model.TransactionSource
import com.omniflow.core.domain.model.TransactionType
import com.omniflow.core.domain.repository.CategoryMemoryEntry
import com.omniflow.core.domain.repository.ImportCommitRepository
import com.omniflow.core.domain.repository.ImportCommitTransaction
import com.omniflow.core.domain.repository.TransactionRepository
import com.omniflow.core.domain.util.UuidGenerator
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

class SqlDelightTransactionRepository(
    private val database: OmniFlowDatabase,
    private val ids: UuidGenerator = UuidGenerator(),
    private val now: () -> Instant = { Clock.System.now() },
) : TransactionRepository, ImportCommitRepository {
    override suspend fun activeTransaction(transactionId: TransactionId): Transaction? =
        findActiveTransaction(transactionId)

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

    override suspend fun create(transaction: Transaction) {
        val timestamp = now().toEpochMilliseconds()
        database.transaction {
            createInTransaction(transaction, timestamp)
        }
    }

    override suspend fun createAll(transactions: List<Transaction>) {
        if (transactions.isEmpty()) return
        val timestamp = now().toEpochMilliseconds()
        database.transaction {
            transactions.forEach { transaction -> createInTransaction(transaction, timestamp) }
        }
    }

    override suspend fun commit(
        sessionId: String,
        transactions: List<ImportCommitTransaction>,
        categoryMemories: List<CategoryMemoryEntry>,
    ) {
        val timestamp = now().toEpochMilliseconds()
        database.transaction {
            transactions.forEach { entry ->
                val tagIds = entry.tagNames
                    .map(String::trim)
                    .filter(String::isNotEmpty)
                    .distinct()
                    .map { name ->
                        database.tagQueries.activeTagIdByName(entry.transaction.ledgerId, name)
                            .executeAsOneOrNull()
                            ?: ids.next().also { tagId ->
                                database.tagQueries.insertTag(
                                    id = tagId,
                                    ledger_id = entry.transaction.ledgerId,
                                    name = name,
                                    created_at = timestamp,
                                    updated_at = timestamp,
                                )
                            }
                    }
                    .toSet()
                createInTransaction(entry.transaction.copy(tagIds = tagIds), timestamp)
            }
            categoryMemories.forEach { memory ->
                database.categoryMemoryQueries.upsertCategoryMemory(
                    ledger_id = memory.ledgerId,
                    memory_key = memory.memoryKey,
                    category_id = memory.categoryId,
                    updated_at = timestamp,
                )
            }
            database.importSessionQueries.deleteImportPreviewItems(sessionId)
            database.importSessionQueries.deleteImportSession(sessionId)
        }
    }

    override suspend fun update(transaction: Transaction) {
        val timestamp = now().toEpochMilliseconds()
        database.transaction {
            val existing = findActiveTransaction(transaction.id) ?: error("交易不存在或已删除")
            val resolvedCategoryId = if (transaction.type == TransactionType.TRANSFER && transaction.categoryId.isBlank()) {
                findOrCreateTransferCategory(transaction.ledgerId, timestamp)
            } else {
                transaction.categoryId
            }
            val toUpdate = if (transaction.type == TransactionType.TRANSFER) {
                transaction.copy(categoryId = resolvedCategoryId, isExcluded = true)
            } else {
                transaction.copy(categoryId = resolvedCategoryId)
            }
            validateUpdatedTransaction(existing, toUpdate)
            database.transactionQueries.updateTransaction(
                ledger_id = toUpdate.ledgerId,
                account_id = toUpdate.accountId,
                category_id = toUpdate.categoryId,
                amount_minor = toUpdate.amount.minor,
                type = toUpdate.type.name,
                occurred_at = toUpdate.occurredAt.toEpochMilliseconds(),
                note = toUpdate.note,
                is_excluded = if (toUpdate.isExcluded) 1L else 0L,
                external_source = toUpdate.source?.name,
                external_id = toUpdate.externalId,
                updated_at = timestamp,
                target_account_id = toUpdate.targetAccountId,
                id = toUpdate.id,
            )
            replaceTags(toUpdate.id, toUpdate.tagIds)
            revertBalanceEffects(existing, timestamp)
            applyBalanceEffects(toUpdate, timestamp)
        }
    }

    override suspend fun archive(transactionId: TransactionId) {
        val timestamp = now().toEpochMilliseconds()
        database.transaction {
            val existing = findActiveTransaction(transactionId) ?: error("交易不存在或已删除")
            revertBalanceEffects(existing, timestamp)
            database.transactionQueries.archiveTransaction(timestamp, transactionId)
        }
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

    private fun validateNewTransaction(transaction: Transaction) {
        require(transaction.amount.minor > 0) { "金额必须大于零" }
        require(database.ledgerQueries.activeLedgerId(transaction.ledgerId).executeAsOneOrNull() != null) {
            "账本不存在或已删除"
        }
        require(database.accountQueries.activeAccountId(transaction.accountId).executeAsOneOrNull() != null) {
            "账户不存在或已删除"
        }
        if (transaction.type == TransactionType.TRANSFER) {
            require(transaction.targetAccountId != null) { "转账必须指定转入账户" }
            require(transaction.targetAccountId != transaction.accountId) { "转出账户与转入账户不能相同" }
            require(database.accountQueries.activeAccountId(transaction.targetAccountId).executeAsOneOrNull() != null) {
                "转入账户不存在或已删除"
            }
        } else {
            require(database.categoryQueries.activeCategoryTypeForLedger(
                id = transaction.categoryId,
                ledger_id = transaction.ledgerId,
            ).executeAsOneOrNull() == transaction.type.name) {
                "分类不存在、已删除或与交易类型不一致"
            }
        }
        transaction.tagIds.forEach { tagId ->
            require(database.tagQueries.activeTagIdForLedger(tagId, transaction.ledgerId).executeAsOneOrNull() != null) {
                "标签不存在、已删除或不属于当前账本"
            }
        }
    }

    private fun createInTransaction(transaction: Transaction, timestamp: Long) {
        val resolvedCategoryId = if (transaction.type == TransactionType.TRANSFER && transaction.categoryId.isBlank()) {
            findOrCreateTransferCategory(transaction.ledgerId, timestamp)
        } else {
            transaction.categoryId
        }
        val toInsert = if (transaction.type == TransactionType.TRANSFER) {
            transaction.copy(categoryId = resolvedCategoryId, isExcluded = true)
        } else {
            transaction.copy(categoryId = resolvedCategoryId)
        }
        validateNewTransaction(toInsert)
        database.transactionQueries.insertTransaction(
            id = toInsert.id,
            ledger_id = toInsert.ledgerId,
            account_id = toInsert.accountId,
            category_id = toInsert.categoryId,
            amount_minor = toInsert.amount.minor,
            type = toInsert.type.name,
            occurred_at = toInsert.occurredAt.toEpochMilliseconds(),
            note = toInsert.note,
            is_excluded = if (toInsert.isExcluded) 1L else 0L,
            external_source = toInsert.source?.name,
            external_id = toInsert.externalId,
            created_at = timestamp,
            updated_at = timestamp,
            target_account_id = toInsert.targetAccountId,
        )
        replaceTags(toInsert.id, toInsert.tagIds)
        applyBalanceEffects(toInsert, timestamp)
    }

    private fun validateUpdatedTransaction(existing: Transaction, updated: Transaction) {
        require(updated.amount.minor > 0) { "金额必须大于零" }
        require(database.ledgerQueries.activeLedgerId(updated.ledgerId).executeAsOneOrNull() != null) {
            "账本不存在或已删除"
        }
        if (updated.accountId != existing.accountId) {
            require(database.accountQueries.activeAccountId(updated.accountId).executeAsOneOrNull() != null) {
                "账户不存在或已删除"
            }
        }
        if (updated.type == TransactionType.TRANSFER) {
            require(updated.targetAccountId != null) { "转账必须指定转入账户" }
            require(updated.targetAccountId != updated.accountId) { "转出账户与转入账户不能相同" }
            require(database.accountQueries.activeAccountId(updated.targetAccountId).executeAsOneOrNull() != null) {
                "转入账户不存在或已删除"
            }
        } else {
            if (updated.ledgerId != existing.ledgerId ||
                updated.categoryId != existing.categoryId ||
                updated.type != existing.type
            ) {
                require(database.categoryQueries.activeCategoryTypeForLedger(
                    id = updated.categoryId,
                    ledger_id = updated.ledgerId,
                ).executeAsOneOrNull() == updated.type.name) {
                    "分类不存在、已删除或与交易类型不一致"
                }
            }
        }

        val tagsToValidate = if (updated.ledgerId == existing.ledgerId) {
            updated.tagIds - existing.tagIds
        } else {
            updated.tagIds
        }
        tagsToValidate.forEach { tagId ->
            require(database.tagQueries.activeTagIdForLedger(tagId, updated.ledgerId).executeAsOneOrNull() != null) {
                "标签不存在、已删除或不属于当前账本"
            }
        }
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
            ?: error("关联账户不存在")
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
