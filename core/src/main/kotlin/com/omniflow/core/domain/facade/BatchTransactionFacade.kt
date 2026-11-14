package com.omniflow.core.domain.facade

import com.omniflow.core.domain.model.AccountId
import com.omniflow.core.domain.model.BatchOperationResult
import com.omniflow.core.domain.model.CategoryId
import com.omniflow.core.domain.model.LedgerId
import com.omniflow.core.domain.model.TransactionId

interface BatchTransactionFacade {
    suspend fun delete(transactionIds: List<TransactionId>): Result<BatchOperationResult>
    suspend fun updateAccount(transactionIds: List<TransactionId>, targetAccountId: AccountId): Result<BatchOperationResult>
    suspend fun updateCategory(transactionIds: List<TransactionId>, targetCategoryId: CategoryId): Result<BatchOperationResult>
    suspend fun updateLedger(transactionIds: List<TransactionId>, targetLedgerId: LedgerId): Result<BatchOperationResult>
}
