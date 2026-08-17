package com.omniflow.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omniflow.core.domain.model.Account
import com.omniflow.core.domain.model.DateRange
import com.omniflow.core.domain.model.Ledger
import com.omniflow.core.domain.model.LedgerScope
import com.omniflow.core.domain.model.hourMinuteText
import com.omniflow.core.domain.model.TransactionType
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

/** 所有筛选控件统一的高度，和分段控件、账本药丸对齐。 */
private val FieldHeight = 44.dp

@Composable
internal fun SearchScreen(
    state: SearchUiState,
    onKeyword: (String) -> Unit,
    onScope: (LedgerScope) -> Unit,
    onType: (TransactionType?) -> Unit,
    onPrimaryCategoryText: (String) -> Unit,
    onSecondaryCategoryText: (String) -> Unit,
    onTagText: (String) -> Unit,
    onNoteText: (String) -> Unit,
    onAccount: (String?) -> Unit,
    onAmount: (String, String) -> Unit,
    onDateRange: (DateRange?) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onEditTransaction: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var filtersExpanded by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val hasInput = state.query.hasFilters ||
        state.minimumAmountText.isNotBlank() ||
        state.maximumAmountText.isNotBlank()
    Column(modifier.readableContentWidth().fillMaxHeight()) {
        Column(
            Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            KeywordField(state.query.keyword, onKeyword, onSearch = {
                focusManager.clearFocus()
                onSearch()
            })
            Row(
                Modifier.fillMaxWidth().clickable(role = Role.Button) { filtersExpanded = !filtersExpanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (filtersExpanded) "收起筛选" else "展开筛选",
                    style = OmniText.caption,
                    color = mutedContent(),
                )
                Icon(
                    if (filtersExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = mutedContent(),
                )
            }
            if (filtersExpanded) {
                FilterPanel(
                    state = state,
                    onScope = onScope,
                    onType = onType,
                    onPrimaryCategoryText = onPrimaryCategoryText,
                    onSecondaryCategoryText = onSecondaryCategoryText,
                    onTagText = onTagText,
                    onNoteText = onNoteText,
                    onAccount = onAccount,
                    onAmount = onAmount,
                    onDateRange = onDateRange,
                )
            }
            // 两个动作永远露在外面：收起筛选之后也要能改关键词再搜一次
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onClear,
                    enabled = hasInput || state.result != null,
                    modifier = Modifier.weight(1f).height(FieldHeight),
                    shape = RoundedCornerShape(OmniRadius.small),
                ) { Text("清除") }
                Button(
                    onClick = { focusManager.clearFocus(); onSearch() },
                    enabled = hasInput,
                    modifier = Modifier.weight(1f).height(FieldHeight),
                    shape = RoundedCornerShape(OmniRadius.small),
                ) { Text("搜索") }
            }
        }
        LazyColumn(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { Spacer(Modifier.height(2.dp)) }
            state.error?.let { error ->
                item { Text(error, color = MaterialTheme.colorScheme.error, style = OmniText.caption) }
            }
            if (state.isLoading) {
                item {
                    Text(
                        "搜索中…",
                        Modifier.fillMaxWidth().padding(24.dp),
                        style = OmniText.caption,
                        color = mutedContent(),
                    )
                }
            }
            if (!state.isLoading && state.error == null && state.result == null) {
                item { SearchHint("输入关键词或选择筛选条件，点「搜索」开始") }
            }
            state.result?.let { result ->
                item { SearchSummary(result.items.size, result.summary.incomeTotal, result.summary.expenseTotal) }
                if (result.items.isEmpty()) item { SearchHint("没有符合条件的交易") }
                items(result.items, key = { it.transaction.id }) { item ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(OmniRadius.medium),
                        color = surfaceCard(),
                    ) {
                        Box(Modifier.padding(horizontal = 12.dp)) {
                            TransactionRow(
                                iconKey = item.transaction.categoryIconKey,
                                title = item.transaction.categoryDisplayName,
                                subtitle = listOfNotNull(
                                    item.transaction.note?.takeIf(String::isNotBlank),
                                    item.tags.takeIf { it.isNotEmpty() }?.joinToString(" ") { "#${it.name}" },
                                    item.transaction.accountName,
                                ).joinToString(" · "),
                                amount = item.transaction.amount,
                                type = item.transaction.type,
                                timeText = item.transaction.occurredAt.hourMinuteText(),
                                dimmed = item.transaction.isExcluded,
                                onClick = { onEditTransaction(item.transaction.id) },
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(88.dp)) }
        }
    }
}

@Composable
private fun FilterPanel(
    state: SearchUiState,
    onScope: (LedgerScope) -> Unit,
    onType: (TransactionType?) -> Unit,
    onPrimaryCategoryText: (String) -> Unit,
    onSecondaryCategoryText: (String) -> Unit,
    onTagText: (String) -> Unit,
    onNoteText: (String) -> Unit,
    onAccount: (String?) -> Unit,
    onAmount: (String, String) -> Unit,
    onDateRange: (DateRange?) -> Unit,
) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(OmniRadius.medium),
        color = surfaceCard(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // 和统计页的周/月/年/范围同一个控件，不再自己写一套三段按钮
            OmniSegmented(
                options = SearchTypeOptions,
                selected = state.query.type,
                label = {
                    when (it) {
                        null -> "全部"
                        TransactionType.EXPENSE -> "支出"
                        TransactionType.INCOME -> "收入"
                    }
                },
                onSelected = onType,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterMenu(
                    label = when (val scope = state.query.scope) {
                        LedgerScope.All -> "所有账本"
                        is LedgerScope.Single -> state.ledgers.firstOrNull { it.id == scope.ledgerId }?.name ?: "账本"
                    },
                    allLabel = "所有账本",
                    values = state.ledgers,
                    valueLabel = Ledger::name,
                    onAll = { onScope(LedgerScope.All) },
                    onSelected = { onScope(LedgerScope.Single(it.id)) },
                    modifier = Modifier.weight(1f),
                )
                FilterMenu(
                    label = state.accounts.firstOrNull { it.id == state.query.accountId }?.name ?: "所有账户",
                    allLabel = "所有账户",
                    values = state.accounts,
                    valueLabel = Account::name,
                    onAll = { onAccount(null) },
                    onSelected = { onAccount(it.id) },
                    modifier = Modifier.weight(1f),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterField("一级分类", state.query.primaryCategoryText, onPrimaryCategoryText, Modifier.weight(1f))
                FilterField("二级分类", state.query.secondaryCategoryText, onSecondaryCategoryText, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterField("标签", state.query.tagText, onTagText, Modifier.weight(1f))
                FilterField("备注", state.query.noteText, onNoteText, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterField(
                    "最低金额",
                    state.minimumAmountText,
                    { onAmount(it, state.maximumAmountText) },
                    Modifier.weight(1f),
                    KeyboardType.Decimal,
                )
                FilterField(
                    "最高金额",
                    state.maximumAmountText,
                    { onAmount(state.minimumAmountText, it) },
                    Modifier.weight(1f),
                    KeyboardType.Decimal,
                )
            }
            DateFilterRow(state.query.dateRange, onDateRange)
        }
    }
}

/** 所有文字筛选框长一个样：44dp 高、小圆角、内嵌底色，不带 Material 的浮动标签。 */
@Composable
private fun FilterField(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Surface(
        modifier = modifier.height(FieldHeight),
        shape = RoundedCornerShape(OmniRadius.small),
        color = surfaceInset(),
    ) {
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(label, style = OmniText.bodyRow, color = mutedContent(), maxLines = 1)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValue,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = OmniText.bodyRow.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                )
            }
        }
    }
}

@Composable
private fun <T> FilterMenu(
    label: String,
    allLabel: String,
    values: List<T>,
    valueLabel: (T) -> String,
    onAll: () -> Unit,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        Surface(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().height(FieldHeight),
            shape = RoundedCornerShape(OmniRadius.small),
            color = surfaceInset(),
        ) {
            Row(Modifier.padding(start = 12.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    style = OmniText.bodyRow,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(Icons.Default.ExpandMore, null, Modifier.size(18.dp), tint = mutedContent())
            }
        }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(allLabel) }, onClick = { expanded = false; onAll() })
            values.forEach { value ->
                DropdownMenuItem(text = { Text(valueLabel(value)) }, onClick = { expanded = false; onSelected(value) })
            }
        }
    }
}

@Composable
private fun KeywordField(value: String, onValue: (String) -> Unit, onSearch: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = CircleShape,
        color = surfaceInset(),
    ) {
        Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Search, null, Modifier.size(20.dp), tint = mutedContent())
            Spacer(Modifier.size(10.dp))
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text("关键词、分类、账户或标签", style = OmniText.bodyRow, color = mutedContent(), maxLines = 1)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValue,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = OmniText.bodyRow.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { onSearch() }),
                )
            }
            if (value.isNotEmpty()) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "清除关键词",
                    modifier = Modifier.size(18.dp).clickable { onValue("") },
                    tint = mutedContent(),
                )
            }
        }
    }
}

@Composable
private fun DateFilterRow(range: DateRange?, onRange: (DateRange?) -> Unit) {
    val context = LocalContext.current
    val initialStart = range?.startInclusive?.toLocalDateTime(ChinaTimeZone)?.date
    val initialEnd = range?.endExclusive?.let { Instant.fromEpochMilliseconds(it.toEpochMilliseconds() - 1) }
        ?.toLocalDateTime(ChinaTimeZone)?.date
    var start by remember(range) { mutableStateOf(initialStart) }
    var end by remember(range) { mutableStateOf(initialEnd) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DateFilterButton(start?.toString() ?: "开始日期", Modifier.weight(1f)) {
            showSearchDatePicker(context, start ?: Clock.System.now().toLocalDateTime(ChinaTimeZone).date) { selected ->
                start = selected
                onRange(dateRangeOrNull(start, end))
            }
        }
        DateFilterButton(end?.toString() ?: "结束日期", Modifier.weight(1f)) {
            showSearchDatePicker(context, end ?: start ?: Clock.System.now().toLocalDateTime(ChinaTimeZone).date) { selected ->
                end = selected
                onRange(dateRangeOrNull(start, end))
            }
        }
        if (range != null || start != null || end != null) {
            TextButton(onClick = { start = null; end = null; onRange(null) }) {
                Text("清除", style = OmniText.caption)
            }
        }
    }
}

@Composable
private fun DateFilterButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(FieldHeight),
        shape = RoundedCornerShape(OmniRadius.small),
        color = surfaceInset(),
    ) {
        Box(Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
            Text(label, style = OmniText.bodyRow, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SearchSummary(count: Int, income: com.omniflow.core.domain.model.Money, expense: com.omniflow.core.domain.model.Money) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(OmniRadius.medium),
        color = surfaceCard(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("${count.grouped()} 笔", Modifier.weight(1f), style = OmniText.caption, color = mutedContent())
            Text("收 ${income.asPlainAmount()}", style = OmniText.caption, color = incomeColor())
            Spacer(Modifier.size(12.dp))
            Text("支 ${expense.asPlainAmount()}", style = OmniText.caption, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun SearchHint(message: String) {
    Text(
        message,
        modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
        style = OmniText.caption,
        color = mutedContent(),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

private val SearchTypeOptions = listOf(null, TransactionType.EXPENSE, TransactionType.INCOME)

private fun showSearchDatePicker(context: android.content.Context, date: LocalDate, onDate: (LocalDate) -> Unit) {
    android.app.DatePickerDialog(
        context,
        { _, year, month, day -> onDate(LocalDate(year, month + 1, day)) },
        date.year,
        date.monthNumber - 1,
        date.dayOfMonth,
    ).show()
}

private fun dateRangeOrNull(first: LocalDate?, second: LocalDate?): DateRange? {
    if (first == null || second == null) return null
    val start = minOf(first, second)
    val end = maxOf(first, second)
    val next = java.time.LocalDate.of(end.year, end.monthNumber, end.dayOfMonth).plusDays(1)
    return DateRange(
        start.atStartOfDayIn(ChinaTimeZone),
        LocalDate(next.year, next.monthValue, next.dayOfMonth).atStartOfDayIn(ChinaTimeZone),
    )
}
