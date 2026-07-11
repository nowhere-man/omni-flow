package com.omniflow.android.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.omniflow.shared.domain.model.CategoryShareGranularity
import com.omniflow.shared.domain.model.DateRange
import com.omniflow.shared.domain.model.LedgerScope
import com.omniflow.shared.domain.model.Money
import com.omniflow.shared.domain.model.StatementTable
import com.omniflow.shared.domain.model.TransactionType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

@Composable
internal fun AnalyticsScreen(
    state: AnalyticsUiState,
    onScope: (LedgerScope) -> Unit,
    onRangeMode: (AnalyticsRangeMode) -> Unit,
    onShiftRange: (Long) -> Unit,
    onCustomRange: (DateRange) -> Unit,
    onRankingType: (TransactionType) -> Unit,
    onCategoryAnalysis: (TransactionType, CategoryShareGranularity) -> Unit,
    onCategoryDrillDown: (String?) -> Unit,
    onStatementTable: (Int) -> Unit,
    onDismissStatementTable: () -> Unit,
    onEditTransaction: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            LedgerScopeMenu(state.scope, state.ledgers, onScope)
        }
        if (state.rangeMode == AnalyticsRangeMode.CUSTOM) {
            item { CustomRangePicker(state.range, onCustomRange) }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnalyticsRangeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = state.rangeMode == mode,
                        onClick = { onRangeMode(mode) },
                        label = { Text(mode.label) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onShiftRange(-1) }) {
                    Icon(Icons.Default.ArrowBackIosNew, contentDescription = "上一个范围")
                }
                Text(
                    state.range.displayLabel(),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(onClick = { onShiftRange(1) }) {
                    Icon(Icons.Default.ArrowForwardIos, contentDescription = "下一个范围")
                }
            }
        }
        when {
            state.isLoading && state.dashboard == null -> item { LoadingBlock() }
            state.error != null && state.dashboard == null -> item { ErrorBlock(state.error) }
            state.dashboard != null -> {
                val dashboard = state.dashboard
                item {
                    SummaryRow(
                        dashboard.summary.expenseTotal,
                        dashboard.summary.incomeTotal,
                        dashboard.summary.netIncome,
                    )
                }
                item {
                    AnalyticsCard("收支趋势") {
                        var showIncome by remember { mutableStateOf(true) }
                        var showExpense by remember { mutableStateOf(true) }
                        var selectedPointLabel by remember(dashboard.trend.points) { mutableStateOf<String?>(null) }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(showIncome, { showIncome = !showIncome }, label = { Text("收入") })
                            FilterChip(showExpense, { showExpense = !showExpense }, label = { Text("支出") })
                        }
                        val maximum = dashboard.trend.points.maxOfOrNull {
                            maxOf(it.expense.minor, it.income.minor)
                        }?.coerceAtLeast(1) ?: 1
                        if (dashboard.trend.points.isEmpty()) EmptyText("暂无趋势数据")
                        dashboard.trend.points.forEach { point ->
                            Column(
                                Modifier.fillMaxWidth()
                                    .clickable { selectedPointLabel = point.label }
                                    .background(
                                        if (selectedPointLabel == point.label) MaterialTheme.colorScheme.secondaryContainer
                                        else MaterialTheme.colorScheme.surface,
                                        RoundedCornerShape(12.dp),
                                    )
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(point.label, style = MaterialTheme.typography.labelMedium)
                                if (showIncome) AmountBar("收入", point.income, maximum)
                                if (showExpense) AmountBar("支出", point.expense, maximum)
                            }
                        }
                        dashboard.trend.points.firstOrNull { it.label == selectedPointLabel }?.let { point ->
                            Text("已选 ${point.label}：收入 ${point.income.asRmb()} · 支出 ${point.expense.asRmb()}")
                        }
                        TextButton(onClick = {
                            val year = state.range.startInclusive
                                .toLocalDateTime(ChinaTimeZone).year
                            onStatementTable(year)
                        }) { Text("查看账单表格") }
                    }
                }
                item {
                    AnalyticsCard("环比 / 同比") {
                        Text("环比支出 ${dashboard.previousPeriod.expenseChange.asRmb()}")
                        Text("环比收入 ${dashboard.previousPeriod.incomeChange.asRmb()}")
                        Text("同比支出 ${dashboard.yearOverYear.expenseChange.asRmb()}")
                        Text("同比收入 ${dashboard.yearOverYear.incomeChange.asRmb()}")
                        Text("环比结余 ${dashboard.previousPeriod.netIncomeChange.asRmb()}")
                        Text("同比结余 ${dashboard.yearOverYear.netIncomeChange.asRmb()}")
                    }
                }
                item {
                    AnalyticsCard(if (state.rankingType == TransactionType.EXPENSE) "支出排行榜" else "收入排行榜") {
                        TypeSwitch(state.rankingType, onRankingType)
                        if (dashboard.ranking.isEmpty()) EmptyText("暂无排行数据")
                        dashboard.ranking.forEachIndexed { index, item ->
                            Row(
                                Modifier.fillMaxWidth().clickable { onEditTransaction(item.transaction.id) }.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("${index + 1}.", modifier = Modifier.width(32.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(item.transaction.categoryName, fontWeight = FontWeight.Medium)
                                    Text(item.transaction.accountName, style = MaterialTheme.typography.bodySmall)
                                }
                                Text(item.transaction.amount.asRmb(), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                item {
                    AnalyticsCard("分类占比") {
                        TypeSwitch(state.categoryType) { type ->
                            onCategoryAnalysis(type, state.categoryGranularity)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CategoryShareGranularity.entries.forEach { granularity ->
                                FilterChip(
                                    selected = state.categoryGranularity == granularity,
                                    onClick = { onCategoryAnalysis(state.categoryType, granularity) },
                                    label = { Text(if (granularity == CategoryShareGranularity.PRIMARY) "一级分类" else "二级分类") },
                                )
                            }
                        }
                        if (state.primaryCategoryId != null) {
                            TextButton(onClick = { onCategoryDrillDown(null) }) { Text("返回一级分类") }
                        }
                        val total = dashboard.categoryShares.sumOf { it.amount.minor }.coerceAtLeast(1)
                        if (dashboard.categoryShares.isEmpty()) EmptyText("暂无分类数据")
                        dashboard.categoryShares.forEach { share ->
                            Column(
                                Modifier.fillMaxWidth().clickable(
                                    enabled = state.categoryGranularity == CategoryShareGranularity.PRIMARY,
                                ) { onCategoryDrillDown(share.categoryId) },
                            ) {
                                AmountBar(share.categoryName, share.amount, total)
                            }
                        }
                    }
                }
                item {
                    AnalyticsCard("标签分析") {
                        if (dashboard.tagSummary.isEmpty()) EmptyText("暂无标签数据")
                        dashboard.tagSummary.forEach { item ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(item.tag.name)
                                Text("支出 ${item.expense.asRmb()}  收入 ${item.income.asRmb()}")
                            }
                        }
                    }
                }
                item {
                    AnalyticsCard("账户资产分布") {
                        Text("净资产 ${dashboard.accountSummary.netAssets.asRmb()} · 资产 ${dashboard.accountSummary.assets.asRmb()} · 负债 ${dashboard.accountSummary.liabilities.asRmb()}")
                        if (dashboard.accountAssets.isEmpty()) EmptyText("暂无计入总资产的账户")
                        val maximum = dashboard.accountAssets.maxOfOrNull { it.balance.minor }?.coerceAtLeast(1) ?: 1
                        dashboard.accountAssets.forEach { account ->
                            AmountBar(account.accountName, account.balance, maximum)
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    state.statementTable?.let { table -> StatementTableSheet(table, onDismissStatementTable) }
}

@Composable
private fun CustomRangePicker(range: DateRange, onRange: (DateRange) -> Unit) {
    var start by remember(range) { mutableStateOf(range.startInclusive.toLocalDateTime(ChinaTimeZone).date) }
    var end by remember(range) {
        mutableStateOf(
            kotlinx.datetime.Instant.fromEpochMilliseconds(range.endExclusive.toEpochMilliseconds() - 1)
                .toLocalDateTime(ChinaTimeZone).date,
        )
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AnalyticsDateButton("开始", start, Modifier.weight(1f)) { start = it }
        AnalyticsDateButton("结束", end, Modifier.weight(1f)) { end = it }
        TextButton(onClick = {
            val orderedStart = minOf(start, end)
            val orderedEnd = maxOf(start, end)
            val next = java.time.LocalDate.of(orderedEnd.year, orderedEnd.monthNumber, orderedEnd.dayOfMonth).plusDays(1)
            onRange(
                DateRange(
                    orderedStart.atStartOfDayIn(ChinaTimeZone),
                    LocalDate(next.year, next.monthValue, next.dayOfMonth).atStartOfDayIn(ChinaTimeZone),
                ),
            )
        }) { Text("应用") }
    }
}

@Composable
private fun AnalyticsDateButton(label: String, date: LocalDate, modifier: Modifier, onDate: (LocalDate) -> Unit) {
    val context = LocalContext.current
    TextButton(
        onClick = {
            DatePickerDialog(
                context,
                { _, year, month, day -> onDate(LocalDate(year, month + 1, day)) },
                date.year,
                date.monthNumber - 1,
                date.dayOfMonth,
            ).show()
        },
        modifier = modifier,
    ) { Text("$label ${date.monthNumber}/${date.dayOfMonth}") }
}

@Composable
private fun LedgerScopeMenu(scope: LedgerScope, ledgers: List<com.omniflow.shared.domain.model.Ledger>, onScope: (LedgerScope) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    TextButton(onClick = { expanded = true }) {
        Text(when (scope) {
            LedgerScope.All -> "所有账本"
            is LedgerScope.Single -> ledgers.firstOrNull { it.id == scope.ledgerId }?.name ?: "账本"
        })
    }
    DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(text = { Text("所有账本") }, onClick = { expanded = false; onScope(LedgerScope.All) })
        ledgers.forEach { ledger ->
            DropdownMenuItem(text = { Text(ledger.name) }, onClick = { expanded = false; onScope(LedgerScope.Single(ledger.id)) })
        }
    }
}

@Composable
private fun SummaryRow(expense: Money, income: Money, net: Money) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SummaryCard("总支出", expense, Modifier.weight(1f))
        SummaryCard("总收入", income, Modifier.weight(1f))
        SummaryCard("总结余", net, Modifier.weight(1f))
    }
}

@Composable
private fun SummaryCard(label: String, amount: Money, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(amount.asRmb(), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun AnalyticsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun AmountBar(label: String, amount: Money, maximum: Long) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(amount.asRmb())
    }
    LinearProgressIndicator(
        progress = { (amount.minor.toFloat() / maximum.toFloat()).coerceIn(0f, 1f) },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun TypeSwitch(selected: TransactionType, onSelected: (TransactionType) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TransactionType.entries.forEach { type ->
            FilterChip(
                selected = selected == type,
                onClick = { onSelected(type) },
                label = { Text(if (type == TransactionType.EXPENSE) "支出" else "收入") },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatementTableSheet(table: StatementTable, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { Text("${table.year} 年账单", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("全年")
                    Text(table.total.incomeTotal.asRmb())
                    Text(table.total.expenseTotal.asRmb())
                    Text(table.total.netIncome.asRmb())
                }
            }
            items(table.months) { month ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${month.month}月")
                    Text(month.income.asRmb())
                    Text(month.expense.asRmb())
                    Text(month.netIncome.asRmb())
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable private fun LoadingBlock() = Text("加载中…", Modifier.fillMaxWidth().padding(32.dp), textAlign = TextAlign.Center)
@Composable private fun ErrorBlock(message: String) = Text(message, color = MaterialTheme.colorScheme.error)
@Composable private fun EmptyText(message: String) = Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)

private val AnalyticsRangeMode.label: String
    get() = when (this) {
        AnalyticsRangeMode.WEEK -> "周"
        AnalyticsRangeMode.MONTH -> "月"
        AnalyticsRangeMode.YEAR -> "年"
        AnalyticsRangeMode.CUSTOM -> "范围"
    }
