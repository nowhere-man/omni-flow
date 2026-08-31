package com.omniflow.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.omniflow.core.SharedApp
import com.omniflow.core.domain.model.Account
import com.omniflow.core.domain.model.AccountSummary
import com.omniflow.core.domain.model.AccountType
import com.omniflow.core.domain.model.AnalyticsDashboardState
import com.omniflow.core.domain.model.AnalyticsQuery
import com.omniflow.core.domain.model.AppPreferences
import com.omniflow.core.domain.model.AppearanceMode
import com.omniflow.core.domain.model.BudgetProgress
import com.omniflow.core.domain.model.buildBudgetProgress
import com.omniflow.core.domain.model.Category
import com.omniflow.core.domain.model.DateRange
import com.omniflow.core.domain.model.HomeQuery
import com.omniflow.core.domain.model.HomeState
import com.omniflow.core.domain.model.Ledger
import com.omniflow.core.domain.model.LedgerScope
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.ImportCategoryBatchEdit
import com.omniflow.core.domain.model.ImportExcludeBatchEdit
import com.omniflow.core.domain.model.ImportGroupMode
import com.omniflow.core.domain.model.ImportPreviewEdit
import com.omniflow.core.domain.model.ImportPreviewState
import com.omniflow.core.domain.model.ImportRequest
import com.omniflow.core.domain.model.QingziExportRequest
import com.omniflow.core.domain.model.RemoteBackupMeta
import com.omniflow.core.domain.model.Reminder
import com.omniflow.core.domain.model.ReminderSchedule
import com.omniflow.core.domain.model.ReminderScheduleKind
import com.omniflow.core.domain.model.ReminderType
import com.omniflow.core.domain.model.Rule
import com.omniflow.core.domain.model.RuleActionType
import com.omniflow.core.domain.model.RuleConditionType
import com.omniflow.core.domain.model.SearchResult
import com.omniflow.core.domain.model.StatementTable
import com.omniflow.core.domain.model.SyncConfig
import com.omniflow.core.domain.model.SyncState
import com.omniflow.core.domain.model.SyncTarget
import com.omniflow.core.domain.model.ThemeColor
import com.omniflow.core.domain.model.TimeGranularity
import com.omniflow.core.domain.model.Tag
import com.omniflow.core.domain.model.Transaction
import com.omniflow.core.domain.model.TransactionDetailDisplayMode
import com.omniflow.core.domain.model.TransactionDetailQuery
import com.omniflow.core.domain.model.TransactionDetailState
import com.omniflow.core.domain.model.TransactionListItem
import com.omniflow.core.domain.model.TransactionSearchQuery
import com.omniflow.core.domain.model.TransactionSummary
import com.omniflow.core.domain.model.TransactionType
import com.omniflow.core.domain.model.occurredDateBounds
import com.omniflow.core.domain.usecase.CreateTransactionCommand
import com.omniflow.core.parser.ImportFormat
import java.time.LocalDate as JavaLocalDate
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

data class HomeUiState(
    val home: HomeState? = null,
    val ledgers: List<Ledger> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val displayMode: TransactionDetailDisplayMode = TransactionDetailDisplayMode.CARD,
    val appearanceMode: AppearanceMode = AppearanceMode.SYSTEM,
)

data class RangeDetailUiState(
    val detail: TransactionDetailState? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

enum class AnalyticsRangeMode { WEEK, MONTH, YEAR, CUSTOM }

data class AnalyticsUiState(
    val dashboard: AnalyticsDashboardState? = null,
    val statementTable: StatementTable? = null,
    val ledgers: List<Ledger> = emptyList(),
    val scope: LedgerScope = LedgerScope.All,
    val rangeMode: AnalyticsRangeMode = AnalyticsRangeMode.MONTH,
    val range: DateRange = analyticsRange(
        AnalyticsRangeMode.MONTH,
        Clock.System.now().toLocalDateTime(ChinaTimeZone).date,
    ),
    // 排行榜/饼图/标签以前各有一个开关，切换收入要点三次；合成页面级一个
    val analyticsType: TransactionType = TransactionType.EXPENSE,
    val isLoading: Boolean = true,
    val error: String? = null,
)

data class SearchUiState(
    val query: TransactionSearchQuery = TransactionSearchQuery(),
    val result: SearchResult? = null,
    val ledgers: List<Ledger> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val minimumAmountText: String = "",
    val maximumAmountText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class TransactionRecordDetailUiState(
    val transaction: Transaction? = null,
    val ledgerName: String = "",
    val accountName: String = "",
    val primaryCategoryName: String = "",
    val secondaryCategoryName: String? = null,
    val categoryIconKey: String? = null,
    val tagNames: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val error: String? = null,
) {
    val categoryDisplayName: String get() = secondaryCategoryName?.let { "$primaryCategoryName - $it" } ?: primaryCategoryName
}

data class TransactionEditorUiState(
    val editingId: String? = null,
    val ledgers: List<Ledger> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val ledgerId: String? = null,
    val accountId: String? = null,
    val categoryId: String? = null,
    val selectedTagIds: Set<String> = emptySet(),
    val type: TransactionType = TransactionType.EXPENSE,
    val occurredAt: Instant = Clock.System.now(),
    val amountInput: String = "",
    val note: String = "",
    val isExcluded: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val completed: Boolean = false,
)

/** 账本卡上的「N 条账目 / 支出 / 收入 / 结余」。 */
data class LedgerStat(val count: Int, val expense: Money, val income: Money) {
    val net: Money get() = income - expense
}

enum class EntityDetailKind { LEDGER, ACCOUNT, CATEGORY }

data class EntityDetailUiState(
    val kind: EntityDetailKind = EntityDetailKind.LEDGER,
    val entityId: String = "",
    val summary: TransactionSummary = TransactionSummary(Money.Zero, Money.Zero),
    val count: Int = 0,
    val items: List<TransactionListItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

data class MoreUiState(
    val accountSummary: AccountSummary = AccountSummary(Money.Zero, Money.Zero),
    val ledgers: List<Ledger> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val defaultLedgerId: String? = null,
    val selectedLedgerId: String? = null,
    val categories: List<Category> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val rules: List<Rule> = emptyList(),
    val reminders: List<Reminder> = emptyList(),
    val budgets: List<BudgetProgress> = emptyList(),
    val ledgerStats: Map<String, LedgerStat> = emptyMap(),
    val preferences: AppPreferences = AppPreferences(),
    val syncState: SyncState = SyncState(),
    val backups: List<RemoteBackupMeta> = emptyList(),
    val importPreview: ImportPreviewState? = null,
    val importGroupMode: ImportGroupMode = ImportGroupMode.SOURCE,
    val importFilter: ImportFilter = ImportFilter.IMPORTABLE,
    val importDateFilter: ImportDateFilterState? = null,
    val importFileName: String? = null,
    val importFormat: ImportFormat? = null,
    val importMessage: String? = null,
    val exportPayload: String? = null,
    val exportWarnings: List<String> = emptyList(),
    val exportPreviewCount: Int? = null,
    val isLoadingExportPreview: Boolean = false,
    val isImporting: Boolean = false,
    val isExporting: Boolean = false,
    val isLoadingBackups: Boolean = false,
    val error: String? = null,
)

class OmniFlowViewModel(
    private val sharedApp: SharedApp,
) : ViewModel() {
    private val _homeUiState = MutableStateFlow(HomeUiState())
    val homeUiState: StateFlow<HomeUiState> = _homeUiState.asStateFlow()
    private val _analyticsUiState = MutableStateFlow(AnalyticsUiState())
    val analyticsUiState: StateFlow<AnalyticsUiState> = _analyticsUiState.asStateFlow()
    private val _rangeDetailUiState = MutableStateFlow(RangeDetailUiState())
    val rangeDetailUiState: StateFlow<RangeDetailUiState> = _rangeDetailUiState.asStateFlow()
    private val _searchUiState = MutableStateFlow(SearchUiState())
    val searchUiState: StateFlow<SearchUiState> = _searchUiState.asStateFlow()
    private val _transactionRecordDetailUiState = MutableStateFlow(TransactionRecordDetailUiState())
    val transactionRecordDetailUiState: StateFlow<TransactionRecordDetailUiState> =
        _transactionRecordDetailUiState.asStateFlow()
    private val _transactionUiState = MutableStateFlow(TransactionEditorUiState())
    val transactionUiState: StateFlow<TransactionEditorUiState> = _transactionUiState.asStateFlow()
    private val _entityDetailUiState = MutableStateFlow(EntityDetailUiState())
    val entityDetailUiState: StateFlow<EntityDetailUiState> = _entityDetailUiState.asStateFlow()
    private val _moreUiState = MutableStateFlow(MoreUiState())
    val moreUiState: StateFlow<MoreUiState> = _moreUiState.asStateFlow()

    private var homeQuery = HomeQuery(LedgerScope.All, monthRange(Clock.System.now()))
    private var analyticsAnchor = Clock.System.now().toLocalDateTime(ChinaTimeZone).date
    private var preferences = AppPreferences()
    private var defaultLedgerId: String? = null
    private var editingTransaction: Transaction? = null
    private var homeJob: Job? = null
    private var detailJob: Job? = null
    private var analyticsJob: Job? = null
    private var searchJob: Job? = null
    private var editorCategoriesJob: Job? = null
    private var editorTagsJob: Job? = null
    private var moreCategoriesJob: Job? = null
    private var moreTagsJob: Job? = null
    private var moreRulesJob: Job? = null
    private var moreBudgetsJob: Job? = null
    private var importJob: Job? = null
    private var exportPreviewJob: Job? = null
    private var entityDetailJob: Job? = null
    private var ledgerStatsJob: Job? = null

    init {
        viewModelScope.launch { sharedApp.initialize() }
        collectManagement()
        collectPreferences()
        collectSync()
        observeHome()
        observeAnalytics()
    }

    private fun collectManagement() {
        viewModelScope.launch {
            sharedApp.management.observeLedgers().collect { result ->
                val ledgers = result.getOrDefault(emptyList())
                _homeUiState.value = _homeUiState.value.copy(ledgers = ledgers)
                _analyticsUiState.value = _analyticsUiState.value.copy(ledgers = ledgers)
                _searchUiState.value = _searchUiState.value.copy(ledgers = ledgers)
                _transactionUiState.value = _transactionUiState.value.copy(ledgers = ledgers)
                val previousSelection = _moreUiState.value.selectedLedgerId
                val selected = previousSelection?.takeIf { id -> ledgers.any { it.id == id } }
                    ?: ledgers.firstOrNull()?.id
                _moreUiState.value = _moreUiState.value.copy(ledgers = ledgers, selectedLedgerId = selected)
                loadLedgerStats(ledgers.map(Ledger::id))
                if (selected != null && selected != previousSelection) observeMoreLedger(selected)
            }
        }
        viewModelScope.launch {
            sharedApp.management.observeDefaultLedgerId().collect { result ->
                defaultLedgerId = result.getOrNull()
                _moreUiState.value = _moreUiState.value.copy(defaultLedgerId = defaultLedgerId)
            }
        }
        viewModelScope.launch {
            sharedApp.management.observeAccounts().collect { result ->
                val accounts = result.getOrDefault(emptyList())
                _searchUiState.value = _searchUiState.value.copy(accounts = accounts)
                _transactionUiState.value = _transactionUiState.value.copy(accounts = accounts)
                _moreUiState.value = _moreUiState.value.copy(accounts = accounts)
            }
        }
        viewModelScope.launch {
            sharedApp.management.observeAccountSummary().collect { result ->
                result.onSuccess { summary ->
                    _moreUiState.value = _moreUiState.value.copy(accountSummary = summary)
                }
            }
        }
        viewModelScope.launch {
            sharedApp.reminders.observe().collect { result ->
                _moreUiState.value = _moreUiState.value.copy(reminders = result.getOrDefault(emptyList()))
            }
        }
    }

    private fun loadLedgerStats(ledgerIds: List<String>) {
        ledgerStatsJob?.cancel()
        ledgerStatsJob = viewModelScope.launch {
            val stats = ledgerIds.associateWith { id ->
                val result = sharedApp.search(TransactionSearchQuery(scope = LedgerScope.Single(id))).getOrNull()
                LedgerStat(
                    count = result?.items?.size ?: 0,
                    expense = result?.summary?.expenseTotal ?: Money.Zero,
                    income = result?.summary?.incomeTotal ?: Money.Zero,
                )
            }
            _moreUiState.value = _moreUiState.value.copy(ledgerStats = stats)
        }
    }

    private fun collectPreferences() {
        viewModelScope.launch {
            sharedApp.preferences.observe().collect { result ->
                result.onSuccess { saved ->
                    val homeScopeChanged = homeQuery.scope != saved.homeLedgerScope
                    val analyticsScopeChanged = _analyticsUiState.value.scope != saved.analyticsLedgerScope
                    preferences = saved
                    homeQuery = homeQuery.copy(scope = saved.homeLedgerScope)
                    _homeUiState.value = _homeUiState.value.copy(
                        displayMode = saved.transactionDetailDisplayMode,
                        appearanceMode = saved.appearanceMode,
                    )
                    _analyticsUiState.value = _analyticsUiState.value.copy(scope = saved.analyticsLedgerScope)
                    _moreUiState.value = _moreUiState.value.copy(preferences = saved)
                    if (homeScopeChanged) observeHome()
                    if (analyticsScopeChanged) observeAnalytics()
                }
            }
        }
    }

    private fun collectSync() {
        viewModelScope.launch {
            sharedApp.sync.observeSyncState().collect { state ->
                _moreUiState.value = _moreUiState.value.copy(syncState = state)
            }
        }
    }

    fun previousMonth() = updateHomeMonth(-1)
    fun nextMonth() = updateHomeMonth(1)
    fun selectHomeMonth(date: LocalDate) {
        homeQuery = homeQuery.copy(month = monthRange(date))
        observeHome()
    }
    fun selectLedger(scope: LedgerScope) {
        homeQuery = homeQuery.copy(scope = scope)
        observeHome()
        savePreferences(preferences.copy(homeLedgerScope = scope))
    }
    fun showDate(date: LocalDate) {
        showTransactionDetails(dateRange(date), homeQuery.scope, null)
    }
    fun dismissDate() {
        detailJob?.cancel()
        _rangeDetailUiState.value = RangeDetailUiState()
    }
    fun showHomeSummary(type: TransactionType?) = showTransactionDetails(homeQuery.month, homeQuery.scope, type)
    fun toggleDisplayMode() {
        val mode = when (_homeUiState.value.displayMode) {
            TransactionDetailDisplayMode.LIST -> TransactionDetailDisplayMode.CARD
            TransactionDetailDisplayMode.CARD -> TransactionDetailDisplayMode.LIST
        }
        _homeUiState.value = _homeUiState.value.copy(displayMode = mode)
        savePreferences(preferences.copy(transactionDetailDisplayMode = mode))
    }

    private fun showTransactionDetails(range: DateRange, scope: LedgerScope, type: TransactionType?) {
        detailJob?.cancel()
        _rangeDetailUiState.value = RangeDetailUiState(isLoading = true)
        detailJob = viewModelScope.launch {
            sharedApp.home.observeTransactionDetails(TransactionDetailQuery(scope, range, type)).collect { result ->
                _rangeDetailUiState.value = RangeDetailUiState(
                    detail = result.getOrNull(),
                    isLoading = false,
                    error = result.exceptionOrNull()?.message,
                )
            }
        }
    }

    private fun observeHome() {
        homeJob?.cancel()
        _homeUiState.value = _homeUiState.value.copy(isLoading = true, error = null)
        homeJob = viewModelScope.launch {
            sharedApp.home.observeHome(homeQuery).collect { result ->
                _homeUiState.value = _homeUiState.value.copy(
                    home = result.getOrNull(),
                    isLoading = false,
                    error = result.exceptionOrNull()?.message,
                )
            }
        }
    }

    private fun updateHomeMonth(delta: Long) {
        val start = homeQuery.month.startInclusive.toLocalDateTime(ChinaTimeZone).date
        val month = JavaLocalDate.of(start.year, start.monthNumber, 1).plusMonths(delta)
        homeQuery = homeQuery.copy(month = monthRange(LocalDate(month.year, month.monthValue, 1)))
        observeHome()
    }

    fun setAnalyticsScope(scope: LedgerScope) {
        _analyticsUiState.value = _analyticsUiState.value.copy(scope = scope)
        savePreferences(preferences.copy(analyticsLedgerScope = scope))
        observeAnalytics()
    }
    fun setAnalyticsRangeMode(mode: AnalyticsRangeMode) {
        _analyticsUiState.value = _analyticsUiState.value.copy(rangeMode = mode, range = analyticsRange(mode, analyticsAnchor))
        observeAnalytics()
    }
    fun shiftAnalyticsRange(delta: Long) {
        analyticsAnchor = when (_analyticsUiState.value.rangeMode) {
            AnalyticsRangeMode.WEEK -> analyticsAnchor.minus(-7 * delta.toInt(), DateTimeUnit.DAY)
            AnalyticsRangeMode.MONTH -> javaDate(analyticsAnchor).plusMonths(delta).toKotlinDate()
            AnalyticsRangeMode.YEAR -> javaDate(analyticsAnchor).plusYears(delta).toKotlinDate()
            AnalyticsRangeMode.CUSTOM -> analyticsAnchor
        }
        _analyticsUiState.value = _analyticsUiState.value.copy(
            range = analyticsRange(_analyticsUiState.value.rangeMode, analyticsAnchor),
        )
        observeAnalytics()
    }
    fun resetAnalyticsRange() {
        val mode = _analyticsUiState.value.rangeMode
        if (mode == AnalyticsRangeMode.CUSTOM) return
        analyticsAnchor = Clock.System.now().toLocalDateTime(ChinaTimeZone).date
        _analyticsUiState.value = _analyticsUiState.value.copy(range = analyticsRange(mode, analyticsAnchor))
        observeAnalytics()
    }
    fun setAnalyticsCustomRange(range: DateRange) {
        _analyticsUiState.value = _analyticsUiState.value.copy(rangeMode = AnalyticsRangeMode.CUSTOM, range = range)
        observeAnalytics()
    }
    fun setAnalyticsType(type: TransactionType) {
        _analyticsUiState.value = _analyticsUiState.value.copy(analyticsType = type)
        observeAnalytics()
    }
    fun selectAnalyticsMonth(month: Int) {
        val year = _analyticsUiState.value.dashboard?.yearStatement?.year
            ?: _analyticsUiState.value.range.startInclusive.toLocalDateTime(ChinaTimeZone).year
        analyticsAnchor = LocalDate(year, month, 1)
        _analyticsUiState.value = _analyticsUiState.value.copy(
            rangeMode = AnalyticsRangeMode.MONTH,
            range = monthRange(analyticsAnchor),
        )
        observeAnalytics()
    }
    fun loadStatementTable(year: Int) {
        viewModelScope.launch {
            sharedApp.analytics.statementTable(_analyticsUiState.value.scope, year).onSuccess { table ->
                _analyticsUiState.value = _analyticsUiState.value.copy(statementTable = table)
            }.onFailure { error ->
                _analyticsUiState.value = _analyticsUiState.value.copy(error = error.message)
            }
        }
    }
    fun dismissStatementTable() {
        _analyticsUiState.value = _analyticsUiState.value.copy(statementTable = null)
    }

    private fun observeAnalytics() {
        analyticsJob?.cancel()
        val current = _analyticsUiState.value
        _analyticsUiState.value = current.copy(isLoading = true, error = null)
        analyticsJob = viewModelScope.launch {
            sharedApp.analytics.observeDashboard(
                AnalyticsQuery(
                    scope = current.scope,
                    range = current.range,
                    rankingType = current.analyticsType,
                    categoryShareType = current.analyticsType,
                    tagAnalysisType = current.analyticsType,
                    trendGranularity = analyticsGranularity(current.rangeMode, current.range),
                ),
            ).collect { result ->
                _analyticsUiState.value = _analyticsUiState.value.copy(
                    dashboard = result.getOrNull(),
                    isLoading = false,
                    error = result.exceptionOrNull()?.message,
                )
            }
        }
    }

    // 搜索条件只改草稿，不触发查询：边输边搜会在输到一半时不停刷新结果列表，
    // 也让「同时看几个条件的组合」变得没法操作。真正的查询交给 runSearch()。
    fun setSearchKeyword(keyword: String) = updateDraft(_searchUiState.value.query.copy(keyword = keyword))
    fun setSearchScope(scope: LedgerScope) = updateDraft(
        _searchUiState.value.query.copy(
            scope = scope,
            primaryCategoryId = null,
            secondaryCategoryId = null,
            tagId = null,
        ),
    )
    fun setSearchType(type: TransactionType?) = updateDraft(_searchUiState.value.query.copy(type = type))
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
                amount = com.omniflow.core.domain.model.AmountFilter(
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

    /** 点「搜索」才真正查库；金额格式也在这时候校验，边输边报错太吵。 */
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

    private fun updateDraft(query: TransactionSearchQuery) {
        _searchUiState.value = _searchUiState.value.copy(query = query)
    }

    private fun runQuery(query: TransactionSearchQuery) {
        searchJob?.cancel()
        if (!query.hasFilters) {
            _searchUiState.value = _searchUiState.value.copy(result = null, isLoading = false, error = null)
            return
        }
        searchJob = viewModelScope.launch {
            _searchUiState.value = _searchUiState.value.copy(isLoading = true, error = null)
            sharedApp.search(query).onSuccess { result ->
                _searchUiState.value = _searchUiState.value.copy(result = result, isLoading = false)
            }.onFailure { error ->
                _searchUiState.value = _searchUiState.value.copy(isLoading = false, error = error.message)
            }
        }
    }

    /**
     * 账本 / 账户 / 分类详情。全部走已有的 search 用例，core 不需要新增查询能力。
     */
    fun showEntityDetail(kind: EntityDetailKind, id: String) {
        entityDetailJob?.cancel()
        val query = when (kind) {
            EntityDetailKind.LEDGER -> TransactionSearchQuery(scope = LedgerScope.Single(id))
            EntityDetailKind.ACCOUNT -> TransactionSearchQuery(accountId = id)
            EntityDetailKind.CATEGORY -> TransactionSearchQuery(primaryCategoryId = id)
        }
        _entityDetailUiState.value = EntityDetailUiState(kind = kind, entityId = id, isLoading = true)
        entityDetailJob = viewModelScope.launch {
            sharedApp.search(query).onSuccess { result ->
                _entityDetailUiState.value = _entityDetailUiState.value.copy(
                    summary = result.summary,
                    count = result.items.size,
                    items = result.items.map { it.transaction },
                    isLoading = false,
                )
            }.onFailure { error ->
                _entityDetailUiState.value = _entityDetailUiState.value.copy(isLoading = false, error = error.message)
            }
        }
    }

    /** 详情页「查看全部」：把筛选条件推到搜索页再跳过去，这条路径要直接出结果。 */
    fun openSearchFor(kind: EntityDetailKind, id: String) {
        val query = when (kind) {
            EntityDetailKind.LEDGER -> TransactionSearchQuery(scope = LedgerScope.Single(id))
            EntityDetailKind.ACCOUNT -> TransactionSearchQuery(accountId = id)
            EntityDetailKind.CATEGORY -> TransactionSearchQuery(primaryCategoryId = id)
        }
        _searchUiState.value = _searchUiState.value.copy(
            query = query,
            minimumAmountText = "",
            maximumAmountText = "",
        )
        runQuery(query)
    }

    fun showTransactionRecordDetail(transactionId: String) {
        _transactionRecordDetailUiState.value = TransactionRecordDetailUiState(isLoading = true)
        viewModelScope.launch {
            val result = sharedApp.getTransactionRecordDetail(transactionId)
            val detail = result.getOrNull()
            if (detail == null) {
                _transactionRecordDetailUiState.value = TransactionRecordDetailUiState(
                    error = result.exceptionOrNull()?.message ?: "交易不存在或已删除",
                )
                return@launch
            }
            _transactionRecordDetailUiState.value = TransactionRecordDetailUiState(
                transaction = detail.transaction,
                ledgerName = detail.ledgerName,
                accountName = detail.accountName,
                primaryCategoryName = detail.primaryCategoryName,
                secondaryCategoryName = detail.secondaryCategoryName,
                categoryIconKey = detail.categoryIconKey,
                tagNames = detail.tagNames,
            )
        }
    }

    fun dismissTransactionRecordDetail() {
        _transactionRecordDetailUiState.value = TransactionRecordDetailUiState()
    }

    fun deleteTransactionRecordDetail(onDeleted: () -> Unit = {}) {
        val transaction = _transactionRecordDetailUiState.value.transaction ?: return
        _transactionRecordDetailUiState.value = _transactionRecordDetailUiState.value.copy(isDeleting = true, error = null)
        viewModelScope.launch {
            sharedApp.deleteTransaction(transaction.id).onSuccess {
                dismissTransactionRecordDetail()
                onDeleted()
            }.onFailure { error ->
                _transactionRecordDetailUiState.value = _transactionRecordDetailUiState.value.copy(
                    isDeleting = false,
                    error = error.message,
                )
            }
        }
    }

    fun startNewTransaction(date: LocalDate? = null, ledgerId: String? = null) {
        editingTransaction = null
        val accounts = _transactionUiState.value.accounts
        val selectedLedger = ledgerId ?: defaultLedgerId
        val now = Clock.System.now().toLocalDateTime(ChinaTimeZone)
        _transactionUiState.value = TransactionEditorUiState(
            ledgers = _transactionUiState.value.ledgers,
            accounts = accounts,
            ledgerId = selectedLedger,
            accountId = accounts.firstOrNull { it.type == AccountType.CASH }?.id ?: accounts.firstOrNull()?.id,
            occurredAt = LocalDateTime(
                date ?: now.date,
                LocalTime(now.hour, now.minute),
            ).toInstant(ChinaTimeZone),
        )
        observeEditorLedger(selectedLedger)
    }
    fun editTransaction(transactionId: String) {
        viewModelScope.launch {
            sharedApp.getTransaction(transactionId).onSuccess { transaction ->
                if (transaction == null) {
                    _transactionUiState.value = _transactionUiState.value.copy(error = "交易不存在或已删除")
                    return@onSuccess
                }
                editingTransaction = transaction
                _transactionUiState.value = TransactionEditorUiState(
                    editingId = transaction.id,
                    ledgers = _transactionUiState.value.ledgers,
                    accounts = _transactionUiState.value.accounts,
                    ledgerId = transaction.ledgerId,
                    accountId = transaction.accountId,
                    categoryId = transaction.categoryId,
                    selectedTagIds = transaction.tagIds,
                    type = transaction.type,
                    occurredAt = transaction.occurredAt,
                    amountInput = transaction.amount.toInputString(),
                    note = transaction.note.orEmpty(),
                    isExcluded = transaction.isExcluded,
                )
                observeEditorLedger(transaction.ledgerId)
            }.onFailure { error ->
                _transactionUiState.value = _transactionUiState.value.copy(error = error.message)
            }
        }
    }
    fun setTransactionType(type: TransactionType) {
        val category = _transactionUiState.value.categories.firstOrNull { it.id == _transactionUiState.value.categoryId }
        _transactionUiState.value = _transactionUiState.value.copy(
            type = type,
            categoryId = category?.takeIf { it.type == type }?.id,
        )
    }
    fun setTransactionLedger(ledgerId: String?) {
        _transactionUiState.value = _transactionUiState.value.copy(ledgerId = ledgerId, categoryId = null, selectedTagIds = emptySet())
        observeEditorLedger(ledgerId)
    }
    fun setTransactionAccount(accountId: String?) {
        _transactionUiState.value = _transactionUiState.value.copy(accountId = accountId)
    }
    fun setTransactionCategory(categoryId: String?) {
        _transactionUiState.value = _transactionUiState.value.copy(categoryId = categoryId)
    }
    fun reorderTransactionPrimaryCategories(categoryIds: List<String>) {
        val state = _transactionUiState.value
        val ledgerId = state.ledgerId ?: return
        viewModelScope.launch {
            sharedApp.reorderPrimaryCategories(ledgerId, state.type, categoryIds)
                .onFailure { error -> _transactionUiState.value = state.copy(error = error.message) }
        }
    }
    fun createTransactionSecondaryCategory(name: String) {
        val state = _transactionUiState.value
        val ledgerId = state.ledgerId ?: return
        val selected = state.categories.firstOrNull { it.id == state.categoryId } ?: return
        val parentId = selected.parentId ?: selected.id
        val id = UUID.randomUUID().toString()
        viewModelScope.launch {
            sharedApp.createCategory(Category(id, ledgerId, parentId, name, null, state.type))
                .onSuccess { _transactionUiState.value = _transactionUiState.value.copy(categoryId = id) }
                .onFailure { error -> _transactionUiState.value = state.copy(error = error.message) }
        }
    }
    fun toggleTransactionTag(tagId: String) {
        val tags = _transactionUiState.value.selectedTagIds.toMutableSet()
        if (!tags.add(tagId)) tags.remove(tagId)
        _transactionUiState.value = _transactionUiState.value.copy(selectedTagIds = tags)
    }
    fun setTransactionNote(note: String) {
        _transactionUiState.value = _transactionUiState.value.copy(note = note)
    }
    fun setTransactionDate(instant: Instant) {
        _transactionUiState.value = _transactionUiState.value.copy(occurredAt = instant)
    }
    fun setTransactionExcluded(excluded: Boolean) {
        _transactionUiState.value = _transactionUiState.value.copy(isExcluded = excluded)
    }
    fun pressAmountKey(key: String) {
        val current = _transactionUiState.value.amountInput
        val updated = when (key) {
            "退格" -> current.dropLast(1)
            "+", "-" -> if (current.isNotEmpty() && current.last() !in "+-") current + key else current
            "." -> if (current.substringAfterLast('+').substringAfterLast('-').contains('.')) current else current + key
            else -> current + key
        }
        _transactionUiState.value = _transactionUiState.value.copy(amountInput = updated, error = null)
    }
    fun saveTransaction(saveAgain: Boolean) {
        val state = _transactionUiState.value
        val amount = evaluateAmount(state.amountInput)?.let(::Money)
        if (amount == null) {
            _transactionUiState.value = state.copy(error = "请输入有效金额")
            return
        }
        _transactionUiState.value = state.copy(isSaving = true, error = null)
        viewModelScope.launch {
            val existing = editingTransaction
            val result = if (existing == null) {
                sharedApp.createTransaction(
                    CreateTransactionCommand(
                        id = UUID.randomUUID().toString(),
                        ledgerId = state.ledgerId,
                        accountId = state.accountId,
                        categoryId = state.categoryId,
                        amount = amount,
                        type = state.type,
                        occurredAt = state.occurredAt,
                        note = state.note,
                        isExcluded = state.isExcluded,
                        tagIds = state.selectedTagIds,
                    ),
                )
            } else {
                sharedApp.updateTransaction(
                    existing.copy(
                        ledgerId = state.ledgerId ?: existing.ledgerId,
                        accountId = state.accountId ?: existing.accountId,
                        categoryId = state.categoryId ?: existing.categoryId,
                        amount = amount,
                        type = state.type,
                        occurredAt = state.occurredAt,
                        note = state.note,
                        isExcluded = state.isExcluded,
                        tagIds = state.selectedTagIds,
                    ),
                )
            }
            result.onSuccess {
                if (saveAgain) {
                    editingTransaction = null
                    _transactionUiState.value = state.copy(
                        editingId = null,
                        amountInput = "",
                        categoryId = null,
                        selectedTagIds = emptySet(),
                        note = "",
                        isExcluded = false,
                        isSaving = false,
                    )
                } else {
                    _transactionUiState.value = state.copy(isSaving = false, completed = true)
                }
            }.onFailure { error ->
                _transactionUiState.value = state.copy(isSaving = false, error = error.message)
            }
        }
    }
    fun consumeTransactionCompletion() {
        _transactionUiState.value = _transactionUiState.value.copy(completed = false)
    }

    private fun observeEditorLedger(ledgerId: String?) {
        editorCategoriesJob?.cancel()
        editorTagsJob?.cancel()
        if (ledgerId == null) {
            _transactionUiState.value = _transactionUiState.value.copy(categories = emptyList(), tags = emptyList())
            return
        }
        editorCategoriesJob = viewModelScope.launch {
            sharedApp.management.observeCategories(ledgerId).collect { result ->
                _transactionUiState.value = _transactionUiState.value.copy(categories = result.getOrDefault(emptyList()))
            }
        }
        editorTagsJob = viewModelScope.launch {
            sharedApp.management.observeTags(ledgerId).collect { result ->
                _transactionUiState.value = _transactionUiState.value.copy(tags = result.getOrDefault(emptyList()))
            }
        }
    }

    fun setAppearanceMode(mode: AppearanceMode) = savePreferences(preferences.copy(appearanceMode = mode))
    fun setThemeColor(color: ThemeColor) = savePreferences(preferences.copy(themeColor = color))
    fun setAppLockEnabled(enabled: Boolean) = savePreferences(preferences.copy(appLockEnabled = enabled))
    fun configureSync(target: SyncTarget, retention: Int) {
        viewModelScope.launch {
            sharedApp.sync.configure(SyncConfig(target, retention)).onFailure { error ->
                _moreUiState.value = _moreUiState.value.copy(error = error.message)
            }
        }
    }
    fun syncNow() {
        viewModelScope.launch {
            sharedApp.sync.syncNow().onFailure { error ->
                _moreUiState.value = _moreUiState.value.copy(error = error.message)
            }
        }
    }
    fun loadBackups() {
        _moreUiState.value = _moreUiState.value.copy(isLoadingBackups = true, error = null)
        viewModelScope.launch {
            sharedApp.sync.listBackups().onSuccess { backups ->
                _moreUiState.value = _moreUiState.value.copy(backups = backups, isLoadingBackups = false)
            }.onFailure { error ->
                _moreUiState.value = _moreUiState.value.copy(isLoadingBackups = false, error = error.message)
            }
        }
    }
    fun restoreBackup(meta: RemoteBackupMeta) {
        viewModelScope.launch {
            sharedApp.sync.restore(meta).onFailure { error ->
                _moreUiState.value = _moreUiState.value.copy(error = error.message)
            }
        }
    }

    fun selectMoreLedger(ledgerId: String) {
        _moreUiState.value = _moreUiState.value.copy(selectedLedgerId = ledgerId)
        observeMoreLedger(ledgerId)
    }

    private fun observeMoreLedger(ledgerId: String) {
        moreBudgetsJob?.cancel()
        moreBudgetsJob = viewModelScope.launch {
            sharedApp.budgets.observe(ledgerId).collect { result ->
                val budgets = result.getOrDefault(emptyList())
                _moreUiState.value = _moreUiState.value.copy(
                    budgets = buildBudgetProgress(
                        budgets = budgets,
                        categories = _moreUiState.value.categories,
                        monthTransactions = _homeUiState.value.home?.groups.orEmpty().flatMap { it.items },
                    ),
                )
            }
        }
        moreCategoriesJob?.cancel()
        moreTagsJob?.cancel()
        moreRulesJob?.cancel()
        moreCategoriesJob = viewModelScope.launch {
            sharedApp.management.observeCategories(ledgerId).collect { result ->
                _moreUiState.value = _moreUiState.value.copy(categories = result.getOrDefault(emptyList()))
            }
        }
        moreTagsJob = viewModelScope.launch {
            sharedApp.management.observeTags(ledgerId).collect { result ->
                _moreUiState.value = _moreUiState.value.copy(tags = result.getOrDefault(emptyList()))
            }
        }
        moreRulesJob = viewModelScope.launch {
            sharedApp.management.observeRules(ledgerId).collect { result ->
                _moreUiState.value = _moreUiState.value.copy(rules = result.getOrDefault(emptyList()))
            }
        }
    }

    fun saveLedger(id: String?, name: String, coverKey: String?) = mutate {
        val ledger = Ledger(id ?: UUID.randomUUID().toString(), name, coverKey)
        if (id == null) sharedApp.createLedger(ledger) else sharedApp.updateLedger(ledger)
    }

    fun deleteLedger(id: String) = mutate { sharedApp.deleteLedger(id) }

    fun setDefaultLedger(id: String?) = mutate { sharedApp.setDefaultLedger(id) }

    fun saveAccount(
        id: String?,
        name: String,
        type: AccountType,
        iconKey: String,
        cardNumber: String?,
        note: String?,
        balance: Money,
        includeInTotalAssets: Boolean,
    ) = mutate {
        val account = Account(
            id = id ?: UUID.randomUUID().toString(),
            name = name,
            type = type,
            iconKey = iconKey,
            cardNumber = cardNumber,
            note = note,
            balance = balance,
            includeInTotalAssets = includeInTotalAssets,
        )
        if (id == null) sharedApp.createAccount(account) else sharedApp.updateAccount(account)
    }

    fun calibrateAccount(id: String, balance: Money) = mutate { sharedApp.calibrateAccount(id, balance) }

    fun deleteAccount(id: String) = mutate { sharedApp.deleteAccount(id) }

    fun saveCategory(
        id: String?,
        ledgerId: String,
        parentId: String?,
        name: String,
        iconKey: String?,
        type: TransactionType,
    ) = mutate {
        val category = Category(id ?: UUID.randomUUID().toString(), ledgerId, parentId, name, iconKey, type)
        if (id == null) sharedApp.createCategory(category) else sharedApp.updateCategory(category)
    }

    fun deleteCategory(id: String) = mutate { sharedApp.deleteCategory(id) }

    fun saveTag(id: String?, ledgerId: String, name: String) = mutate {
        val tag = Tag(id ?: UUID.randomUUID().toString(), ledgerId, name)
        if (id == null) sharedApp.createTag(tag) else sharedApp.updateTag(tag)
    }

    fun deleteTag(id: String) = mutate { sharedApp.deleteTag(id) }

    fun saveRule(
        id: String?,
        ledgerId: String,
        name: String,
        conditionType: RuleConditionType,
        conditionValue: String,
        actionType: RuleActionType,
        actionValue: String,
        priority: Int,
    ) = mutate {
        val rule = Rule(
            id = id ?: UUID.randomUUID().toString(),
            ledgerId = ledgerId,
            name = name,
            conditionType = conditionType,
            conditionValue = conditionValue,
            actionType = actionType,
            actionValue = actionValue,
            priority = priority,
        )
        if (id == null) sharedApp.createRule(rule) else sharedApp.updateRule(rule)
    }

    fun deleteRule(id: String) = mutate { sharedApp.deleteRule(id) }

    /** 规则页长按拖拽后一次性落库。 */
    fun reorderRules(ruleIds: List<String>) = mutate {
        val ledgerId = _moreUiState.value.selectedLedgerId
            ?: return@mutate Result.failure(IllegalStateException("请先选择账本"))
        sharedApp.reorderRules(ledgerId, ruleIds)
    }

    fun saveReminder(
        id: String?,
        type: ReminderType,
        name: String,
        amount: Money?,
        schedule: ReminderSchedule,
        paused: Boolean,
    ) = mutate {
        val reminder = Reminder(id ?: UUID.randomUUID().toString(), type, name, amount, schedule, paused)
        if (id == null) sharedApp.createReminder(reminder) else sharedApp.updateReminder(reminder)
    }

    fun setReminderPaused(reminder: Reminder, paused: Boolean) = mutate {
        sharedApp.setReminderPaused(reminder, paused)
    }

    fun deleteReminder(id: String) = mutate { sharedApp.deleteReminder(id) }

    fun saveBudget(id: String?, categoryId: String?, amount: Money) = mutate {
        val ledgerId = _moreUiState.value.selectedLedgerId
            ?: return@mutate Result.failure(IllegalStateException("请先选择账本"))
        sharedApp.budgets.save(id, ledgerId, categoryId, amount)
    }

    fun deleteBudget(id: String) = mutate { sharedApp.budgets.delete(id) }

    fun importFile(
        ledgerId: String,
        fileName: String,
        bytes: ByteArray,
        selectedFormat: ImportFormat? = null,
    ) {
        importJob?.cancel()
        _moreUiState.value = _moreUiState.value.copy(
            importPreview = null,
            importDateFilter = null,
            importFileName = fileName,
            importFormat = selectedFormat,
            importMessage = null,
            isImporting = true,
            error = null,
        )
        importJob = viewModelScope.launch {
            sharedApp.imports.preview(
                ImportRequest(
                    ledgerId = ledgerId,
                    fileName = fileName,
                    bytes = bytes,
                    selectedFormat = selectedFormat,
                ),
            ).collect { result ->
                result.onSuccess { preview ->
                    _moreUiState.value = _moreUiState.value.copy(
                        importPreview = preview,
                        importFormat = preview.format,
                        isImporting = preview.phase != com.omniflow.core.domain.model.ImportPreviewPhase.READY,
                    )
                    // 解析完成时用明细的最早/最晚日期预填筛选，默认全区间展示。
                    if (preview.phase == com.omniflow.core.domain.model.ImportPreviewPhase.READY &&
                        _moreUiState.value.importDateFilter == null
                    ) {
                        preview.occurredDateBounds()?.let { (start, end) ->
                            _moreUiState.value = _moreUiState.value.copy(
                                importDateFilter = ImportDateFilterState(enabled = true, start = start, end = end),
                            )
                        }
                    }
                }.onFailure { error ->
                    _moreUiState.value = _moreUiState.value.copy(isImporting = false, error = error.message)
                }
            }
        }
    }

    fun editImportItem(
        itemId: String,
        type: TransactionType?,
        categoryId: String?,
        accountId: String?,
        isSkipped: Boolean,
        note: String? = null,
        tags: List<String>? = null,
        isExcluded: Boolean? = null,
    ) {
        val preview = _moreUiState.value.importPreview ?: return
        val item = preview.items.firstOrNull { it.id == itemId } ?: return
        viewModelScope.launch {
            sharedApp.imports.editItem(
                ImportPreviewEdit(
                    sessionId = preview.sessionId,
                    itemId = item.id,
                    type = type,
                    categoryId = categoryId,
                    accountId = accountId,
                    note = note ?: item.note,
                    tags = tags ?: item.tags,
                    isExcluded = isExcluded ?: item.isExcluded,
                    isSkipped = isSkipped,
                ),
            ).onSuccess { state -> _moreUiState.value = _moreUiState.value.copy(importPreview = state) }
                .onFailure { error -> _moreUiState.value = _moreUiState.value.copy(error = error.message) }
        }
    }

    /** 整组或单条改分类；导入时唯一需要用户决策的字段就是分类。 */
    fun setImportCategory(itemIds: Set<String>, categoryId: String?, type: TransactionType? = null) {
        val preview = _moreUiState.value.importPreview ?: return
        if (itemIds.isEmpty()) return
        viewModelScope.launch {
            sharedApp.imports.editCategories(
                preview.sessionId,
                ImportCategoryBatchEdit(itemIds, categoryId, type),
            ).onSuccess { updated -> _moreUiState.value = _moreUiState.value.copy(importPreview = updated) }
                .onFailure { error -> _moreUiState.value = _moreUiState.value.copy(error = error.message) }
        }
    }

    /** 整组或单条切换是否入账。 */
    fun setImportSkipped(itemIds: Set<String>, skipped: Boolean) {
        val preview = _moreUiState.value.importPreview ?: return
        if (itemIds.isEmpty()) return
        viewModelScope.launch {
            sharedApp.imports.editSkipped(
                preview.sessionId,
                ImportExcludeBatchEdit(itemIds, skipped),
            ).onSuccess { updated -> _moreUiState.value = _moreUiState.value.copy(importPreview = updated) }
                .onFailure { error -> _moreUiState.value = _moreUiState.value.copy(error = error.message) }
        }
    }

    /** AI 建议失败后重试；结果直接替换当前预览。 */
    fun resuggestImport() {
        val preview = _moreUiState.value.importPreview ?: return
        _moreUiState.value = _moreUiState.value.copy(isImporting = true)
        viewModelScope.launch {
            sharedApp.imports.resuggest(preview.sessionId).onSuccess { updated ->
                _moreUiState.value = _moreUiState.value.copy(importPreview = updated, isImporting = false)
            }.onFailure { error ->
                _moreUiState.value = _moreUiState.value.copy(isImporting = false, error = error.message)
            }
        }
    }

    fun setImportGroupMode(mode: ImportGroupMode) {
        _moreUiState.value = _moreUiState.value.copy(importGroupMode = mode)
    }

    fun setImportFilter(filter: ImportFilter) {
        _moreUiState.value = _moreUiState.value.copy(importFilter = filter)
    }

    /** 更新预览页日期筛选；起止日自动归一化。 */
    fun setImportDateFilter(filter: ImportDateFilterState) {
        _moreUiState.value = _moreUiState.value.copy(
            importDateFilter = filter.copy(
                start = minOf(filter.start, filter.end),
                end = maxOf(filter.start, filter.end),
            ),
        )
    }

    fun cancelImport() {
        val preview = _moreUiState.value.importPreview ?: return
        _moreUiState.value = _moreUiState.value.copy(importPreview = null, importDateFilter = null, importMessage = null)
        viewModelScope.launch { sharedApp.imports.cancel(preview.sessionId) }
    }

    fun commitImport() {
        val state = _moreUiState.value
        val preview = state.importPreview ?: return
        val dateRange = state.importDateFilter?.takeIf { it.enabled }?.let { exportRange(it.start, it.end) }
        _moreUiState.value = state.copy(isImporting = true, error = null)
        viewModelScope.launch {
            sharedApp.imports.commit(preview.sessionId, dateRange).onSuccess { result ->
                _moreUiState.value = _moreUiState.value.copy(
                    importPreview = null,
                    importDateFilter = null,
                    isImporting = false,
                    importMessage = "已导入 ${result.importedCount} 条，跳过 ${result.excludedCount} 条",
                )
            }.onFailure { error ->
                _moreUiState.value = _moreUiState.value.copy(isImporting = false, error = error.message)
            }
        }
    }

    fun observeExportPreview(scope: LedgerScope, dateRange: DateRange?, type: TransactionType?) {
        exportPreviewJob?.cancel()
        _moreUiState.value = _moreUiState.value.copy(
            exportPreviewCount = null,
            isLoadingExportPreview = true,
            error = null,
        )
        exportPreviewJob = viewModelScope.launch {
            sharedApp.home.observeTransactionDetails(
                TransactionDetailQuery(scope, dateRange ?: allExportDateRange, type),
            ).collect { result ->
                result.onSuccess { detail ->
                    _moreUiState.value = _moreUiState.value.copy(
                        exportPreviewCount = detail.items.size,
                        isLoadingExportPreview = false,
                        error = null,
                    )
                }.onFailure { error ->
                    _moreUiState.value = _moreUiState.value.copy(
                        isLoadingExportPreview = false,
                        error = error.message,
                    )
                }
            }
        }
    }

    fun prepareExport(scope: LedgerScope, dateRange: DateRange?, type: TransactionType?) {
        _moreUiState.value = _moreUiState.value.copy(isExporting = true, error = null)
        viewModelScope.launch {
            val ledgerIds = when (scope) {
                LedgerScope.All -> emptySet()
                is LedgerScope.Single -> setOf(scope.ledgerId)
            }
            sharedApp.qingzi.export(
                QingziExportRequest(ledgerIds = ledgerIds, dateRange = dateRange, type = type),
            ).onSuccess { result ->
                _moreUiState.value = _moreUiState.value.copy(
                    exportPayload = result.payload,
                    exportWarnings = result.warnings,
                    isExporting = false,
                )
            }.onFailure { error ->
                _moreUiState.value = _moreUiState.value.copy(isExporting = false, error = error.message)
            }
        }
    }

    fun consumeExportPayload() {
        _moreUiState.value = _moreUiState.value.copy(exportPayload = null)
    }

    fun clearMoreMessage() {
        _moreUiState.value = _moreUiState.value.copy(error = null, importMessage = null)
    }

    private fun mutate(block: suspend () -> Result<Unit>) {
        _moreUiState.value = _moreUiState.value.copy(error = null)
        viewModelScope.launch {
            block().onFailure { error -> _moreUiState.value = _moreUiState.value.copy(error = error.message) }
        }
    }

    private fun savePreferences(updated: AppPreferences) {
        preferences = updated
        _moreUiState.value = _moreUiState.value.copy(preferences = updated)
        viewModelScope.launch { sharedApp.preferences.save(updated) }
    }
}

class OmniFlowViewModelFactory(
    private val sharedApp: SharedApp,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = OmniFlowViewModel(sharedApp) as T
}

internal val ChinaTimeZone = TimeZone.currentSystemDefault()

internal fun monthRange(now: Instant): DateRange = monthRange(now.toLocalDateTime(ChinaTimeZone).date)

internal fun monthRange(date: LocalDate): DateRange {
    val next = javaDate(LocalDate(date.year, date.monthNumber, 1)).plusMonths(1)
    return DateRange(
        LocalDate(date.year, date.monthNumber, 1).atStartOfDayIn(ChinaTimeZone),
        next.toKotlinDate().atStartOfDayIn(ChinaTimeZone),
    )
}

internal fun dateRange(date: LocalDate): DateRange {
    val next = javaDate(date).plusDays(1).toKotlinDate()
    return DateRange(date.atStartOfDayIn(ChinaTimeZone), next.atStartOfDayIn(ChinaTimeZone))
}

private fun analyticsRange(mode: AnalyticsRangeMode, anchor: LocalDate): DateRange = when (mode) {
    AnalyticsRangeMode.WEEK -> {
        val start = javaDate(anchor).minusDays((javaDate(anchor).dayOfWeek.value - 1).toLong()).toKotlinDate()
        DateRange(start.atStartOfDayIn(ChinaTimeZone), javaDate(start).plusDays(7).toKotlinDate().atStartOfDayIn(ChinaTimeZone))
    }
    AnalyticsRangeMode.MONTH -> monthRange(anchor)
    AnalyticsRangeMode.YEAR -> DateRange(
        LocalDate(anchor.year, 1, 1).atStartOfDayIn(ChinaTimeZone),
        LocalDate(anchor.year + 1, 1, 1).atStartOfDayIn(ChinaTimeZone),
    )
    AnalyticsRangeMode.CUSTOM -> monthRange(anchor)
}

private fun analyticsGranularity(mode: AnalyticsRangeMode, range: DateRange): TimeGranularity = when (mode) {
    AnalyticsRangeMode.WEEK, AnalyticsRangeMode.MONTH -> TimeGranularity.DAY
    AnalyticsRangeMode.YEAR -> TimeGranularity.MONTH
    AnalyticsRangeMode.CUSTOM -> {
        val days = (range.endExclusive.toEpochMilliseconds() - range.startInclusive.toEpochMilliseconds()) / 86_400_000
        if (days <= 90) TimeGranularity.DAY else TimeGranularity.MONTH
    }
}

private fun javaDate(date: LocalDate) = JavaLocalDate.of(date.year, date.monthNumber, date.dayOfMonth)
private fun JavaLocalDate.toKotlinDate() = LocalDate(year, monthValue, dayOfMonth)

private val allExportDateRange = DateRange(
    startInclusive = Instant.fromEpochMilliseconds(0),
    endExclusive = Instant.fromEpochMilliseconds(Long.MAX_VALUE),
)

private fun Money.toInputString(): String = "${minor / 100}.${kotlin.math.abs(minor % 100).toString().padStart(2, '0')}"

private fun String.toSearchMoneyOrNull(): Money? {
    val value = trim().takeIf(String::isNotEmpty)?.toBigDecimalOrNull() ?: return null
    if (value.signum() < 0 || value.scale() > 2) return null
    return runCatching { Money(value.movePointRight(2).longValueExact()) }.getOrNull()
}

private fun evaluateAmount(expression: String): Long? {
    if (expression.isBlank()) return null
    var total = 0L
    var operation = '+'
    val token = StringBuilder()
    for (character in expression + "+") {
        if (character == '+' || character == '-') {
            val value = decimalMinor(token.toString()) ?: return null
            total = if (operation == '+') total + value else total - value
            operation = character
            token.clear()
        } else {
            token.append(character)
        }
    }
    return total.takeIf { it > 0 }
}

private fun decimalMinor(value: String): Long? {
    if (value.isBlank()) return null
    val parts = value.split('.', limit = 2)
    val yuan = parts[0].toLongOrNull() ?: return null
    val fraction = parts.getOrElse(1) { "" }
    if (fraction.length > 2 || fraction.any { !it.isDigit() }) return null
    return yuan * 100 + fraction.padEnd(2, '0').toLongOrNull().orZero()
}

private fun Long?.orZero(): Long = this ?: 0L
