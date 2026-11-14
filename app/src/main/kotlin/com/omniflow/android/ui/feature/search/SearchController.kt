package com.omniflow.android.ui.feature.search

import com.omniflow.core.SharedApp
import com.omniflow.core.domain.model.Account
import com.omniflow.core.domain.model.AmountFilter
import com.omniflow.core.domain.model.Category
import com.omniflow.core.domain.model.DateRange
import com.omniflow.core.domain.model.Ledger
import com.omniflow.core.domain.model.LedgerScope
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.TransactionSearchQuery
import com.omniflow.core.domain.model.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SearchController(
    private val scope: CoroutineScope,
    private val sharedApp: SharedApp,
) {
    private val _searchUiState = MutableStateFlow(SearchUiState())
    val searchUiState: StateFlow<SearchUiState> = _searchUiState.asStateFlow()

    private var searchJob: Job? = null
    private var searchCategoriesJob: Job? = null
    private var searchTagsJob: Job? = null

    fun onLedgersUpdated(ledgers: List<Ledger>) {
        _searchUiState.value = _searchUiState.value.copy(ledgers = ledgers)
        observeSearchScope(_searchUiState.value.query.scope, ledgers)
    }

    fun onAccountsUpdated(accounts: List<Account>) {
        _searchUiState.value = _searchUiState.value.copy(accounts = accounts)
    }

    fun setSearchKeyword(keyword: String) = updateDraft(_searchUiState.value.query.copy(keyword = keyword))

    fun setSearchScope(scope: LedgerScope) {
        updateDraft(
            _searchUiState.value.query.copy(
                scope = scope,
                primaryCategoryId = null,
                secondaryCategoryId = null,
                tagId = null,
                tagNames = emptySet(),
            ),
        )
        observeSearchScope(scope, _searchUiState.value.ledgers)
    }

    fun setSearchType(type: TransactionType?) = updateDraft(_searchUiState.value.query.copy(type = type))

    fun setSearchCategory(primary: Category?, secondary: Category?) {
        val currentScope = _searchUiState.value.query.scope
        updateDraft(
            _searchUiState.value.query.copy(
                primaryCategoryText = primary?.name.orEmpty(),
                secondaryCategoryText = secondary?.name.orEmpty(),
                primaryCategoryId = if (currentScope is LedgerScope.Single) primary?.id else null,
                secondaryCategoryId = if (currentScope is LedgerScope.Single) secondary?.id else null,
            ),
        )
    }

    fun toggleSearchTag(tagName: String) {
        val current = _searchUiState.value.query.tagNames
        val updated = if (tagName in current) current - tagName else current + tagName
        updateDraft(
            _searchUiState.value.query.copy(
                tagNames = updated,
                tagText = if (updated.size == 1) updated.first() else "",
            ),
        )
    }

    fun setSearchPrimaryCategoryText(value: String) = updateDraft(
        _searchUiState.value.query.copy(primaryCategoryText = value),
    )

    fun setSearchSecondaryCategoryText(value: String) = updateDraft(
        _searchUiState.value.query.copy(secondaryCategoryText = value),
    )

    fun setSearchTagText(value: String) = updateDraft(_searchUiState.value.query.copy(tagText = value))

    fun setSearchNoteText(value: String) = updateDraft(_searchUiState.value.query.copy(noteText = value))

    fun setSearchAmount(minimumText: String, maximumText: String) {
        _searchUiState.value = _searchUiState.value.copy(
            minimumAmountText = minimumText,
            maximumAmountText = maximumText,
            error = null,
        )
        updateDraft(
            _searchUiState.value.query.copy(
                amount = AmountFilter(
                    minimum = minimumText.toSearchMoneyOrNull(),
                    maximum = maximumText.toSearchMoneyOrNull(),
                ),
            ),
        )
    }

    fun setSearchDateRange(range: DateRange?) = updateDraft(_searchUiState.value.query.copy(dateRange = range))

    fun setSearchAccount(accountId: String?) = updateDraft(_searchUiState.value.query.copy(accountId = accountId))

    fun clearSearch() {
        searchJob?.cancel()
        _searchUiState.value = _searchUiState.value.copy(
            query = TransactionSearchQuery(),
            result = null,
            minimumAmountText = "",
            maximumAmountText = "",
            isLoading = false,
            error = null,
        )
    }

    fun runSearch() {
        val state = _searchUiState.value
        val minimumInvalid = state.minimumAmountText.isNotBlank() && state.minimumAmountText.toSearchMoneyOrNull() == null
        val maximumInvalid = state.maximumAmountText.isNotBlank() && state.maximumAmountText.toSearchMoneyOrNull() == null
        val minimum = state.query.amount.minimum
        val maximum = state.query.amount.maximum
        val reversed = minimum != null && maximum != null && minimum > maximum
        if (minimumInvalid || maximumInvalid || reversed) {
            searchJob?.cancel()
            _searchUiState.value = state.copy(
                result = null,
                isLoading = false,
                error = if (reversed) "最低金额不能大于最高金额" else "金额格式有误，最多输入两位小数",
            )
            return
        }
        runQuery(state.query)
    }

    fun openSearchFor(query: TransactionSearchQuery) {
        _searchUiState.value = _searchUiState.value.copy(
            query = query,
            minimumAmountText = "",
            maximumAmountText = "",
        )
        runQuery(query)
    }

    private fun updateDraft(query: TransactionSearchQuery) {
        _searchUiState.value = _searchUiState.value.copy(query = query)
    }

    private fun runQuery(query: TransactionSearchQuery) {
        searchJob?.cancel()
        if (!query.hasFilters) {
            _searchUiState.value = _searchUiState.value.copy(result = null, isLoading = false, error = null)
            return
        }
        searchJob = scope.launch {
            _searchUiState.value = _searchUiState.value.copy(isLoading = true, error = null)
            sharedApp.search(query).onSuccess { result ->
                _searchUiState.value = _searchUiState.value.copy(result = result, isLoading = false)
            }.onFailure { error ->
                _searchUiState.value = _searchUiState.value.copy(isLoading = false, error = error.message)
            }
        }
    }

    private fun observeSearchScope(scope: LedgerScope, ledgers: List<Ledger>) {
        searchCategoriesJob?.cancel()
        searchTagsJob?.cancel()
        val targetLedgerIds = when (scope) {
            is LedgerScope.Single -> listOf(scope.ledgerId)
            LedgerScope.All -> ledgers.map { it.id }
        }
        if (targetLedgerIds.isEmpty()) {
            _searchUiState.value = _searchUiState.value.copy(categories = emptyList(), tags = emptyList())
            return
        }
        searchCategoriesJob = this.scope.launch {
            combine(targetLedgerIds.map { sharedApp.management.observeCategories(it) }) { results ->
                results.flatMap { it.getOrDefault(emptyList()) }
                    .distinctBy { (it.parentId ?: "") + ":" + it.name + ":" + it.type }
            }.collect { merged ->
                _searchUiState.value = _searchUiState.value.copy(categories = merged)
            }
        }
        searchTagsJob = this.scope.launch {
            combine(targetLedgerIds.map { sharedApp.management.observeTags(it) }) { results ->
                results.flatMap { it.getOrDefault(emptyList()) }.distinctBy { it.name }
            }.collect { merged ->
                _searchUiState.value = _searchUiState.value.copy(tags = merged)
            }
        }
    }

    private fun String.toSearchMoneyOrNull(): Money? {
        val clean = trim()
        if (clean.isBlank()) return null
        return Money.parseOrNull(clean)
    }
}
