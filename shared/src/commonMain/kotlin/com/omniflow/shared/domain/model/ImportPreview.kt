package com.omniflow.shared.domain.model

import com.omniflow.shared.parser.ImportFormat
import com.omniflow.shared.parser.RawTransaction

typealias ImportSessionId = String
typealias ImportPreviewItemId = String

enum class ImportDuplicateStatus { NONE, CONFIRMED, SUSPECTED }

enum class ImportCategoryOrigin { NONE, RULE, MEMORY, USER }

data class ImportRequest(
    val ledgerId: LedgerId,
    val fileName: String,
    val bytes: ByteArray,
    val selectedFormat: ImportFormat? = null,
)

data class ImportPreviewItem(
    val id: ImportPreviewItemId,
    val raw: RawTransaction,
    val type: TransactionType?,
    val categoryId: CategoryId?,
    val categoryOrigin: ImportCategoryOrigin,
    val accountId: AccountId?,
    val note: String?,
    val tags: List<String>,
    val isExcluded: Boolean,
    val isSkipped: Boolean,
    val duplicateStatus: ImportDuplicateStatus,
) {
    val requiresTypeSelection: Boolean get() = !isSkipped && type == null
    val requiresCategorySelection: Boolean get() = !isSkipped && categoryId == null
}

data class ImportPreviewState(
    val sessionId: ImportSessionId,
    val ledgerId: LedgerId,
    val format: ImportFormat,
    val items: List<ImportPreviewItem>,
) {
    val importableItems: List<ImportPreviewItem> get() = items.filterNot(ImportPreviewItem::isSkipped)
    val expenseTotal: Money get() = importableItems
        .filter { it.type == TransactionType.EXPENSE }
        .fold(Money.Zero) { total, item -> total + item.raw.amount }
    val incomeTotal: Money get() = importableItems
        .filter { it.type == TransactionType.INCOME }
        .fold(Money.Zero) { total, item -> total + item.raw.amount }
    val isReadyToCommit: Boolean get() = importableItems.none {
        it.requiresTypeSelection || it.requiresCategorySelection || it.accountId == null
    }
}

data class ImportPreviewEdit(
    val sessionId: ImportSessionId,
    val itemId: ImportPreviewItemId,
    val type: TransactionType?,
    val categoryId: CategoryId?,
    val accountId: AccountId?,
    val note: String?,
    val tags: List<String>,
    val isExcluded: Boolean,
    val isSkipped: Boolean,
)

data class ImportCategoryBatchEdit(
    val itemIds: Set<ImportPreviewItemId>,
    val categoryId: CategoryId?,
)

data class ImportExcludeBatchEdit(
    val itemIds: Set<ImportPreviewItemId>,
    val isSkipped: Boolean,
)

data class ImportCommitResult(
    val importedCount: Int,
    val excludedCount: Int,
)
