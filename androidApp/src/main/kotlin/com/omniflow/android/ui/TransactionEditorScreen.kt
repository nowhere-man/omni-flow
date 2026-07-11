package com.omniflow.android.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omniflow.shared.domain.model.Account
import com.omniflow.shared.domain.model.Category
import com.omniflow.shared.domain.model.Ledger
import com.omniflow.shared.domain.model.TransactionType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

@Composable
internal fun TransactionEditorScreen(
    state: TransactionEditorUiState,
    onType: (TransactionType) -> Unit,
    onLedger: (String?) -> Unit,
    onAccount: (String?) -> Unit,
    onCategory: (String?) -> Unit,
    onTag: (String) -> Unit,
    onNote: (String) -> Unit,
    onDate: (kotlinx.datetime.Instant) -> Unit,
    onExcluded: (Boolean) -> Unit,
    onAmountKey: (String) -> Unit,
    onSaveAgain: () -> Unit,
    onDone: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedCategory = state.categories.firstOrNull { it.id == state.categoryId }
    val primaryId = selectedCategory?.parentId ?: selectedCategory?.id
    val primaryCategories = state.categories.filter { it.parentId == null && it.type == state.type }
    val secondaryCategories = primaryId?.let { id ->
        state.categories.filter { it.parentId == id && it.type == state.type }
    }.orEmpty()
    val ledgerName = state.ledgers.firstOrNull { it.id == state.ledgerId }?.name ?: "选择账本"
    val accountName = state.accounts.firstOrNull { it.id == state.accountId }?.name ?: "选择账户"

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            TransactionFooter(
                state = state,
                onAmountKey = onAmountKey,
                onSaveAgain = onSaveAgain,
                onDone = onDone,
                onDelete = onDelete,
            )
        },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { Spacer(Modifier.height(2.dp)) }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("记一笔", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭记账")
                    }
                }
            }
            item { TransactionModeSwitch(state.type, onType) }
            item {
                CompactSelectionRow(
                    ledgerName = ledgerName,
                    accountName = accountName,
                    ledgers = state.ledgers,
                    accounts = state.accounts,
                    onLedger = { onLedger(it.id) },
                    onAccount = { onAccount(it.id) },
                )
            }
            item {
                Text("分类", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (primaryCategories.isEmpty()) {
                    Text("选择账本后加载分类", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    CategoryIconGrid(primaryCategories, primaryId, onCategory)
                }
            }
            if (secondaryCategories.isNotEmpty()) {
                item {
                    Text("二级分类", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    CategoryIconGrid(secondaryCategories, state.categoryId, onCategory)
                }
            }
            if (state.tags.isNotEmpty()) {
                item {
                    Text("标签", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    TagRows(state.tags, state.selectedTagIds, onTag)
                }
            }
            item {
                OutlinedTextField(
                    value = state.note,
                    onValueChange = onNote,
                    label = { Text("备注") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            item { DateAndExcluded(state, onDate, onExcluded) }
        }
    }
}

@Composable
private fun TransactionModeSwitch(
    selected: TransactionType,
    onSelected: (TransactionType) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(Modifier.padding(4.dp)) {
            TransactionType.entries.forEach { type ->
                val isSelected = type == selected
                Text(
                    if (type == TransactionType.EXPENSE) "支出" else "收入",
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        )
                        .clickable { onSelected(type) }
                        .padding(vertical = 10.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun CompactSelectionRow(
    ledgerName: String,
    accountName: String,
    ledgers: List<Ledger>,
    accounts: List<Account>,
    onLedger: (Ledger) -> Unit,
    onAccount: (Account) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            CompactMenu("账本", ledgerName, ledgers, Ledger::name, onLedger, Modifier.weight(1f))
            Spacer(
                Modifier
                    .width(1.dp)
                    .height(36.dp)
                    .background(MaterialTheme.colorScheme.outline),
            )
            CompactMenu("账户", accountName, accounts, Account::name, onAccount, Modifier.weight(1f))
        }
    }
}

@Composable
private fun <T> CompactMenu(
    label: String,
    value: String,
    values: List<T>,
    valueLabel: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        TextButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth()) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            values.forEach { valueItem ->
                DropdownMenuItem(
                    text = { Text(valueLabel(valueItem)) },
                    onClick = { expanded = false; onSelected(valueItem) },
                )
            }
        }
    }
}

@Composable
private fun CategoryIconGrid(
    categories: List<Category>,
    selectedId: String?,
    onSelected: (String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { category ->
                    val selected = selectedId == category.id
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            )
                            .clickable { onSelected(category.id) }
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Surface(
                            modifier = Modifier.size(28.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                SvgIcon(category.iconKey ?: "category")
                            }
                        }
                        Text(
                            category.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun TagRows(
    values: List<com.omniflow.shared.domain.model.Tag>,
    selectedIds: Set<String>,
    onSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        values.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { tag ->
                    FilterChip(
                        selected = tag.id in selectedIds,
                        onClick = { onSelected(tag.id) },
                        label = { Text(tag.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun DateAndExcluded(
    state: TransactionEditorUiState,
    onDate: (kotlinx.datetime.Instant) -> Unit,
    onExcluded: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val date = state.occurredAt.toLocalDateTime(ChinaTimeZone).date
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = {
                DatePickerDialog(
                    context,
                    { _, year, month, day -> onDate(LocalDate(year, month + 1, day).atStartOfDayIn(ChinaTimeZone)) },
                    date.year,
                    date.monthNumber - 1,
                    date.dayOfMonth,
                ).show()
            }) { Text(date.toString()) }
            Spacer(Modifier.weight(1f))
            Text("不计入收支", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.width(8.dp))
            Switch(checked = state.isExcluded, onCheckedChange = onExcluded)
        }
    }
}

@Composable
private fun TransactionFooter(
    state: TransactionEditorUiState,
    onAmountKey: (String) -> Unit,
    onSaveAgain: () -> Unit,
    onDone: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("金额", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                Text(
                    "¥${state.amountInput.ifBlank { "0" }}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            state.error?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Keypad(
                isSaving = state.isSaving,
                actionLabel = if (state.editingId == null) "再记" else "删除",
                onKey = onAmountKey,
                onAction = if (state.editingId == null) onSaveAgain else onDelete,
                onDone = onDone,
            )
        }
    }
}

@Composable
private fun Keypad(
    isSaving: Boolean,
    actionLabel: String,
    onKey: (String) -> Unit,
    onAction: () -> Unit,
    onDone: () -> Unit,
) {
    val keyRows = listOf(
        listOf("1", "2", "3", "+"),
        listOf("4", "5", "6", "-"),
        listOf("7", "8", "9", actionLabel),
        listOf(".", "0", "⌫", "完成"),
    )
    keyRows.forEachIndexed { rowIndex, row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { key ->
                val isDone = key == "完成"
                val isAction = rowIndex == 2 && key == actionLabel
                if (isDone) {
                    Button(
                        onClick = onDone,
                        enabled = !isSaving,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                    ) { Text(if (isSaving) "保存中" else key) }
                } else {
                    OutlinedButton(
                        onClick = if (isAction) onAction else { -> onKey(key) },
                        enabled = !isSaving,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            key,
                            style = if (isAction) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }
    }
}
