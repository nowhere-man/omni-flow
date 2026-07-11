package com.omniflow.android.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.omniflow.shared.domain.model.Account
import com.omniflow.shared.domain.model.Category
import com.omniflow.shared.domain.model.DateRange
import com.omniflow.shared.domain.model.Ledger
import com.omniflow.shared.domain.model.LedgerScope
import com.omniflow.shared.domain.model.Tag
import com.omniflow.shared.domain.model.TransactionType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

private enum class AmountMode { NONE, EXACT, RANGE }

@Composable
internal fun SearchScreen(
    state: SearchUiState,
    onKeyword: (String) -> Unit,
    onScope: (LedgerScope) -> Unit,
    onType: (TransactionType?) -> Unit,
    onPrimaryCategory: (String?) -> Unit,
    onSecondaryCategory: (String?) -> Unit,
    onTag: (String?) -> Unit,
    onAccount: (String?) -> Unit,
    onAmount: (String, String, String) -> Unit,
    onDateRange: (DateRange?) -> Unit,
    onToggleAdvanced: () -> Unit,
    onClear: () -> Unit,
    onEditTransaction: (String) -> Unit,
    onAddTransaction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var amountMode by remember { mutableStateOf(AmountMode.NONE) }
    var exact by remember { mutableStateOf("") }
    var minimum by remember { mutableStateOf("") }
    var maximum by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf<LocalDate?>(null) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    val primaryCategories = state.categories.filter { it.parentId == null }
    val secondaryCategories = state.categories.filter { it.parentId == state.query.primaryCategoryId }
    fun clearFilters() {
        amountMode = AmountMode.NONE
        exact = ""
        minimum = ""
        maximum = ""
        startDate = null
        endDate = null
        onClear()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        item { Text("搜索", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold) }
        item {
            OutlinedTextField(
                value = state.query.keyword,
                onValueChange = onKeyword,
                label = { Text("关键词：备注、分类、账户或标签") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onToggleAdvanced) { Text(if (state.isAdvancedExpanded) "收起高级筛选" else "展开高级筛选") }
                TextButton(onClick = ::clearFilters) { Text("清除") }
            }
        }
        if (state.isAdvancedExpanded) {
            item {
                FilterCard("账本与类型") {
                    ValueMenu(
                        label = when (val scope = state.query.scope) {
                            LedgerScope.All -> "所有账本"
                            is LedgerScope.Single -> state.ledgers.firstOrNull { it.id == scope.ledgerId }?.name ?: "账本"
                        },
                        allLabel = "所有账本",
                        values = state.ledgers,
                        valueLabel = Ledger::name,
                        onAll = { onScope(LedgerScope.All) },
                        onSelected = { onScope(LedgerScope.Single(it.id)) },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(null, TransactionType.EXPENSE, TransactionType.INCOME).forEach { type ->
                            FilterChip(
                                selected = state.query.type == type,
                                onClick = { onType(type) },
                                label = { Text(type?.let { if (it == TransactionType.EXPENSE) "支出" else "收入" } ?: "不限") },
                            )
                        }
                    }
                }
            }
            item {
                FilterCard("分类、标签与账户") {
                    ValueMenu(
                        label = primaryCategories.firstOrNull { it.id == state.query.primaryCategoryId }?.name ?: "不限一级分类",
                        allLabel = "不限一级分类",
                        values = primaryCategories,
                        valueLabel = Category::name,
                        onAll = { onPrimaryCategory(null) },
                        onSelected = { onPrimaryCategory(it.id) },
                    )
                    if (secondaryCategories.isNotEmpty()) {
                        ValueMenu(
                            label = secondaryCategories.firstOrNull { it.id == state.query.secondaryCategoryId }?.name ?: "不限二级分类",
                            allLabel = "不限二级分类",
                            values = secondaryCategories,
                            valueLabel = Category::name,
                            onAll = { onSecondaryCategory(null) },
                            onSelected = { onSecondaryCategory(it.id) },
                        )
                    }
                    ValueMenu(
                        label = state.tags.firstOrNull { it.id == state.query.tagId }?.name ?: "不限标签",
                        allLabel = "不限标签",
                        values = state.tags,
                        valueLabel = Tag::name,
                        onAll = { onTag(null) },
                        onSelected = { onTag(it.id) },
                    )
                    ValueMenu(
                        label = state.accounts.firstOrNull { it.id == state.query.accountId }?.name ?: "不限账户",
                        allLabel = "不限账户",
                        values = state.accounts,
                        valueLabel = Account::name,
                        onAll = { onAccount(null) },
                        onSelected = { onAccount(it.id) },
                    )
                }
            }
            item {
                FilterCard("金额") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AmountMode.entries.forEach { mode ->
                            FilterChip(
                                selected = amountMode == mode,
                                onClick = {
                                    amountMode = mode
                                    if (mode != AmountMode.EXACT) exact = ""
                                    if (mode != AmountMode.RANGE) { minimum = ""; maximum = "" }
                                    onAmount(exact, minimum, maximum)
                                },
                                label = { Text(when (mode) { AmountMode.NONE -> "不限"; AmountMode.EXACT -> "精确"; AmountMode.RANGE -> "范围" }) },
                            )
                        }
                    }
                    if (amountMode == AmountMode.EXACT) {
                        MoneyField("精确金额", exact) { exact = it; onAmount(exact, "", "") }
                    }
                    if (amountMode == AmountMode.RANGE) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MoneyField("最小金额", minimum, Modifier.weight(1f)) { minimum = it; onAmount("", minimum, maximum) }
                            MoneyField("最大金额", maximum, Modifier.weight(1f)) { maximum = it; onAmount("", minimum, maximum) }
                        }
                    }
                }
            }
            item {
                FilterCard("日期") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DateButton("开始", startDate, Modifier.weight(1f)) { date ->
                            startDate = date
                            updateDateRange(startDate, endDate, onDateRange)
                        }
                        DateButton("结束", endDate, Modifier.weight(1f)) { date ->
                            endDate = date
                            updateDateRange(startDate, endDate, onDateRange)
                        }
                    }
                    TextButton(onClick = { startDate = null; endDate = null; onDateRange(null) }) { Text("不限日期") }
                }
            }
        }
        state.error?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error) } }
        if (state.isLoading) item { Text("搜索中…", Modifier.fillMaxWidth().padding(24.dp)) }
        if (!state.isLoading && state.error == null && state.result == null) {
            item { SearchEmptyState(hasFilters = false, onClear = ::clearFilters, onAddTransaction = onAddTransaction) }
        }
        state.result?.let { result ->
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("收入 ${result.summary.incomeTotal.asRmb()}")
                        Text("支出 ${result.summary.expenseTotal.asRmb()}")
                    }
                }
            }
            if (result.items.isEmpty()) item {
                SearchEmptyState(
                    hasFilters = state.query.hasFilters,
                    onClear = ::clearFilters,
                    onAddTransaction = onAddTransaction,
                )
            }
            items(result.items, key = { it.transaction.id }) { item ->
                Card(
                    Modifier.fillMaxWidth().clickable { onEditTransaction(item.transaction.id) },
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(item.transaction.categoryName, fontWeight = FontWeight.SemiBold)
                            Text(item.transaction.amount.asRmb())
                        }
                        Text("${item.transaction.ledgerName} · ${item.transaction.accountName}", style = MaterialTheme.typography.bodySmall)
                        item.transaction.note?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        if (item.tags.isNotEmpty()) Text(item.tags.joinToString(" · ") { it.name }, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun FilterCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun SearchEmptyState(
    hasFilters: Boolean,
    onClear: () -> Unit,
    onAddTransaction: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(if (hasFilters) "没有符合条件的交易" else "还没有可搜索的交易", fontWeight = FontWeight.SemiBold)
        Text(
            if (hasFilters) "调整筛选条件，或清除筛选后再试。" else "新增一笔交易后，可按备注、分类或账户检索。",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        if (hasFilters) {
            TextButton(onClick = onClear) { Text("清除筛选") }
        } else {
            Button(onClick = onAddTransaction) { Text("新增交易") }
        }
    }
}

@Composable
private fun MoneyField(label: String, value: String, modifier: Modifier = Modifier, onValue: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) onValue(it) },
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}

@Composable
private fun DateButton(label: String, date: LocalDate?, modifier: Modifier, onDate: (LocalDate) -> Unit) {
    val context = LocalContext.current
    val initial = date ?: kotlinx.datetime.Clock.System.now().let { it.toLocalDateTime(ChinaTimeZone).date }
    OutlinedButton(
        onClick = {
            DatePickerDialog(
                context,
                { _, year, month, day -> onDate(LocalDate(year, month + 1, day)) },
                initial.year,
                initial.monthNumber - 1,
                initial.dayOfMonth,
            ).show()
        },
        modifier = modifier,
    ) { Text(date?.toString() ?: label) }
}

private fun updateDateRange(start: LocalDate?, end: LocalDate?, onDateRange: (DateRange?) -> Unit) {
    if (start == null && end == null) return onDateRange(null)
    val startValue = start ?: end ?: return
    val endValue = end ?: startValue
    val orderedStart = minOf(startValue, endValue)
    val orderedEnd = maxOf(startValue, endValue)
    onDateRange(
        DateRange(
            orderedStart.atStartOfDayIn(ChinaTimeZone),
            java.time.LocalDate.of(orderedEnd.year, orderedEnd.monthNumber, orderedEnd.dayOfMonth)
                .plusDays(1)
                .let { LocalDate(it.year, it.monthValue, it.dayOfMonth) }
                .atStartOfDayIn(ChinaTimeZone),
        ),
    )
}

@Composable
private fun <T> ValueMenu(
    label: String,
    allLabel: String,
    values: List<T>,
    valueLabel: (T) -> String,
    onAll: () -> Unit,
    onSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(label) }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(allLabel) }, onClick = { expanded = false; onAll() })
            values.forEach { value ->
                DropdownMenuItem(text = { Text(valueLabel(value)) }, onClick = { expanded = false; onSelected(value) })
            }
        }
    }
}
