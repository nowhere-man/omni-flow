package com.omniflow.android.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.feature.batch.BatchActionBar
import com.omniflow.android.ui.feature.batch.BatchAccountPickerDialog
import com.omniflow.android.ui.feature.batch.BatchDeleteConfirmDialog
import com.omniflow.android.ui.feature.batch.BatchLedgerPickerDialog
import com.omniflow.android.ui.feature.batch.rememberBatchSelectionState
import com.omniflow.android.ui.feature.search.SearchFilterPanel
import com.omniflow.android.ui.feature.search.SearchUiState
import com.omniflow.core.domain.model.Category
import com.omniflow.core.domain.model.DateRange
import com.omniflow.core.domain.model.LedgerScope
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.TransactionType
import com.omniflow.core.domain.model.hourMinuteText

private val FieldHeight = 44.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SearchScreen(
    state: SearchUiState,
    onKeyword: (String) -> Unit,
    onScope: (LedgerScope) -> Unit,
    onType: (TransactionType?) -> Unit,
    onCategorySelected: (Category?, Category?) -> Unit,
    onTagToggle: (String) -> Unit,
    onNoteText: (String) -> Unit,
    onAccount: (String?) -> Unit,
    onAmount: (String, String) -> Unit,
    onDateRange: (DateRange?) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onEditTransaction: (String) -> Unit,
    onBatchDelete: (List<String>) -> Unit = {},
    onBatchChangeCategory: (List<String>, String) -> Unit = { _, _ -> },
    onBatchChangeAccount: (List<String>, String) -> Unit = { _, _ -> },
    onBatchChangeLedger: (List<String>, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    var filtersExpanded by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val hasInput = state.query.hasFilters ||
        state.minimumAmountText.isNotBlank() ||
        state.maximumAmountText.isNotBlank()

    val selectionState = rememberBatchSelectionState()
    val allResultIds = state.result?.items?.map { it.transaction.id }.orEmpty()
    val isAllSelected = selectionState.count == allResultIds.size && allResultIds.isNotEmpty()

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAccountPicker by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showLedgerPicker by remember { mutableStateOf(false) }

    Box(modifier.readableContentWidth().fillMaxHeight()) {
        Column(Modifier.fillMaxHeight()) {
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
                    SearchFilterPanel(
                        state = state,
                        onScope = onScope,
                        onType = onType,
                        onCategorySelected = onCategorySelected,
                        onTagToggle = onTagToggle,
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (selectionState.isActive) {
                                            selectionState.toggle(item.transaction.id)
                                        } else {
                                            onEditTransaction(item.transaction.id)
                                        }
                                    },
                                    onLongClick = {
                                        selectionState.startSelection(item.transaction.id)
                                    },
                                ),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (selectionState.isActive) {
                                    Checkbox(
                                        checked = selectionState.isSelected(item.transaction.id),
                                        onCheckedChange = { selectionState.toggle(item.transaction.id) },
                                        modifier = Modifier.padding(start = 12.dp),
                                    )
                                }
                                Box(Modifier.weight(1f).padding(horizontal = 12.dp)) {
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
                                        onClick = {
                                            if (selectionState.isActive) {
                                                selectionState.toggle(item.transaction.id)
                                            } else {
                                                onEditTransaction(item.transaction.id)
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(88.dp)) }
            }
        }

        if (selectionState.isActive) {
            BatchActionBar(
                selectedCount = selectionState.count,
                totalCount = allResultIds.size,
                isAllSelected = isAllSelected,
                onSelectAllToggle = {
                    if (isAllSelected) selectionState.deselectAll() else selectionState.selectAll(allResultIds)
                },
                onBatchDelete = { showDeleteConfirm = true },
                onBatchChangeCategory = { showCategoryPicker = true },
                onBatchChangeAccount = { showAccountPicker = true },
                onBatchChangeLedger = { showLedgerPicker = true },
                onExit = { selectionState.clear() },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    if (showDeleteConfirm) {
        BatchDeleteConfirmDialog(
            count = selectionState.count,
            onConfirm = {
                val ids = selectionState.selectedIds.toList()
                showDeleteConfirm = false
                selectionState.clear()
                onBatchDelete(ids)
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }

    if (showAccountPicker) {
        BatchAccountPickerDialog(
            accounts = state.accounts,
            onSelect = { accountId ->
                val ids = selectionState.selectedIds.toList()
                showAccountPicker = false
                selectionState.clear()
                onBatchChangeAccount(ids, accountId)
            },
            onDismiss = { showAccountPicker = false },
        )
    }

    if (showLedgerPicker) {
        BatchLedgerPickerDialog(
            ledgers = state.ledgers,
            onSelect = { ledgerId ->
                val ids = selectionState.selectedIds.toList()
                showLedgerPicker = false
                selectionState.clear()
                onBatchChangeLedger(ids, ledgerId)
            },
            onDismiss = { showLedgerPicker = false },
        )
    }

    if (showCategoryPicker) {
        CategoryPickerSheet(
            title = "批量修改分类",
            categories = state.categories,
            selectedCategoryId = null,
            initialType = TransactionType.EXPENSE,
            onDismiss = { showCategoryPicker = false },
            onSelected = { categoryId, _ ->
                val ids = selectionState.selectedIds.toList()
                showCategoryPicker = false
                selectionState.clear()
                onBatchChangeCategory(ids, categoryId)
            },
        )
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
private fun SearchSummary(count: Int, income: Money, expense: Money) {
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
