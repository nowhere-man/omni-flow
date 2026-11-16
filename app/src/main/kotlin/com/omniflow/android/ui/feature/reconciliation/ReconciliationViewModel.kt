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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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

    private var annualReportJob: Job? = null
    private var periodSnapshotJob: Job? = null
    private var allPeriodsJob: Job? = null

    init {
        val today = Clock.System.now().toLocalDateTime(AppTimeZone).date
        val initialYear = today.year.coerceAtLeast(2026)
        val currentPeriod = "${today.year}-${today.monthNumber.toString().padStart(2, '0')}"
        _state.update {
            it.copy(
                selectedYear = initialYear,
                selectedPeriod = currentPeriod,
            )
        }
        loadAllPeriods()
        loadAnnualReport(initialYear, null)
        loadPeriodSnapshot(currentPeriod, null)
    }

    private fun loadAllPeriods() {
        allPeriodsJob?.cancel()
        allPeriodsJob = viewModelScope.launch {
            sharedApp.reconciliation.observeAllPeriods().collectLatest { periods ->
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

    fun selectYear(year: Int) {
        val newPeriod = "$year-${_state.value.selectedPeriod.takeLast(2)}"
        _state.update { it.copy(selectedYear = year, selectedPeriod = newPeriod) }
        loadAnnualReport(year, _state.value.selectedRole)
        loadPeriodSnapshot(newPeriod, _state.value.selectedRole)
    }

    // A8: Cache previousYear before state update to ensure cross-year report reload triggers correctly
    fun selectPeriod(period: String) {
        val previousYear = _state.value.selectedYear
        val year = period.take(4).toIntOrNull() ?: previousYear
        _state.update { it.copy(selectedPeriod = period, selectedYear = year) }
        loadPeriodSnapshot(period, _state.value.selectedRole)
        if (year != previousYear) {
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

    // A7: Controlled job cancellation + collectLatest instead of unmanaged cascading collects
    private fun loadAnnualReport(year: Int, role: AccountRole?) {
        annualReportJob?.cancel()
        annualReportJob = viewModelScope.launch {
            sharedApp.reconciliation.observeAnnualReport(year, role).collectLatest { result ->
                result.fold(
                    onSuccess = { report -> _state.update { it.copy(annualReport = report, error = null) } },
                    onFailure = { err -> _state.update { it.copy(error = err.message) } },
                )
            }
        }
    }

    // A7: Controlled job cancellation + collectLatest instead of unmanaged cascading collects
    private fun loadPeriodSnapshot(period: String, role: AccountRole?) {
        periodSnapshotJob?.cancel()
        periodSnapshotJob = viewModelScope.launch {
            sharedApp.reconciliation.observePeriodSnapshot(period, role).collectLatest { result ->
                result.fold(
                    onSuccess = { snap -> _state.update { it.copy(periodSnapshot = snap, error = null) } },
                    onFailure = { err -> _state.update { it.copy(error = err.message) } },
                )
            }
        }
    }

    // A3: Use updateMonthlyNotes to prevent clearing income
    fun updateMonthlyNotes(notes: String) {
        viewModelScope.launch {
            sharedApp.reconciliation.updateMonthlyNotes(
                period = _state.value.selectedPeriod,
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
