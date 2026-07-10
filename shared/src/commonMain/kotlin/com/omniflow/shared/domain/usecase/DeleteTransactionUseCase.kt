package com.omniflow.shared.domain.usecase

import com.omniflow.shared.domain.model.TransactionId
import com.omniflow.shared.domain.repository.TransactionRepository

class DeleteTransactionUseCase(
    private val transactions: TransactionRepository,
) {
    suspend operator fun invoke(transactionId: TransactionId): Result<Unit> = runCatching {
        transactions.archive(transactionId)
    }
}
