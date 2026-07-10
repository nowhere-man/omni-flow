package com.omniflow.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.omniflow.shared.SharedApp
import com.omniflow.shared.domain.model.CalendarTransactionFilter
import com.omniflow.shared.domain.model.DateRange
import com.omniflow.shared.domain.model.HomeQuery
import com.omniflow.shared.domain.model.HomeState
import com.omniflow.shared.domain.model.Ledger
import com.omniflow.shared.domain.model.LedgerScope
import com.omniflow.shared.domain.model.TransactionDetailQuery
import com.omniflow.shared.domain.model.TransactionDetailState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import java.time.LocalDate as JavaLocalDate

data class HomeUiState(
    val home: HomeState? = null,
    val ledgers: List<Ledger> = emptyList(),
    val isLedgerMenuExpanded: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedDate: TransactionDetailState? = null,
    val isDateLoading: Boolean = false,
    val dateError: String? = null,
    val displayMode: TransactionDisplayMode = TransactionDisplayMode.LIST,
    val calendarFilter: CalendarTransactionFilter = CalendarTransactionFilter.ALL,
)

enum class TransactionDisplayMode { LIST, CARD }

class OmniFlowViewModel(
    private val sharedApp: SharedApp,
) : ViewModel() {
    private val _homeUiState = MutableStateFlow(HomeUiState())
    val homeUiState: StateFlow<HomeUiState> = _homeUiState.asStateFlow()

    private var query = HomeQuery(
        scope = LedgerScope.All,
        month = monthRange(Clock.System.now()),
    )
    private var homeJob: Job? = null
    private var detailJob: Job? = null

    init {
        viewModelScope.launch { sharedApp.initialize() }
        viewModelScope.launch {
            sharedApp.management.observeLedgers().collect { result ->
                _homeUiState.value = _homeUiState.value.copy(ledgers = result.getOrDefault(emptyList()))
            }
        }
        observeHome()
    }

    fun previousMonth() = updateMonth(-1)

    fun nextMonth() = updateMonth(1)

    fun setCalendarFilter(filter: CalendarTransactionFilter) {
        query = query.copy(calendarFilter = filter)
        _homeUiState.value = _homeUiState.value.copy(calendarFilter = filter)
        observeHome()
    }

    fun toggleLedgerMenu() {
        _homeUiState.value = _homeUiState.value.copy(
            isLedgerMenuExpanded = !_homeUiState.value.isLedgerMenuExpanded,
        )
    }

    fun selectLedger(scope: LedgerScope) {
        query = query.copy(scope = scope)
        _homeUiState.value = _homeUiState.value.copy(isLedgerMenuExpanded = false)
        observeHome()
    }

    fun showDate(date: LocalDate) {
        detailJob?.cancel()
        _homeUiState.value = _homeUiState.value.copy(
            selectedDate = null,
            isDateLoading = true,
            dateError = null,
        )
        detailJob = viewModelScope.launch {
            sharedApp.home.observeTransactionDetails(
                TransactionDetailQuery(query.scope, dateRange(date)),
            ).collect { result ->
                _homeUiState.value = _homeUiState.value.copy(
                    selectedDate = result.getOrNull(),
                    isDateLoading = false,
                    dateError = result.exceptionOrNull()?.message,
                )
            }
        }
    }

    fun dismissDate() {
        detailJob?.cancel()
        _homeUiState.value = _homeUiState.value.copy(selectedDate = null, isDateLoading = false, dateError = null)
    }

    fun toggleDisplayMode() {
        _homeUiState.value = _homeUiState.value.copy(
            displayMode = when (_homeUiState.value.displayMode) {
                TransactionDisplayMode.LIST -> TransactionDisplayMode.CARD
                TransactionDisplayMode.CARD -> TransactionDisplayMode.LIST
            },
        )
    }

    private fun observeHome() {
        homeJob?.cancel()
        _homeUiState.value = _homeUiState.value.copy(isLoading = true, error = null)
        homeJob = viewModelScope.launch {
            sharedApp.home.observeHome(query).collect { result ->
                _homeUiState.value = _homeUiState.value.copy(
                    home = result.getOrNull(),
                    isLoading = false,
                    error = result.exceptionOrNull()?.message,
                )
            }
        }
    }

    private fun updateMonth(delta: Long) {
        val start = query.month.startInclusive.toLocalDateTime(ChinaTimeZone).date
        val month = JavaLocalDate.of(start.year, start.monthNumber, 1).plusMonths(delta)
        query = query.copy(month = monthRange(LocalDate(month.year, month.monthValue, 1)))
        observeHome()
    }
}

class OmniFlowViewModelFactory(
    private val sharedApp: SharedApp,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = OmniFlowViewModel(sharedApp) as T
}

private val ChinaTimeZone = TimeZone.of("Asia/Shanghai")

private fun monthRange(now: Instant): DateRange = monthRange(now.toLocalDateTime(ChinaTimeZone).date)

private fun monthRange(date: LocalDate): DateRange {
    val next = JavaLocalDate.of(date.year, date.monthNumber, 1).plusMonths(1)
    return DateRange(
        startInclusive = LocalDate(date.year, date.monthNumber, 1).atStartOfDayIn(ChinaTimeZone),
        endExclusive = LocalDate(next.year, next.monthValue, 1).atStartOfDayIn(ChinaTimeZone),
    )
}

private fun dateRange(date: LocalDate): DateRange {
    val next = JavaLocalDate.of(date.year, date.monthNumber, date.dayOfMonth).plusDays(1)
    return DateRange(
        startInclusive = date.atStartOfDayIn(ChinaTimeZone),
        endExclusive = LocalDate(next.year, next.monthValue, next.dayOfMonth).atStartOfDayIn(ChinaTimeZone),
    )
}
