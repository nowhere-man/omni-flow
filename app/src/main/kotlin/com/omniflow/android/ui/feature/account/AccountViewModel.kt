package com.omniflow.android.ui.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.omniflow.core.SharedApp
import com.omniflow.core.domain.model.Account
import com.omniflow.core.domain.model.AccountAggregator
import com.omniflow.core.domain.model.AccountGroup
import com.omniflow.core.domain.model.AccountId
import com.omniflow.core.domain.model.AccountSummary
import com.omniflow.core.domain.model.AccountType
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.SearchTransactionItem
import com.omniflow.core.domain.model.TransactionSearchQuery
import com.omniflow.core.domain.model.TransactionSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class AccountListUiState(
    val accounts: List<Account> = emptyList(),
    val groups: List<AccountGroup> = emptyList(),
    val summary: AccountSummary = AccountSummary(Money.Zero, Money.Zero),
    val hideAmount: Boolean = false,
    val error: String? = null,
)

data class AccountDetailUiState(
    val account: Account? = null,
    val transactions: List<SearchTransactionItem> = emptyList(),
    val summary: TransactionSummary = TransactionSummary(Money.Zero, Money.Zero),
    val totalCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
)

class AccountViewModel(
    private val sharedApp: SharedApp,
) : ViewModel() {

    private val _listState = MutableStateFlow(AccountListUiState())
    val listState: StateFlow<AccountListUiState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow(AccountDetailUiState())
    val detailState: StateFlow<AccountDetailUiState> = _detailState.asStateFlow()

    init {
        observeAccounts()
    }

    private fun observeAccounts() {
        viewModelScope.launch {
            sharedApp.management.observeAccounts().collect { result ->
                result.fold(
                    onSuccess = { accounts ->
                        val groups = AccountAggregator.groupByType(accounts)
                        val summary = AccountAggregator.calculateSummary(accounts)
                        _listState.update { it.copy(accounts = accounts, groups = groups, summary = summary, error = null) }
                        // 如果正在查看某个账户的详情，同步刷新账户信息
                        _detailState.value.account?.let { current ->
                            val updated = accounts.find { it.id == current.id }
                            _detailState.update { it.copy(account = updated) }
                        }
                    },
                    onFailure = { err ->
                        _listState.update { it.copy(error = err.message) }
                    },
                )
            }
        }
    }

    fun toggleHideAmount() {
        _listState.update { it.copy(hideAmount = !it.hideAmount) }
    }

    fun loadAccountDetail(accountId: AccountId) {
        viewModelScope.launch {
            _detailState.update { it.copy(isLoading = true, error = null) }
            val account = _listState.value.accounts.find { it.id == accountId }
            val searchResult = sharedApp.search(TransactionSearchQuery(accountId = accountId)).getOrNull()
            _detailState.update {
                it.copy(
                    account = account,
                    transactions = searchResult?.items ?: emptyList(),
                    summary = searchResult?.summary ?: TransactionSummary(Money.Zero, Money.Zero),
                    totalCount = searchResult?.items?.size ?: 0,
                    isLoading = false,
                    error = null,
                )
            }
        }
    }

    fun createAccount(
        name: String,
        type: AccountType,
        iconKey: String,
        cardNumber: String?,
        note: String?,
        balance: Money,
        includeInTotalAssets: Boolean,
    ) {
        viewModelScope.launch {
            val account = Account(
                id = "acc-${Clock.System.now().toEpochMilliseconds()}",
                name = name,
                type = type,
                iconKey = iconKey,
                cardNumber = cardNumber,
                note = note,
                balance = balance,
                includeInTotalAssets = includeInTotalAssets,
            )
            sharedApp.createAccount(account).onFailure { err ->
                _listState.update { it.copy(error = err.message) }
            }
        }
    }

    fun updateAccount(
        id: AccountId,
        name: String,
        type: AccountType,
        iconKey: String,
        cardNumber: String?,
        note: String?,
        balance: Money,
        includeInTotalAssets: Boolean,
    ) {
        viewModelScope.launch {
            val account = Account(
                id = id,
                name = name,
                type = type,
                iconKey = iconKey,
                cardNumber = cardNumber,
                note = note,
                balance = balance,
                includeInTotalAssets = includeInTotalAssets,
            )
            sharedApp.updateAccount(account).onFailure { err ->
                _listState.update { it.copy(error = err.message) }
            }
        }
    }

    fun calibrateBalance(accountId: AccountId, newBalance: Money) {
        viewModelScope.launch {
            sharedApp.calibrateAccount(accountId, newBalance).fold(
                onSuccess = { loadAccountDetail(accountId) },
                onFailure = { err -> _detailState.update { it.copy(error = err.message) } },
            )
        }
    }

    fun deleteAccount(accountId: AccountId, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            sharedApp.deleteAccount(accountId).fold(
                onSuccess = { onSuccess() },
                onFailure = { err -> _listState.update { it.copy(error = err.message) } },
            )
        }
    }
}

class AccountViewModelFactory(
    private val sharedApp: SharedApp,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AccountViewModel(sharedApp) as T
    }
}
