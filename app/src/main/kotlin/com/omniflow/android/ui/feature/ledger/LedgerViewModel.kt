package com.omniflow.android.ui.feature.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.omniflow.core.SharedApp
import com.omniflow.core.domain.model.Ledger
import com.omniflow.core.domain.model.LedgerAggregator
import com.omniflow.core.domain.model.LedgerId
import com.omniflow.core.domain.model.LedgerOverview
import com.omniflow.core.domain.model.LedgerScope
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.SearchTransactionItem
import com.omniflow.core.domain.model.TransactionSearchQuery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class LedgerListUiState(
    val ledgers: List<Ledger> = emptyList(),
    val defaultLedgerId: LedgerId? = null,
    val ledgerStats: Map<LedgerId, LedgerOverview> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class LedgerDetailUiState(
    val ledger: Ledger? = null,
    val isDefault: Boolean = false,
    val overview: LedgerOverview = LedgerOverview(0, Money.Zero, Money.Zero),
    val transactions: List<SearchTransactionItem> = emptyList(),
    val totalCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
)

class LedgerViewModel(
    private val sharedApp: SharedApp,
) : ViewModel() {

    private val _listState = MutableStateFlow(LedgerListUiState())
    val listState: StateFlow<LedgerListUiState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow(LedgerDetailUiState())
    val detailState: StateFlow<LedgerDetailUiState> = _detailState.asStateFlow()

    init {
        observeLedgers()
    }

    private fun observeLedgers() {
        viewModelScope.launch {
            sharedApp.management.observeLedgers().collect { result ->
                result.fold(
                    onSuccess = { ledgers ->
                        _listState.update { it.copy(ledgers = ledgers) }
                        loadLedgerStats(ledgers.map { it.id })
                        _detailState.value.ledger?.let { current ->
                            val updated = ledgers.find { it.id == current.id }
                            _detailState.update { it.copy(ledger = updated) }
                        }
                    },
                    onFailure = { err -> _listState.update { it.copy(error = err.message) } },
                )
            }
        }
        viewModelScope.launch {
            sharedApp.management.observeDefaultLedgerId().collect { result ->
                val defaultId = result.getOrNull()
                _listState.update { it.copy(defaultLedgerId = defaultId) }
                _detailState.update { it.copy(isDefault = it.ledger?.id == defaultId) }
            }
        }
    }

    private fun loadLedgerStats(ledgerIds: List<LedgerId>) {
        viewModelScope.launch {
            val stats = ledgerIds.associateWith { id ->
                val search = sharedApp.search(TransactionSearchQuery(scope = LedgerScope.Single(id))).getOrNull()
                val summary = search?.summary
                val expense = summary?.expenseTotal ?: Money.Zero
                val income = summary?.incomeTotal ?: Money.Zero
                val count = search?.items?.size ?: 0
                LedgerOverview(
                    transactionCount = count,
                    totalIncome = income,
                    totalExpense = expense,
                )
            }
            _listState.update { it.copy(ledgerStats = stats) }
        }
    }

    fun loadLedgerDetail(ledgerId: LedgerId) {
        viewModelScope.launch {
            _detailState.update { it.copy(isLoading = true, error = null) }
            val ledger = _listState.value.ledgers.find { it.id == ledgerId }
            val isDefault = _listState.value.defaultLedgerId == ledgerId
            val search = sharedApp.search(TransactionSearchQuery(scope = LedgerScope.Single(ledgerId))).getOrNull()
            val summary = search?.summary
            val expense = summary?.expenseTotal ?: Money.Zero
            val income = summary?.incomeTotal ?: Money.Zero
            val count = search?.items?.size ?: 0

            _detailState.update {
                it.copy(
                    ledger = ledger,
                    isDefault = isDefault,
                    overview = LedgerOverview(transactionCount = count, totalIncome = income, totalExpense = expense),
                    transactions = search?.items ?: emptyList(),
                    totalCount = count,
                    isLoading = false,
                    error = null,
                )
            }
        }
    }

    fun createLedger(name: String, coverKey: String?) {
        viewModelScope.launch {
            val ledger = Ledger(
                id = "ledger-${Clock.System.now().toEpochMilliseconds()}",
                name = name,
                coverKey = coverKey,
            )
            sharedApp.createLedger(ledger).onFailure { err ->
                _listState.update { it.copy(error = err.message) }
            }
        }
    }

    fun updateLedger(id: LedgerId, name: String, coverKey: String?) {
        viewModelScope.launch {
            val ledger = Ledger(id = id, name = name, coverKey = coverKey)
            sharedApp.updateLedger(ledger).fold(
                onSuccess = { loadLedgerDetail(id) },
                onFailure = { err -> _listState.update { it.copy(error = err.message) } },
            )
        }
    }

    fun setDefaultLedger(id: LedgerId) {
        viewModelScope.launch {
            sharedApp.setDefaultLedger(id).fold(
                onSuccess = {
                    _listState.update { it.copy(defaultLedgerId = id) }
                    if (_detailState.value.ledger?.id == id) {
                        _detailState.update { it.copy(isDefault = true) }
                    }
                },
                onFailure = { err -> _listState.update { it.copy(error = err.message) } },
            )
        }
    }

    fun deleteLedger(id: LedgerId, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            sharedApp.deleteLedger(id).fold(
                onSuccess = { onSuccess() },
                onFailure = { err -> _listState.update { it.copy(error = err.message) } },
            )
        }
    }
}

class LedgerViewModelFactory(
    private val sharedApp: SharedApp,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return LedgerViewModel(sharedApp) as T
    }
}
