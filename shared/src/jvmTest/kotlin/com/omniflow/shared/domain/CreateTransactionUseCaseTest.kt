package com.omniflow.shared.domain

import com.omniflow.shared.domain.model.AccountId
import com.omniflow.shared.domain.model.Money
import com.omniflow.shared.domain.model.Transaction
import com.omniflow.shared.domain.model.TransactionType
import com.omniflow.shared.domain.repository.TransactionRepository
import com.omniflow.shared.domain.usecase.CreateTransactionCommand
import com.omniflow.shared.domain.usecase.CreateTransactionUseCase
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CreateTransactionUseCaseTest {
    @Test
    fun createsRmbTransactionWithMinorUnits() = runBlocking {
        val repository = RecordingTransactionRepository()
        val result = CreateTransactionUseCase(repository)(
            CreateTransactionCommand(
                id = "transaction-1",
                ledgerId = "ledger-1",
                accountId = "account-1",
                categoryId = "category-1",
                amount = Money(1234),
                type = TransactionType.EXPENSE,
                occurredAt = Instant.fromEpochMilliseconds(1_000),
                note = " 午餐 ",
                isExcluded = false,
            )
        )

        assertTrue(result.isSuccess)
        assertEquals(Money(1234), repository.transaction?.amount)
        assertEquals("午餐", repository.transaction?.note)
    }

    @Test
    fun rejectsMissingLedgerAndZeroAmount() = runBlocking {
        val repository = RecordingTransactionRepository()
        val useCase = CreateTransactionUseCase(repository)
        val missingLedger = useCase(validCommand(ledgerId = null))
        val zeroAmount = useCase(validCommand(amount = Money.Zero))

        assertFalse(missingLedger.isSuccess)
        assertFalse(zeroAmount.isSuccess)
        assertEquals(null, repository.transaction)
    }

    private fun validCommand(
        ledgerId: String? = "ledger-1",
        amount: Money = Money(1),
    ) = CreateTransactionCommand(
        id = "transaction-1",
        ledgerId = ledgerId,
        accountId = "account-1",
        categoryId = "category-1",
        amount = amount,
        type = TransactionType.EXPENSE,
        occurredAt = Instant.fromEpochMilliseconds(1_000),
        note = null,
        isExcluded = false,
    )

    private class RecordingTransactionRepository : TransactionRepository {
        var transaction: Transaction? = null

        override suspend fun create(transaction: Transaction) {
            this.transaction = transaction
        }
    }
}
