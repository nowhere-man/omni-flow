package com.omniflow.core.domain.model

import com.omniflow.core.parser.ImportFormat
import com.omniflow.core.parser.RawTransaction

typealias ImportSessionId = String
typealias ImportPreviewItemId = String

enum class ImportDuplicateStatus { NONE, CONFIRMED, SUSPECTED }

enum class ImportCategoryOrigin { NONE, RULE, MEMORY, AI, USER }

enum class ImportPreviewPhase { DETECTING, PARSING, ENRICHING, SUGGESTING, READY }

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
    val format: ImportFormat?,
    val items: List<ImportPreviewItem>,
    val phase: ImportPreviewPhase = ImportPreviewPhase.READY,
    val progress: Float = 1f,
    /**
     * AI 建议失败或被截断时的提示。只活在本次预览的内存态里、不落库：
     * 它是一次性通知，重新 observe 会话时不该再冒出来。
     */
    val suggestionError: String? = null,
) {
    val aiSuggestedCount: Int get() = items.count {
        !it.isSkipped && it.categoryOrigin == ImportCategoryOrigin.AI
    }
    val importableItems: List<ImportPreviewItem> get() = items.filterNot(ImportPreviewItem::isSkipped)
    // 不计收支的条目不进收支汇总，和首页统计口径保持一致。
    val expenseTotal: Money get() = importableItems.countableTotal(TransactionType.EXPENSE)
    val incomeTotal: Money get() = importableItems.countableTotal(TransactionType.INCOME)
    val pendingCount: Int get() = importableItems.count {
        it.requiresTypeSelection || it.requiresCategorySelection
    }
    val skippedCount: Int get() = items.count(ImportPreviewItem::isSkipped)
    val isReadyToCommit: Boolean get() = phase == ImportPreviewPhase.READY && importableItems.none {
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
