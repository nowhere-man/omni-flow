package com.omniflow.core.domain.usecase

import com.omniflow.core.domain.model.Transaction
import com.omniflow.core.domain.model.TransactionId
import com.omniflow.core.domain.repository.TransactionRepository

class GetTransactionUseCase(private val transactions: TransactionRepository) {
    suspend operator fun invoke(transactionId: TransactionId): Result<Transaction?> = runCatching {
        transactions.activeTransaction(transactionId)
    }
}
