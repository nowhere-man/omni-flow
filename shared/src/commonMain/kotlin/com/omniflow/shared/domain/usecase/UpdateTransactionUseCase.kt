package com.omniflow.shared.domain.usecase

import com.omniflow.shared.domain.model.Transaction
import com.omniflow.shared.domain.repository.TransactionRepository

class UpdateTransactionUseCase(
    private val transactions: TransactionRepository,
) {
    suspend operator fun invoke(transaction: Transaction): Result<Unit> = runCatching {
        require(transaction.amount.minor > 0) { "金额必须大于零" }
        transactions.update(
            transaction.copy(
                note = transaction.note?.trim()?.takeIf(String::isNotEmpty),
                externalId = transaction.externalId?.trim()?.takeIf(String::isNotEmpty),
            )
        )
    }
}
