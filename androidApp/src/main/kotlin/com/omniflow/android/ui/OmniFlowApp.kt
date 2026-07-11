package com.omniflow.android.ui

import android.Manifest
import android.app.Activity
import android.app.KeyguardManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omniflow.shared.domain.model.CalendarDaySummary
import com.omniflow.shared.domain.model.AppearanceMode
import com.omniflow.shared.domain.model.CalendarTransactionFilter
import com.omniflow.shared.domain.model.DayTransactionGroup
import com.omniflow.shared.domain.model.LedgerScope
import com.omniflow.shared.domain.model.Money
import com.omniflow.shared.domain.model.TransactionDetailState
import com.omniflow.shared.domain.model.TransactionDetailDisplayMode
import com.omniflow.shared.domain.model.TransactionListItem
import com.omniflow.shared.domain.model.TransactionType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime
import java.time.LocalDate as JavaLocalDate
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import com.omniflow.android.ReminderScheduler

private enum class MainDestination(val label: String, val icon: ImageVector) {
    HOME("首页", Icons.Default.Home),
    ANALYTICS("统计", Icons.AutoMirrored.Filled.ShowChart),
    ADD("记账", Icons.Default.AddCircle),
    SEARCH("搜索", Icons.Default.Search),
    MORE("更多", Icons.Default.MoreHoriz),
}

private val ExpenseColor = Color(0xFFB3261E)
private val IncomeColor = Color(0xFF2E7D32)

@Composable
fun OmniFlowApp(viewModel: OmniFlowViewModel) {
    val homeState by viewModel.homeUiState.collectAsState()
    val analyticsState by viewModel.analyticsUiState.collectAsState()
    val searchState by viewModel.searchUiState.collectAsState()
    val transactionState by viewModel.transactionUiState.collectAsState()
    val moreState by viewModel.moreUiState.collectAsState()
    var destination by rememberSaveable { mutableStateOf(MainDestination.HOME) }
    val darkTheme = when (homeState.appearanceMode) {
        AppearanceMode.SYSTEM -> isSystemInDarkTheme()
        AppearanceMode.LIGHT -> false
        AppearanceMode.DARK -> true
    }
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(moreState.reminders) {
        ReminderScheduler(context).sync(moreState.reminders)
        if (moreState.reminders.isNotEmpty() && Build.VERSION.SDK_INT >= 33) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    LaunchedEffect(transactionState.completed) {
        if (transactionState.completed) {
            destination = MainDestination.HOME
            viewModel.consumeTransactionCompletion()
        }
    }

    MaterialTheme(
        colorScheme = if (darkTheme) {
            darkColorScheme(primary = Color(0xFFB5CCB6), secondary = Color(0xFFD6C790))
        } else {
            lightColorScheme(
                primary = Color(0xFF355E3B),
                secondary = Color(0xFF6A5D35),
                surface = Color(0xFFFFFBFF),
                background = Color(0xFFFFFBFF),
            )
        },
    ) {
        AppLockGate(moreState.preferences.appLockEnabled) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    MainDestination.entries.forEach { item ->
                        NavigationBarItem(
                            selected = destination == item,
                            onClick = {
                                if (item == MainDestination.ADD && destination != MainDestination.ADD) viewModel.startNewTransaction()
                                destination = item
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                        )
                    }
                }
            },
        ) { padding ->
            when (destination) {
                MainDestination.HOME -> HomeScreen(
                    state = homeState,
                    onPreviousMonth = viewModel::previousMonth,
                    onNextMonth = viewModel::nextMonth,
                    onCalendarFilter = viewModel::setCalendarFilter,
                    onLedgerMenu = viewModel::toggleLedgerMenu,
                    onLedgerSelected = viewModel::selectLedger,
                    onDateSelected = viewModel::showDate,
                    onMonthSelected = viewModel::selectHomeMonth,
                    onToggleDisplayMode = viewModel::toggleDisplayMode,
                    onDismissDate = viewModel::dismissDate,
                    onDateScope = viewModel::selectDateScope,
                    onAdd = { date, ledgerId ->
                        viewModel.startNewTransaction(date, ledgerId)
                        destination = MainDestination.ADD
                    },
                    onEdit = { transactionId ->
                        viewModel.editTransaction(transactionId)
                        destination = MainDestination.ADD
                    },
                    modifier = Modifier.padding(padding),
                )
                MainDestination.ANALYTICS -> AnalyticsScreen(
                    state = analyticsState,
                    onScope = viewModel::setAnalyticsScope,
                    onRangeMode = viewModel::setAnalyticsRangeMode,
                    onShiftRange = viewModel::shiftAnalyticsRange,
                    onCustomRange = viewModel::setAnalyticsCustomRange,
                    onRankingType = viewModel::setRankingType,
                    onCategoryAnalysis = viewModel::setCategoryAnalysis,
                    onCategoryDrillDown = viewModel::setCategoryDrillDown,
                    onStatementTable = viewModel::loadStatementTable,
                    onDismissStatementTable = viewModel::dismissStatementTable,
                    onEditTransaction = { transactionId ->
                        viewModel.editTransaction(transactionId)
                        destination = MainDestination.ADD
                    },
                    modifier = Modifier.padding(padding),
                )
                MainDestination.ADD -> TransactionEditorScreen(
                    state = transactionState,
                    onType = viewModel::setTransactionType,
                    onLedger = viewModel::setTransactionLedger,
                    onAccount = viewModel::setTransactionAccount,
                    onCategory = viewModel::setTransactionCategory,
                    onTag = viewModel::toggleTransactionTag,
                    onNote = viewModel::setTransactionNote,
                    onDate = viewModel::setTransactionDate,
                    onExcluded = viewModel::setTransactionExcluded,
                    onAmountKey = viewModel::pressAmountKey,
                    onSaveAgain = { viewModel.saveTransaction(true) },
                    onDone = { viewModel.saveTransaction(false) },
                    onDelete = viewModel::deleteEditingTransaction,
                    modifier = Modifier.padding(padding),
                )
                MainDestination.SEARCH -> SearchScreen(
                    state = searchState,
                    onKeyword = viewModel::setSearchKeyword,
                    onScope = viewModel::setSearchScope,
                    onType = viewModel::setSearchType,
                    onPrimaryCategory = viewModel::setSearchPrimaryCategory,
                    onSecondaryCategory = viewModel::setSearchSecondaryCategory,
                    onTag = viewModel::setSearchTag,
                    onAccount = viewModel::setSearchAccount,
                    onAmount = viewModel::setSearchAmount,
                    onDateRange = viewModel::setSearchDateRange,
                    onToggleAdvanced = viewModel::toggleAdvancedSearch,
                    onClear = viewModel::clearSearch,
                    onEditTransaction = { transactionId ->
                        viewModel.editTransaction(transactionId)
                        destination = MainDestination.ADD
                    },
                    modifier = Modifier.padding(padding),
                )
                MainDestination.MORE -> MoreScreen(moreState, viewModel, Modifier.padding(padding))
            }
        }
        }
    }
}

@Composable
private fun AppLockGate(enabled: Boolean, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var locked by rememberSaveable(enabled) { mutableStateOf(enabled) }
    var authInProgress by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        authInProgress = false
        locked = result.resultCode != Activity.RESULT_OK
    }
    fun requestUnlock() {
        val manager = context.getSystemService(KeyguardManager::class.java)
        val intent = manager.createConfirmDeviceCredentialIntent("解锁 OmniFlow", "请验证设备凭据")
        if (intent == null) locked = false else {
            authInProgress = true
            launcher.launch(intent)
        }
    }
    DisposableEffect(lifecycleOwner, enabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && enabled && !authInProgress) locked = true
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(enabled, locked) {
        if (!enabled) locked = false
        if (enabled && locked && !authInProgress) requestUnlock()
    }
    if (enabled && locked) {
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("OmniFlow 已锁定", style = MaterialTheme.typography.headlineSmall)
                Button(onClick = ::requestUnlock) { Text("解锁") }
            }
        }
    } else {
        content()
    }
}

@Composable
private fun HomeScreen(
    state: HomeUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCalendarFilter: (CalendarTransactionFilter) -> Unit,
    onLedgerMenu: () -> Unit,
    onLedgerSelected: (LedgerScope) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onMonthSelected: (LocalDate) -> Unit,
    onToggleDisplayMode: () -> Unit,
    onDismissDate: () -> Unit,
    onDateScope: (LedgerScope) -> Unit,
    onAdd: (LocalDate?, String?) -> Unit,
    onEdit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val home = state.home
    Box(modifier.fillMaxSize()) {
        when {
            state.isLoading && home == null -> LoadingState()
            state.error != null && home == null -> ErrorState(state.error)
            home != null -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(Modifier.height(8.dp))
                LedgerSelector(
                    scope = home.scope,
                    ledgers = state.ledgers,
                    expanded = state.isLedgerMenuExpanded,
                    onToggle = onLedgerMenu,
                    onSelected = onLedgerSelected,
                )
                MonthSelector(home.month.startInclusive.toLocalDateTime(ChinaTimeZone).date, onPreviousMonth, onNextMonth, onMonthSelected)
                MonthlySummary(home.summary.expenseTotal, home.summary.incomeTotal)
                CalendarFilter(state.calendarFilter, onCalendarFilter)
                CalendarMonth(
                    month = home.month.startInclusive.toLocalDateTime(ChinaTimeZone).date,
                    summaries = home.calendar,
                    onDateSelected = onDateSelected,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("账单", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onToggleDisplayMode) {
                        val icon = if (state.displayMode == TransactionDetailDisplayMode.LIST) {
                            Icons.Default.ViewModule
                        } else {
                            Icons.AutoMirrored.Filled.List
                        }
                        Icon(icon, contentDescription = "切换明细展示方式")
                    }
                }
                if (state.ledgers.isEmpty()) {
                    Text("请先在“更多 → 账本”中创建账本", color = MaterialTheme.colorScheme.error)
                }
                TransactionGroups(home.groups, state.displayMode, onEdit)
                Spacer(Modifier.height(12.dp))
            }
        }

        if (state.isDateLoading || state.selectedDate != null || state.dateError != null) {
            DateDetailSheet(
                state = state.selectedDate,
                isLoading = state.isDateLoading,
                error = state.dateError,
                displayMode = state.displayMode,
                ledgers = state.ledgers,
                scope = state.selectedDateScope ?: state.selectedDate?.scope ?: LedgerScope.All,
                onDismiss = onDismissDate,
                onScope = onDateScope,
                onAdd = onAdd,
                onEdit = onEdit,
            )
        }
    }
}

@Composable
private fun LedgerSelector(
    scope: LedgerScope,
    ledgers: List<com.omniflow.shared.domain.model.Ledger>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onSelected: (LedgerScope) -> Unit,
) {
    Box {
        TextButton(onClick = onToggle) {
            Icon(Icons.Default.AccountBalance, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(
                when (scope) {
                    LedgerScope.All -> "所有账本"
                    is LedgerScope.Single -> ledgers.firstOrNull { it.id == scope.ledgerId }?.name ?: "账本"
                },
                style = MaterialTheme.typography.titleMedium,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = onToggle) {
            DropdownMenuItem(text = { Text("所有账本") }, onClick = { onSelected(LedgerScope.All) })
            ledgers.forEach { ledger ->
                DropdownMenuItem(text = { Text(ledger.name) }, onClick = { onSelected(LedgerScope.Single(ledger.id)) })
            }
        }
    }
}

@Composable
private fun MonthSelector(month: LocalDate, onPrevious: () -> Unit, onNext: () -> Unit, onSelected: (LocalDate) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ArrowBackIosNew, contentDescription = "上个月")
        }
        Text(
            "${month.year}年${month.monthNumber}月",
            modifier = Modifier.clickable {
                android.app.DatePickerDialog(
                    context,
                    { _, year, selectedMonth, _ -> onSelected(LocalDate(year, selectedMonth + 1, 1)) },
                    month.year,
                    month.monthNumber - 1,
                    month.dayOfMonth,
                ).show()
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ArrowForwardIos, contentDescription = "下个月")
        }
    }
}

@Composable
private fun MonthlySummary(expense: Money, income: Money) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SummaryAmount("支出", expense, ExpenseColor)
            SummaryAmount("收入", income, IncomeColor)
            SummaryAmount("结余", income - expense, MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun SummaryAmount(label: String, amount: Money, color: Color) {
    Column {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        Text(amount.asRmb(), color = color, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CalendarFilter(
    selectedFilter: CalendarTransactionFilter,
    onSelected: (CalendarTransactionFilter) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CalendarTransactionFilter.entries.forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onSelected(filter) },
                label = {
                    Text(
                        when (filter) {
                            CalendarTransactionFilter.ALL -> "全部"
                            CalendarTransactionFilter.INCOME -> "收入"
                            CalendarTransactionFilter.EXPENSE -> "支出"
                        },
                    )
                },
            )
        }
    }
}

@Composable
private fun CalendarMonth(
    month: LocalDate,
    summaries: List<CalendarDaySummary>,
    onDateSelected: (LocalDate) -> Unit,
) {
    val totals = remember(summaries) { summaries.associateBy(CalendarDaySummary::date) }
    val first = JavaLocalDate.of(month.year, month.monthNumber, 1)
    val leading = first.dayOfWeek.value % 7
    val cells = List(leading) { null } + (1..first.lengthOfMonth()).map { day ->
        LocalDate(month.year, month.monthNumber, day)
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth()) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach { weekday ->
                Text(
                    weekday,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                week.forEach { date ->
                    CalendarCell(date, date?.let(totals::get), onDateSelected, Modifier.weight(1f))
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f).aspectRatio(1f)) }
            }
        }
    }
}

@Composable
private fun CalendarCell(
    date: LocalDate?,
    summary: CalendarDaySummary?,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier,
) {
    if (date == null) {
        Spacer(modifier.aspectRatio(1f))
        return
    }
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .clickable { onDateSelected(date) }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.labelMedium)
        if (summary != null) {
            if (summary.expenseTotal != Money.Zero) {
                Text(summary.expenseTotal.asCompactRmb(), color = ExpenseColor, style = MaterialTheme.typography.labelSmall)
            }
            if (summary.incomeTotal != Money.Zero) {
                Text(summary.incomeTotal.asCompactRmb(), color = IncomeColor, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun TransactionGroups(groups: List<DayTransactionGroup>, displayMode: TransactionDetailDisplayMode, onEdit: (String) -> Unit) {
    if (groups.isEmpty()) {
        Text(
            "暂无账单",
            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        groups.forEach { group ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(group.date.displayName(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    if (group.expenseTotal != Money.Zero) Text("支出 ${group.expenseTotal.asRmb()}", color = ExpenseColor, style = MaterialTheme.typography.labelMedium)
                    if (group.incomeTotal != Money.Zero) Text("收入 ${group.incomeTotal.asRmb()}", color = IncomeColor, style = MaterialTheme.typography.labelMedium)
                }
                TransactionItems(group.items, displayMode, onEdit)
            }
        }
    }
}

@Composable
private fun TransactionItems(items: List<TransactionListItem>, displayMode: TransactionDetailDisplayMode, onEdit: (String) -> Unit) {
    when (displayMode) {
        TransactionDetailDisplayMode.LIST -> Column {
            items.forEachIndexed { index, item ->
                TransactionListRow(item, onEdit)
                if (index != items.lastIndex) Divider()
            }
        }

        TransactionDetailDisplayMode.CARD -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { item -> TransactionCard(item, Modifier.weight(1f), onEdit) }
                    repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun TransactionListRow(item: TransactionListItem, onEdit: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onEdit(item.id) }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryIcon(item.categoryIconKey)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.categoryName, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
            Text(item.accountName, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        Column(horizontalAlignment = Alignment.End) {
            AmountText(item)
            Text(item.occurredAt.toLocalDateTime(ChinaTimeZone).time.toString().take(5), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun TransactionCard(item: TransactionListItem, modifier: Modifier = Modifier, onEdit: (String) -> Unit) {
    Card(
        modifier = modifier.clickable { onEdit(item.id) },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryIcon(item.categoryIconKey)
                Spacer(Modifier.weight(1f))
                AmountText(item)
            }
            Text(item.categoryName, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
            Text(item.accountName, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun AmountText(item: TransactionListItem) {
    val sign = if (item.type == TransactionType.EXPENSE) "-" else "+"
    Text(
        "$sign${item.amount.asRmb()}",
        color = if (item.type == TransactionType.EXPENSE) ExpenseColor else IncomeColor,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun CategoryIcon(iconKey: String?) {
    Surface(
        modifier = Modifier.size(36.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        SvgIcon(iconKey ?: "category", Modifier.padding(8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateDetailSheet(
    state: TransactionDetailState?,
    isLoading: Boolean,
    error: String?,
    displayMode: TransactionDetailDisplayMode,
    ledgers: List<com.omniflow.shared.domain.model.Ledger>,
    scope: LedgerScope,
    onDismiss: () -> Unit,
    onScope: (LedgerScope) -> Unit,
    onAdd: (LocalDate?, String?) -> Unit,
    onEdit: (String) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        when {
            isLoading -> LoadingState()
            error != null -> ErrorState(error)
            state != null -> Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val date = state.date.startInclusive.toLocalDateTime(ChinaTimeZone).date
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(date.displayName(), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { onAdd(date, (scope as? LedgerScope.Single)?.ledgerId) }) { Text("新增") }
                }
                DateLedgerSelector(scope, ledgers, onScope)
                MonthlySummary(state.summary.expenseTotal, state.summary.incomeTotal)
                Text(
                    "支出 ${state.items.count { it.type == TransactionType.EXPENSE }} 笔 · 收入 ${state.items.count { it.type == TransactionType.INCOME }} 笔",
                    style = MaterialTheme.typography.bodySmall,
                )
                TransactionItems(state.items, displayMode, onEdit)
                if (state.items.isEmpty()) Text("当日暂无交易，可点击新增开始记账")
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun DateLedgerSelector(
    scope: LedgerScope,
    ledgers: List<com.omniflow.shared.domain.model.Ledger>,
    onScope: (LedgerScope) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(
                when (scope) {
                    LedgerScope.All -> "所有账本"
                    is LedgerScope.Single -> ledgers.firstOrNull { it.id == scope.ledgerId }?.name ?: "账本"
                },
            )
        }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("所有账本") }, onClick = { expanded = false; onScope(LedgerScope.All) })
            ledgers.forEach { ledger ->
                DropdownMenuItem(
                    text = { Text(ledger.name) },
                    onClick = { expanded = false; onScope(LedgerScope.Single(ledger.id)) },
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(message: String) {
    Text(
        message,
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
    )
}
