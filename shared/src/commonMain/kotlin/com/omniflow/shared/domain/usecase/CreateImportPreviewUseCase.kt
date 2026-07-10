package com.omniflow.shared.domain.usecase

import com.omniflow.shared.domain.model.ImportPreviewItem
import com.omniflow.shared.domain.model.ImportPreviewState
import com.omniflow.shared.domain.model.ImportDuplicateStatus
import com.omniflow.shared.domain.model.ImportCategoryOrigin
import com.omniflow.shared.domain.model.ImportSessionId
import com.omniflow.shared.domain.model.LedgerId
import com.omniflow.shared.domain.util.UuidGenerator
import com.omniflow.shared.parser.ImportFormat
import com.omniflow.shared.parser.RawTransaction

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
