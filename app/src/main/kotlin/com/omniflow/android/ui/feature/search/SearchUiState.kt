package com.omniflow.android.ui.feature.search

import com.omniflow.core.domain.model.Account
import com.omniflow.core.domain.model.Category
import com.omniflow.core.domain.model.Ledger
import com.omniflow.core.domain.model.SearchResult
import com.omniflow.core.domain.model.Tag
import com.omniflow.core.domain.model.TransactionSearchQuery

data class SearchUiState(
    val query: TransactionSearchQuery = TransactionSearchQuery(),
    val result: SearchResult? = null,
    val ledgers: List<Ledger> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val minimumAmountText: String = "",
    val maximumAmountText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)
