package com.omniflow.core.domain.usecase

import com.omniflow.core.domain.model.ImportPreviewItem
import com.omniflow.core.domain.model.ImportPreviewState
import com.omniflow.core.domain.model.ImportDuplicateStatus
import com.omniflow.core.domain.model.ImportCategoryOrigin
import com.omniflow.core.domain.model.ImportSessionId
import com.omniflow.core.domain.model.LedgerId
import com.omniflow.core.domain.util.UuidGenerator
import com.omniflow.core.parser.ImportFormat
import com.omniflow.core.parser.RawTransaction

class CreateImportPreviewUseCase(
    private val ids: UuidGenerator = UuidGenerator(),
) {
    fun fromRaw(
        sessionId: ImportSessionId,
        ledgerId: LedgerId,
        format: ImportFormat,
        transactions: List<RawTransaction>,
    ): ImportPreviewState =
        ImportPreviewState(
            sessionId = sessionId,
            ledgerId = ledgerId,
            format = format,
            items = transactions.map { transaction ->
                ImportPreviewItem(
                    id = ids.next(),
                    raw = transaction,
                    type = transaction.type,
                    categoryId = null,
                    categoryOrigin = ImportCategoryOrigin.NONE,
                    accountId = null,
                    note = transaction.note,
                    tags = transaction.tags,
                    isExcluded = transaction.isExcluded,
                    isSkipped = false,
                    duplicateStatus = ImportDuplicateStatus.NONE,
                )
            },
        )

}
