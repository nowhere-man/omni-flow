package com.omniflow.shared.domain.repository

import com.omniflow.shared.domain.model.Account
import com.omniflow.shared.domain.model.AccountId
import com.omniflow.shared.domain.model.AccountSummary
import com.omniflow.shared.domain.model.Category
import com.omniflow.shared.domain.model.CategoryId
import com.omniflow.shared.domain.model.Ledger
import com.omniflow.shared.domain.model.LedgerId
import com.omniflow.shared.domain.model.Money
import com.omniflow.shared.domain.model.ImportPreviewEdit
import com.omniflow.shared.domain.model.ImportPreviewItem
import com.omniflow.shared.domain.model.ImportSessionId
import com.omniflow.shared.parser.ImportFormat
import com.omniflow.shared.domain.model.Rule
import com.omniflow.shared.domain.model.RuleId
import com.omniflow.shared.domain.model.Reminder
import com.omniflow.shared.domain.model.ReminderId
import com.omniflow.shared.domain.model.Tag
import com.omniflow.shared.domain.model.TagId
import com.omniflow.shared.domain.model.Transaction
import com.omniflow.shared.domain.model.TransactionId

interface LedgerRepository {
    suspend fun activeLedgers(): List<Ledger>
    suspend fun create(ledger: Ledger)
    suspend fun update(ledger: Ledger)
    suspend fun archive(ledgerId: LedgerId)
    suspend fun defaultLedgerId(): LedgerId?
    suspend fun setDefaultLedgerId(ledgerId: LedgerId?)
}

interface AccountRepository {
    suspend fun activeAccounts(): List<Account>
    suspend fun create(account: Account)
    suspend fun update(account: Account)
    suspend fun calibrate(accountId: AccountId, balance: Money)
    suspend fun summary(): AccountSummary
    suspend fun archive(accountId: AccountId)
}

interface TransactionRepository {
    suspend fun activeTransaction(transactionId: TransactionId): Transaction?
    suspend fun create(transaction: Transaction)
    suspend fun createAll(transactions: List<Transaction>) {
        for (transaction in transactions) {
            create(transaction)
        }
    }
    suspend fun update(transaction: Transaction)
    suspend fun archive(transactionId: TransactionId)
}

interface CategoryRepository {
    suspend fun activeCategories(ledgerId: LedgerId): List<Category>
    suspend fun create(category: Category)
    suspend fun update(category: Category)
    suspend fun archive(categoryId: CategoryId)
}

interface TagRepository {
    suspend fun activeTags(ledgerId: LedgerId): List<Tag>
    suspend fun create(tag: Tag)
    suspend fun update(tag: Tag)
    suspend fun archive(tagId: TagId)
}

interface RuleRepository {
    suspend fun activeRules(ledgerId: LedgerId): List<Rule>
    suspend fun create(rule: Rule)
    suspend fun update(rule: Rule)
    suspend fun reorder(ledgerId: LedgerId, orderedIds: List<RuleId>) {
        val active = activeRules(ledgerId).associateBy(Rule::id)
        require(orderedIds.distinct().size == orderedIds.size && orderedIds.toSet() == active.keys) { "规则排序列表不完整" }
        orderedIds.forEachIndexed { priority, id -> update(active.getValue(id).copy(priority = priority)) }
    }
    suspend fun archive(ruleId: RuleId)
}

interface ReminderRepository {
    suspend fun activeReminders(): List<Reminder>
    suspend fun create(reminder: Reminder)
    suspend fun update(reminder: Reminder)
    suspend fun archive(reminderId: ReminderId)
}

interface CategoryMemoryRepository {
    suspend fun categoryId(ledgerId: LedgerId, memoryKey: String): CategoryId?
    suspend fun remember(ledgerId: LedgerId, memoryKey: String, categoryId: CategoryId)
}

interface ImportSessionRepository {
    suspend fun create(
        sessionId: ImportSessionId,
        ledgerId: LedgerId,
        format: ImportFormat,
        items: List<ImportPreviewItem>,
    )
    suspend fun state(sessionId: ImportSessionId): ImportPreviewSession?
    suspend fun updateItem(sessionId: ImportSessionId, edit: ImportPreviewEdit)
    suspend fun updateCategory(sessionId: ImportSessionId, itemId: String, categoryId: CategoryId?)
    suspend fun updateSkipped(sessionId: ImportSessionId, itemId: String, isSkipped: Boolean)
    suspend fun delete(sessionId: ImportSessionId)
}

data class CategoryMemoryEntry(
    val ledgerId: LedgerId,
    val memoryKey: String,
    val categoryId: CategoryId,
)

data class ImportCommitTransaction(
    val transaction: Transaction,
    val tagNames: List<String>,
)

interface ImportCommitRepository {
    suspend fun commit(
        sessionId: ImportSessionId,
        transactions: List<ImportCommitTransaction>,
        categoryMemories: List<CategoryMemoryEntry>,
    )
}

data class ImportPreviewSession(
    val id: ImportSessionId,
    val ledgerId: LedgerId,
    val format: ImportFormat,
    val items: List<ImportPreviewItem>,
)

interface TransactionDedupeRepository {
    suspend fun hasExternalId(source: String, externalId: String): Boolean
    suspend fun likelyDuplicate(
        ledgerId: LedgerId,
        amount: Money,
        occurredAtStart: Long,
        occurredAtEnd: Long,
        note: String?,
    ): Boolean
}
