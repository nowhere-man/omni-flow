package com.omniflow.android.ui

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omniflow.shared.domain.model.CalendarDaySummary
import com.omniflow.shared.domain.model.CalendarTransactionFilter
import com.omniflow.shared.domain.model.DayTransactionGroup
import com.omniflow.shared.domain.model.LedgerScope
import com.omniflow.shared.domain.model.Money
import com.omniflow.shared.domain.model.TransactionDetailState
import com.omniflow.shared.domain.model.TransactionListItem
import com.omniflow.shared.domain.model.TransactionType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.LocalDate as JavaLocalDate
import java.time.format.TextStyle
import java.util.Locale

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
    var destination by rememberSaveable { mutableStateOf(MainDestination.HOME) }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF355E3B),
            secondary = Color(0xFF6A5D35),
            surface = Color(0xFFFFFBFF),
            background = Color(0xFFFFFBFF),
        ),
    ) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    MainDestination.entries.forEach { item ->
                        NavigationBarItem(
                            selected = destination == item,
                            onClick = { destination = item },
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
                    onToggleDisplayMode = viewModel::toggleDisplayMode,
                    onDismissDate = viewModel::dismissDate,
                    modifier = Modifier.padding(padding),
                )

                else -> DestinationShell(destination, Modifier.padding(padding))
            }
        }
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
    onToggleDisplayMode: () -> Unit,
    onDismissDate: () -> Unit,
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
                MonthSelector(home.month.startInclusive.toLocalDateTime(ChinaTimeZone).date, onPreviousMonth, onNextMonth)
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
                        val icon = if (state.displayMode == TransactionDisplayMode.LIST) {
                            Icons.Default.ViewModule
                        } else {
                            Icons.AutoMirrored.Filled.List
                        }
                        Icon(icon, contentDescription = "切换明细展示方式")
                    }
                }
                TransactionGroups(home.groups, state.displayMode)
                Spacer(Modifier.height(12.dp))
            }
        }

        if (state.isDateLoading || state.selectedDate != null || state.dateError != null) {
            DateDetailSheet(
                state = state.selectedDate,
                isLoading = state.isDateLoading,
                error = state.dateError,
                displayMode = state.displayMode,
                onDismiss = onDismissDate,
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
private fun MonthSelector(month: LocalDate, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ArrowBackIosNew, contentDescription = "上个月")
        }
        Text("${month.year}年${month.monthNumber}月", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
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
private fun TransactionGroups(groups: List<DayTransactionGroup>, displayMode: TransactionDisplayMode) {
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
                TransactionItems(group.items, displayMode)
            }
        }
    }
}

@Composable
private fun TransactionItems(items: List<TransactionListItem>, displayMode: TransactionDisplayMode) {
    when (displayMode) {
        TransactionDisplayMode.LIST -> Column {
            items.forEachIndexed { index, item ->
                TransactionListRow(item)
                if (index != items.lastIndex) Divider()
            }
        }

        TransactionDisplayMode.CARD -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { item -> TransactionCard(item, Modifier.weight(1f)) }
                    repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun TransactionListRow(item: TransactionListItem) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
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
private fun TransactionCard(item: TransactionListItem, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
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
        Icon(
            when (iconKey) {
                "banknote", "chart-line" -> Icons.Default.AccountBalance
                "shopping-bag" -> Icons.Default.ShoppingBag
                else -> Icons.Default.Category
            },
            contentDescription = null,
            modifier = Modifier.padding(8.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateDetailSheet(
    state: TransactionDetailState?,
    isLoading: Boolean,
    error: String?,
    displayMode: TransactionDisplayMode,
    onDismiss: () -> Unit,
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
                Text(date.displayName(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                MonthlySummary(state.summary.expenseTotal, state.summary.incomeTotal)
                TransactionItems(state.items, displayMode)
                Spacer(Modifier.height(20.dp))
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

@Composable
private fun DestinationShell(destination: MainDestination, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.TopStart) {
        Text(destination.label, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
    }
}

private val ChinaTimeZone = TimeZone.of("Asia/Shanghai")

private fun LocalDate.displayName(): String {
    val day = JavaLocalDate.of(year, monthNumber, dayOfMonth)
    return "${monthNumber}月${dayOfMonth}日 ${day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINA)}"
}

private fun Money.asRmb(): String {
    val absolute = kotlin.math.abs(minor)
    val prefix = if (minor < 0) "-" else ""
    return "$prefix¥${absolute / 100}.${(absolute % 100).toString().padStart(2, '0')}"
}

private fun Money.asCompactRmb(): String = "¥${minor / 100}"
