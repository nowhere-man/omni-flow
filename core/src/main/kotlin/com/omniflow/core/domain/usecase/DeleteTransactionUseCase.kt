package com.omniflow.core.domain.usecase

import com.omniflow.core.domain.model.TransactionId
import com.omniflow.core.domain.repository.TransactionRepository

class DeleteTransactionUseCase(
    private val transactions: TransactionRepository,
) {
    suspend operator fun invoke(transactionId: TransactionId): Result<Unit> = runCatching {
        transactions.archive(transactionId)
    }
}
