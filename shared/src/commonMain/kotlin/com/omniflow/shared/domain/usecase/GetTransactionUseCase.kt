package com.omniflow.shared.domain.usecase

import com.omniflow.shared.domain.model.Transaction
import com.omniflow.shared.domain.model.TransactionId
import com.omniflow.shared.domain.repository.TransactionRepository

class GetTransactionUseCase(private val transactions: TransactionRepository) {
    suspend operator fun invoke(transactionId: TransactionId): Result<Transaction?> = runCatching {
        transactions.activeTransaction(transactionId)
    }
}
