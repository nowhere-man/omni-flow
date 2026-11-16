package com.omniflow.android.ui.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.omniflow.core.SharedApp
import com.omniflow.core.domain.model.Account
import com.omniflow.core.domain.model.AccountAggregator
import com.omniflow.core.domain.model.AccountGroup
import com.omniflow.core.domain.model.AccountId
import com.omniflow.core.domain.model.AccountRole
import com.omniflow.core.domain.model.AccountSummary
import com.omniflow.core.domain.model.AccountType
import com.omniflow.core.domain.model.CreditBufferSummary
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
    val filteredAccounts: List<Account> = emptyList(),
    val groups: List<AccountGroup> = emptyList(),
    val summary: AccountSummary = AccountSummary(Money.Zero, Money.Zero),
    val totalCreditLimit: Money = Money.Zero,
    val husbandCreditLimit: Money = Money.Zero,
    val wifeCreditLimit: Money = Money.Zero,
    val familyCreditLimit: Money = Money.Zero,
    val creditBuffer: CreditBufferSummary = CreditBufferSummary.EMPTY,
    val selectedRole: AccountRole? = null,
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
                        recomputeState(accounts = accounts, selectedRole = _listState.value.selectedRole)
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

    private fun recomputeState(accounts: List<Account>, selectedRole: AccountRole?) {
        val filtered = if (selectedRole == null) {
            accounts
        } else {
            accounts.filter { it.ownerRole == selectedRole }
        }

        val groups = AccountAggregator.groupByType(filtered)
        val summary = AccountAggregator.calculateSummary(filtered)
        val creditBuffer = AccountAggregator.calculateCreditBuffer(accounts)

        _listState.update {
            it.copy(
                accounts = accounts,
                filteredAccounts = filtered,
                groups = groups,
                summary = summary,
                totalCreditLimit = creditBuffer.totalLimit,
                husbandCreditLimit = creditBuffer.husband.limit,
                wifeCreditLimit = creditBuffer.wife.limit,
                familyCreditLimit = creditBuffer.family.limit,
                creditBuffer = creditBuffer,
                selectedRole = selectedRole,
                error = null,
            )
        }
    }

    fun selectRole(role: AccountRole?) {
        recomputeState(accounts = _listState.value.accounts, selectedRole = role)
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
        role: AccountRole = AccountRole.HUSBAND,
        creditLimit: Money = Money.Zero,
        availableLimit: Money? = null,
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
                creditLimit = creditLimit,
                availableLimit = availableLimit,
                ownerRole = role,
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
        role: AccountRole = AccountRole.HUSBAND,
        creditLimit: Money = Money.Zero,
        availableLimit: Money? = null,
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
                creditLimit = creditLimit,
                availableLimit = availableLimit,
                ownerRole = role,
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
