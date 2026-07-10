package com.omniflow.shared.domain.usecase

import com.omniflow.shared.domain.model.SearchResult
import com.omniflow.shared.domain.model.TransactionSearchQuery

interface SearchTransactionsUseCase {
    suspend operator fun invoke(query: TransactionSearchQuery): Result<SearchResult>
}
