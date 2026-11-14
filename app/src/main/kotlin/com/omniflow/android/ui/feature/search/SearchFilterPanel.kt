package com.omniflow.android.ui.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.OmniRadius
import com.omniflow.android.ui.OmniSegmented
import com.omniflow.android.ui.OmniText
import com.omniflow.android.ui.designsystem.components.OmniDatePickerDialog
import com.omniflow.android.ui.designsystem.tokens.categoryBadgeColors
import com.omniflow.android.ui.designsystem.tokens.getCategoryIcon
import com.omniflow.android.ui.mutedContent
import com.omniflow.android.ui.surfaceCard
import com.omniflow.android.ui.surfaceInset
import com.omniflow.core.domain.model.Account
import com.omniflow.core.domain.model.AppTimeZone
import com.omniflow.core.domain.model.Category
import com.omniflow.core.domain.model.DateRange
import com.omniflow.core.domain.model.Ledger
import com.omniflow.core.domain.model.LedgerScope
import com.omniflow.core.domain.model.TransactionType
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime

private val FieldHeight = 44.dp
private val SearchTypeOptions = listOf(null, TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.TRANSFER)

@Composable
fun SearchFilterPanel(
    state: SearchUiState,
    onScope: (LedgerScope) -> Unit,
    onType: (TransactionType?) -> Unit,
    onCategorySelected: (primary: Category?, secondary: Category?) -> Unit,
    onTagToggle: (String) -> Unit,
    onNoteText: (String) -> Unit,
    onAccount: (String?) -> Unit,
    onAmount: (String, String) -> Unit,
    onDateRange: (DateRange?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCategoryPicker by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(OmniRadius.medium),
        color = surfaceCard(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 类型选择
            OmniSegmented(
                options = SearchTypeOptions,
                selected = state.query.type,
                label = {
                    when (it) {
                        null -> "全部"
                        TransactionType.EXPENSE -> "支出"
                        TransactionType.INCOME -> "收入"
                        TransactionType.TRANSFER -> "转账"
                    }
                },
                onSelected = onType,
                modifier = Modifier.fillMaxWidth(),
            )

            // 账本与账户
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

            // 级联分类选择器
            val categoryLabel = when {
                state.query.primaryCategoryText.isBlank() -> "所有分类"
                state.query.secondaryCategoryText.isBlank() -> state.query.primaryCategoryText
                else -> "${state.query.primaryCategoryText} - ${state.query.secondaryCategoryText}"
            }
            val hasSelectedCategory = state.query.primaryCategoryText.isNotBlank()
            val categoryIconKey = state.categories.firstOrNull {
                it.parentId == null && it.name == state.query.primaryCategoryText
            }?.iconKey

            Surface(
                onClick = { showCategoryPicker = true },
                modifier = Modifier.fillMaxWidth().height(FieldHeight),
                shape = RoundedCornerShape(OmniRadius.small),
                color = surfaceInset(),
            ) {
                Row(
                    modifier = Modifier.padding(start = 12.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (categoryIconKey != null) {
                        Icon(
                            imageVector = getCategoryIcon(categoryIconKey),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = categoryBadgeColors(categoryIconKey).content,
                        )
                        Spacer(Modifier.width(8.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = mutedContent(),
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = categoryLabel,
                        modifier = Modifier.weight(1f),
                        style = OmniText.bodyRow,
                        color = if (hasSelectedCategory) MaterialTheme.colorScheme.onSurface else mutedContent(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (hasSelectedCategory) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "清除分类",
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onCategorySelected(null, null) },
                            tint = mutedContent(),
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = mutedContent(),
                    )
                }
            }

            // 标签多选 Chips
            if (state.tags.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "标签:",
                        style = OmniText.caption,
                        color = mutedContent(),
                        modifier = Modifier.padding(end = 2.dp),
                    )
                    state.tags.forEach { tag ->
                        val isSelected = tag.name in state.query.tagNames ||
                            (state.query.tagText.isNotBlank() && tag.name == state.query.tagText)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onTagToggle(tag.name) },
                            label = { Text("#${tag.name}", maxLines = 1) },
                        )
                    }
                }
            }

            // 备注
            FilterField("备注", state.query.noteText, onNoteText, Modifier.fillMaxWidth())

            // 最低与最高金额
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

            // 日期范围
            DateFilterRow(state.query.dateRange, onDateRange)
        }
    }

    if (showCategoryPicker) {
        SearchCategoryPickerSheet(
            categories = state.categories,
            initialType = state.query.type,
            selectedPrimaryName = state.query.primaryCategoryText,
            selectedSecondaryName = state.query.secondaryCategoryText,
            onDismiss = { showCategoryPicker = false },
            onSelected = { primary, secondary ->
                onCategorySelected(primary, secondary)
            },
        )
    }
}

@Composable
fun FilterField(
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
fun <T> FilterMenu(
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
private fun DateFilterRow(range: DateRange?, onRange: (DateRange?) -> Unit) {
    val initialStart = range?.startLocalDate(AppTimeZone)
    val initialEnd = range?.endLocalDateInclusive(AppTimeZone)
    var start by remember(range) { mutableStateOf(initialStart) }
    var end by remember(range) { mutableStateOf(initialEnd) }
    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DateFilterButton(start?.toString() ?: "开始日期", Modifier.weight(1f)) {
            pickingStart = true
        }
        DateFilterButton(end?.toString() ?: "结束日期", Modifier.weight(1f)) {
            pickingEnd = true
        }
        if (range != null || start != null || end != null) {
            TextButton(onClick = { start = null; end = null; onRange(null) }) {
                Text("清除", style = OmniText.caption)
            }
        }
    }

    if (pickingStart) {
        OmniDatePickerDialog(
            initialDate = start ?: Clock.System.now().toLocalDateTime(AppTimeZone).date,
            onDateSelected = { selected ->
                start = selected
                onRange(dateRangeOrNull(start, end))
            },
            onDismissRequest = { pickingStart = false },
        )
    }

    if (pickingEnd) {
        OmniDatePickerDialog(
            initialDate = end ?: start ?: Clock.System.now().toLocalDateTime(AppTimeZone).date,
            onDateSelected = { selected ->
                end = selected
                onRange(dateRangeOrNull(start, end))
            },
            onDismissRequest = { pickingEnd = false },
        )
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

private fun dateRangeOrNull(first: LocalDate?, second: LocalDate?): DateRange? {
    if (first == null || second == null) return null
    return DateRange.ofClosedDates(first, second, AppTimeZone)
}
