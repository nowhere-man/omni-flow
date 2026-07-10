package com.omniflow.shared.domain

import com.omniflow.shared.domain.model.Money
import com.omniflow.shared.domain.model.TransactionType
import com.omniflow.shared.domain.usecase.CreateImportPreviewUseCase
import com.omniflow.shared.parser.ImportFormat
import com.omniflow.shared.parser.RawTransaction
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class CreateImportPreviewUseCaseTest {
    @Test
    fun requiresMissingTypeAndCategoryBeforeCommit() {
        val state = CreateImportPreviewUseCase().fromRaw(
            ImportFormat.ALIPAY,
            listOf(
                RawTransaction(
                    format = ImportFormat.ALIPAY,
                    occurredAt = Instant.fromEpochMilliseconds(1),
                    amount = Money(100),
                    type = null,
                    isExcluded = false,
                    accountName = "余额",
                    note = null,
                    externalId = "order",
                    sourceCategory = null,
                ),
            ),
        )

        assertFalse(state.isReadyToCommit)
        assertEquals(Money.Zero, state.expenseTotal)
        assertEquals(Money.Zero, state.incomeTotal)
    }
}
