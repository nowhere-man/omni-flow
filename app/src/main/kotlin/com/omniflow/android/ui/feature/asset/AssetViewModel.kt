package com.omniflow.android.ui.feature.asset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.omniflow.core.SharedApp
import com.omniflow.core.domain.model.Account
import com.omniflow.core.domain.model.AccountAggregator
import com.omniflow.core.domain.model.AccountId
import com.omniflow.core.domain.model.AccountRole
import com.omniflow.core.domain.model.AccountSummary
import com.omniflow.core.domain.model.AccountType
import com.omniflow.core.domain.model.AppTimeZone
import com.omniflow.core.domain.model.Money
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.toLocalDateTime

data class AssetBarSegment(
    val key: String,
    val weight: Float,
    val label: String,
    val isLiability: Boolean,
)

data class AssetUiState(
    val accounts: List<Account> = emptyList(),
    val activeAccounts: List<Account> = emptyList(),
    val creditCards: List<Account> = emptyList(),
    val savingsAndWallets: List<Account> = emptyList(),
    val investments: List<Account> = emptyList(),
    val summary: AccountSummary = AccountSummary(Money.Zero, Money.Zero),
    val totalCreditLimit: Money = Money.Zero,
    val totalDebt: Money = Money.Zero,
    val segments: List<AssetBarSegment> = emptyList(),
    val selectedRole: AccountRole? = null,
    val selectedSegmentKey: String? = null,
    val editingAccount: Account? = null,
    val showEditNotesDialog: Boolean = false,
    val monthlyNotes: String = "",
    val currentPeriod: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)

class AssetViewModel(
    private val sharedApp: SharedApp,
) : ViewModel() {

    private val _state = MutableStateFlow(AssetUiState())
    val state: StateFlow<AssetUiState> = _state.asStateFlow()

    private var accountsJob: Job? = null
    private var notesJob: Job? = null

    init {
        val today = Clock.System.now().toLocalDateTime(AppTimeZone).date
        val currentPeriod = "${today.year}-${today.monthNumber.toString().padStart(2, '0')}"
        _state.update { it.copy(currentPeriod = currentPeriod) }
        observeAccounts()
        observeMonthlyNotes(currentPeriod)
    }

    private fun observeAccounts() {
        accountsJob?.cancel()
        accountsJob = viewModelScope.launch {
            sharedApp.management.observeAccounts().collectLatest { result ->
                result.fold(
                    onSuccess = { accounts ->
                        recomputeState(accounts = accounts, role = _state.value.selectedRole)
                    },
                    onFailure = { err ->
                        _state.update { it.copy(error = err.message) }
                    },
                )
            }
        }
    }

    private fun observeMonthlyNotes(period: String) {
        notesJob?.cancel()
        notesJob = viewModelScope.launch {
            sharedApp.reconciliation.observePeriodSnapshot(period, null).collectLatest { result ->
                val meta = result.getOrNull()?.meta
                if (meta != null && meta.notes.isNotBlank()) {
                    _state.update { it.copy(monthlyNotes = meta.notes) }
                }
            }
        }
    }

    private fun recomputeState(accounts: List<Account>, role: AccountRole?) {
        val activeAccounts = accounts.filter {
            it.deletedAt == null && it.includeInTotalAssets && (role == null || it.ownerRole == role)
        }

        // A6: Unified summary calculation via domain AccountAggregator
        val summary = AccountAggregator.calculateSummary(activeAccounts)

        val creditCards = activeAccounts.filter { it.type == AccountType.CREDIT_CARD }
        val savingsAndWallets = activeAccounts.filter {
            it.type == AccountType.DEBIT_CARD || it.type == AccountType.E_WALLET || it.type == AccountType.CASH
        }
        val investments = activeAccounts.filter { it.type == AccountType.INVESTMENT }

        val totalCreditLimit = Money(creditCards.sumOf { it.creditLimit.minor })
        val totalDebt = Money(activeAccounts.sumOf { it.debt.minor })

        // Segment weights calculation
        val assetAggregates = AccountType.entries.mapNotNull { type ->
            val matching = activeAccounts.filter { it.type == type && it.balance.minor > 0 && it.type != AccountType.CREDIT_CARD }
            if (matching.isEmpty()) null
            else {
                val totalMinor = matching.fold(0L) { acc, a -> acc + a.balance.minor }
                type to Money(totalMinor)
            }
        }

        val totalVol = summary.assets.minor + totalDebt.minor
        val segments = if (totalVol <= 0L) {
            emptyList()
        } else {
            val list = mutableListOf<AssetBarSegment>()
            assetAggregates.forEach { (type, amount) ->
                list.add(
                    AssetBarSegment(
                        key = type.name,
                        weight = amount.minor.toFloat(),
                        label = when (type) {
                            AccountType.CASH -> "现金"
                            AccountType.DEBIT_CARD -> "储蓄卡"
                            AccountType.CREDIT_CARD -> "信用卡"
                            AccountType.E_WALLET -> "电子钱包"
                            AccountType.INVESTMENT -> "理财投资"
                        },
                        isLiability = false,
                    ),
                )
            }
            if (totalDebt.minor > 0L) {
                list.add(
                    AssetBarSegment(
                        key = "LIABILITY",
                        weight = totalDebt.minor.toFloat(),
                        label = "欠款负债",
                        isLiability = true,
                    ),
                )
            }
            list
        }

        _state.update {
            it.copy(
                accounts = accounts,
                activeAccounts = activeAccounts,
                creditCards = creditCards,
                savingsAndWallets = savingsAndWallets,
                investments = investments,
                summary = summary,
                totalCreditLimit = totalCreditLimit,
                totalDebt = totalDebt,
                segments = segments,
                selectedRole = role,
                error = null,
            )
        }
    }

    fun selectRole(role: AccountRole?) {
        recomputeState(accounts = _state.value.accounts, role = role)
    }

    fun selectSegment(key: String?) {
        _state.update {
            val next = if (it.selectedSegmentKey == key) null else key
            it.copy(selectedSegmentKey = next)
        }
    }

    fun setEditingAccount(account: Account?) {
        _state.update { it.copy(editingAccount = account) }
    }

    fun setShowEditNotesDialog(show: Boolean) {
        _state.update { it.copy(showEditNotesDialog = show) }
    }

    fun quickUpdateAccount(
        accountId: AccountId,
        balance: Money,
        availableLimit: Money?,
        profitLoss: Money,
    ) {
        viewModelScope.launch {
            sharedApp.reconciliation.recordAccountUpdate(
                accountId = accountId,
                period = _state.value.currentPeriod,
                balance = balance,
                availableLimit = availableLimit,
                profitLoss = profitLoss,
            )
            setEditingAccount(null)
        }
    }

    // A3: Atomically update monthly notes via dedicated facade method
    fun updateMonthlyNotes(notes: String) {
        viewModelScope.launch {
            _state.update { it.copy(monthlyNotes = notes, showEditNotesDialog = false) }
            sharedApp.reconciliation.updateMonthlyNotes(
                period = _state.value.currentPeriod,
                notes = notes,
            )
        }
    }
}

class AssetViewModelFactory(
    private val sharedApp: SharedApp,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AssetViewModel(sharedApp) as T
    }
}
