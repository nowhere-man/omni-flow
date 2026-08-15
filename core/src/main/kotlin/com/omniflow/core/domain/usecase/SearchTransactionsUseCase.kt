package com.omniflow.core.domain.usecase

import com.omniflow.core.domain.model.SearchResult
import com.omniflow.core.domain.model.TransactionSearchQuery

interface SearchTransactionsUseCase {
    suspend operator fun invoke(query: TransactionSearchQuery): Result<SearchResult>
}
