package com.omniflow.android.ui.feature.reconciliation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.omniflow.core.SharedApp
import com.omniflow.core.domain.model.AccountRole
import com.omniflow.core.domain.model.AppTimeZone
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.PeriodReconciliationSnapshot
import com.omniflow.core.domain.model.ReconciliationAnnualReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.toLocalDateTime

enum class ReconciliationViewMode(val label: String) {
    MONTHLY("月度深度分析"),
    ANNUAL("年度财富台账"),
}

data class ReconciliationUiState(
    val selectedYear: Int = 2026,
    val selectedPeriod: String = "2026-10",
    val selectedRole: AccountRole? = null,
    val viewMode: ReconciliationViewMode = ReconciliationViewMode.MONTHLY,
    val availableYears: List<Int> = listOf(2026, 2025),
    val allPeriods: List<String> = emptyList(),
    val annualReport: ReconciliationAnnualReport? = null,
    val periodSnapshot: PeriodReconciliationSnapshot? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

class ReconciliationViewModel(
    private val sharedApp: SharedApp,
) : ViewModel() {

    private val _state = MutableStateFlow(ReconciliationUiState())
    val state: StateFlow<ReconciliationUiState> = _state.asStateFlow()

    init {
        val today = Clock.System.now().toLocalDateTime(AppTimeZone).date
        val currentPeriod = "${today.year}-${today.monthNumber.toString().padStart(2, '0')}"
        _state.update {
            it.copy(
                selectedYear = today.year.coerceAtLeast(2026),
                selectedPeriod = currentPeriod,
            )
        }
        loadAllPeriods()
        observeData()
    }

    private fun loadAllPeriods() {
        viewModelScope.launch {
            sharedApp.reconciliation.observeAllPeriods().collect { periods ->
                val years = periods.mapNotNull { it.take(4).toIntOrNull() }.distinct().sortedDescending()
                _state.update {
                    it.copy(
                        allPeriods = periods,
                        availableYears = if (years.isNotEmpty()) years else listOf(2026, 2025),
                    )
                }
            }
        }
    }

    private fun observeData() {
        viewModelScope.launch {
            _state.collect { s ->
                loadAnnualReport(s.selectedYear, s.selectedRole)
                loadPeriodSnapshot(s.selectedPeriod, s.selectedRole)
            }
        }
    }

    fun selectYear(year: Int) {
        val newPeriod = "$year-${_state.value.selectedPeriod.takeLast(2)}"
        _state.update { it.copy(selectedYear = year, selectedPeriod = newPeriod) }
        loadAnnualReport(year, _state.value.selectedRole)
        loadPeriodSnapshot(newPeriod, _state.value.selectedRole)
    }

    fun selectPeriod(period: String) {
        val year = period.take(4).toIntOrNull() ?: _state.value.selectedYear
        _state.update { it.copy(selectedPeriod = period, selectedYear = year) }
        loadPeriodSnapshot(period, _state.value.selectedRole)
        if (year != _state.value.selectedYear) {
            loadAnnualReport(year, _state.value.selectedRole)
        }
    }

    fun selectRole(role: AccountRole?) {
        _state.update { it.copy(selectedRole = role) }
        loadAnnualReport(_state.value.selectedYear, role)
        loadPeriodSnapshot(_state.value.selectedPeriod, role)
    }

    fun setViewMode(mode: ReconciliationViewMode) {
        _state.update { it.copy(viewMode = mode) }
    }

    private fun loadAnnualReport(year: Int, role: AccountRole?) {
        viewModelScope.launch {
            sharedApp.reconciliation.observeAnnualReport(year, role).collect { result ->
                result.fold(
                    onSuccess = { report -> _state.update { it.copy(annualReport = report, error = null) } },
                    onFailure = { err -> _state.update { it.copy(error = err.message) } },
                )
            }
        }
    }

    private fun loadPeriodSnapshot(period: String, role: AccountRole?) {
        viewModelScope.launch {
            sharedApp.reconciliation.observePeriodSnapshot(period, role).collect { result ->
                result.fold(
                    onSuccess = { snap -> _state.update { it.copy(periodSnapshot = snap, error = null) } },
                    onFailure = { err -> _state.update { it.copy(error = err.message) } },
                )
            }
        }
    }

    fun updateMonthlyNotes(notes: String) {
        viewModelScope.launch {
            val snap = _state.value.periodSnapshot
            val husbandIncome = snap?.meta?.husbandIncome ?: Money.Zero
            val wifeIncome = snap?.meta?.wifeIncome ?: Money.Zero
            sharedApp.reconciliation.updateMonthlyMeta(
                period = _state.value.selectedPeriod,
                husbandIncome = husbandIncome,
                wifeIncome = wifeIncome,
                notes = notes,
            )
            loadPeriodSnapshot(_state.value.selectedPeriod, _state.value.selectedRole)
            loadAnnualReport(_state.value.selectedYear, _state.value.selectedRole)
        }
    }

    fun updateMonthlyIncome(husbandIncome: Money, wifeIncome: Money) {
        viewModelScope.launch {
            val snap = _state.value.periodSnapshot
            val notes = snap?.meta?.notes ?: ""
            sharedApp.reconciliation.updateMonthlyMeta(
                period = _state.value.selectedPeriod,
                husbandIncome = husbandIncome,
                wifeIncome = wifeIncome,
                notes = notes,
            )
            loadPeriodSnapshot(_state.value.selectedPeriod, _state.value.selectedRole)
            loadAnnualReport(_state.value.selectedYear, _state.value.selectedRole)
        }
    }
}

class ReconciliationViewModelFactory(
    private val sharedApp: SharedApp,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ReconciliationViewModel(sharedApp) as T
    }
}
