package com.omniflow.android.ui.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.omniflow.core.SharedApp
import com.omniflow.core.domain.facade.AnalyticsFacade
import com.omniflow.core.domain.facade.ManagementFacade
import com.omniflow.core.domain.facade.AppPreferenceFacade
import com.omniflow.core.domain.model.AnalyticsDashboardState
import com.omniflow.core.domain.model.AnalyticsQuery
import com.omniflow.core.domain.model.AppPreferences
import com.omniflow.core.domain.model.AppTimeZone
import com.omniflow.core.domain.model.DateRange
import com.omniflow.core.domain.model.Ledger
import com.omniflow.core.domain.model.LedgerScope
import com.omniflow.core.domain.model.StatementTable
import com.omniflow.core.domain.model.TimeGranularity
import com.omniflow.core.domain.model.TransactionType
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import java.time.LocalDate as JavaLocalDate

enum class AnalyticsRangeMode { WEEK, MONTH, YEAR, CUSTOM }

data class AnalyticsUiState(
    val dashboard: AnalyticsDashboardState? = null,
    val statementTable: StatementTable? = null,
    val ledgers: List<Ledger> = emptyList(),
    val scope: LedgerScope = LedgerScope.All,
    val rangeMode: AnalyticsRangeMode = AnalyticsRangeMode.MONTH,
    val range: DateRange = analyticsRange(
        AnalyticsRangeMode.MONTH,
        Clock.System.now().toLocalDateTime(AppTimeZone).date,
    ),
    val categoryShareType: TransactionType = TransactionType.EXPENSE,
    val rankingType: TransactionType = TransactionType.EXPENSE,
    val isLoading: Boolean = true,
    val error: String? = null,
)

class AnalyticsViewModel(
    private val analytics: AnalyticsFacade,
    private val management: ManagementFacade,
    private val preferences: AppPreferenceFacade,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    private var anchorDate: LocalDate = Clock.System.now().toLocalDateTime(AppTimeZone).date
    private var observeJob: Job? = null
    private var appPreferences: AppPreferences = AppPreferences()

    init {
        viewModelScope.launch {
            management.observeLedgers().collect { result ->
                val ledgers = result.getOrDefault(emptyList())
                _uiState.value = _uiState.value.copy(ledgers = ledgers)
            }
        }
        viewModelScope.launch {
            preferences.observe().collect { result ->
                result.onSuccess { saved ->
                    val scopeChanged = _uiState.value.scope != saved.analyticsLedgerScope
                    appPreferences = saved
                    if (scopeChanged) {
                        _uiState.value = _uiState.value.copy(scope = saved.analyticsLedgerScope)
                        observeAnalytics()
                    }
                }
            }
        }
        observeAnalytics()
    }

    fun setScope(scope: LedgerScope) {
        _uiState.value = _uiState.value.copy(scope = scope)
        viewModelScope.launch {
            preferences.save(appPreferences.copy(analyticsLedgerScope = scope))
        }
        observeAnalytics()
    }

    fun setRangeMode(mode: AnalyticsRangeMode) {
        val newRange = analyticsRange(mode, anchorDate)
        _uiState.value = _uiState.value.copy(rangeMode = mode, range = newRange)
        observeAnalytics()
    }

    fun shiftRange(delta: Long) {
        anchorDate = when (_uiState.value.rangeMode) {
            AnalyticsRangeMode.WEEK -> anchorDate.minus(-7 * delta.toInt(), DateTimeUnit.DAY)
            AnalyticsRangeMode.MONTH -> javaDate(anchorDate).plusMonths(delta).toKotlinDate()
            AnalyticsRangeMode.YEAR -> javaDate(anchorDate).plusYears(delta).toKotlinDate()
            AnalyticsRangeMode.CUSTOM -> anchorDate
        }
        _uiState.value = _uiState.value.copy(
            range = analyticsRange(_uiState.value.rangeMode, anchorDate),
        )
        observeAnalytics()
    }

    fun resetRange() {
        val mode = _uiState.value.rangeMode
        if (mode == AnalyticsRangeMode.CUSTOM) return
        anchorDate = Clock.System.now().toLocalDateTime(AppTimeZone).date
        _uiState.value = _uiState.value.copy(range = analyticsRange(mode, anchorDate))
        observeAnalytics()
    }

    fun setCustomRange(range: DateRange) {
        _uiState.value = _uiState.value.copy(rangeMode = AnalyticsRangeMode.CUSTOM, range = range)
        observeAnalytics()
    }

    fun setCategoryShareType(type: TransactionType) {
        _uiState.value = _uiState.value.copy(categoryShareType = type)
        observeAnalytics()
    }

    fun setRankingType(type: TransactionType) {
        _uiState.value = _uiState.value.copy(rankingType = type)
        observeAnalytics()
    }

    fun selectMonth(month: Int) {
        val year = _uiState.value.dashboard?.yearStatement?.year
            ?: _uiState.value.range.startInclusive.toLocalDateTime(AppTimeZone).year
        anchorDate = LocalDate(year, month, 1)
        _uiState.value = _uiState.value.copy(
            rangeMode = AnalyticsRangeMode.MONTH,
            range = DateRange.ofMonth(anchorDate, AppTimeZone),
        )
        observeAnalytics()
    }

    fun loadStatementTable(year: Int) {
        viewModelScope.launch {
            analytics.statementTable(_uiState.value.scope, year).onSuccess { table ->
                _uiState.value = _uiState.value.copy(statementTable = table)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(error = error.message)
            }
        }
    }

    fun dismissStatementTable() {
        _uiState.value = _uiState.value.copy(statementTable = null)
    }

    private fun observeAnalytics() {
        observeJob?.cancel()
        val current = _uiState.value
        _uiState.value = current.copy(isLoading = true, error = null)
        observeJob = viewModelScope.launch {
            analytics.observeDashboard(
                AnalyticsQuery(
                    scope = current.scope,
                    range = current.range,
                    rankingType = current.rankingType,
                    categoryShareType = current.categoryShareType,
                    tagAnalysisType = TransactionType.EXPENSE,
                    trendGranularity = analyticsGranularity(current.rangeMode, current.range),
                ),
            ).collect { result ->
                _uiState.value = _uiState.value.copy(
                    dashboard = result.getOrNull(),
                    isLoading = false,
                    error = result.exceptionOrNull()?.message,
                )
            }
        }
    }

    private fun javaDate(date: LocalDate) = JavaLocalDate.of(date.year, date.monthNumber, date.dayOfMonth)
    private fun JavaLocalDate.toKotlinDate() = LocalDate(year, monthValue, dayOfMonth)
}

internal fun analyticsRange(mode: AnalyticsRangeMode, anchor: LocalDate): DateRange = when (mode) {
    AnalyticsRangeMode.WEEK -> DateRange.ofWeek(anchor, AppTimeZone)
    AnalyticsRangeMode.MONTH -> DateRange.ofMonth(anchor, AppTimeZone)
    AnalyticsRangeMode.YEAR -> DateRange.ofYear(anchor.year, AppTimeZone)
    AnalyticsRangeMode.CUSTOM -> DateRange.ofMonth(anchor, AppTimeZone)
}

internal fun analyticsGranularity(mode: AnalyticsRangeMode, range: DateRange): TimeGranularity = when (mode) {
    AnalyticsRangeMode.WEEK, AnalyticsRangeMode.MONTH -> TimeGranularity.DAY
    AnalyticsRangeMode.YEAR -> TimeGranularity.MONTH
    AnalyticsRangeMode.CUSTOM -> {
        if (range.daysCount() <= 90) TimeGranularity.DAY else TimeGranularity.MONTH
    }
}

class AnalyticsViewModelFactory(
    private val sharedApp: SharedApp,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        AnalyticsViewModel(sharedApp.analytics, sharedApp.management, sharedApp.preferences) as T
}
