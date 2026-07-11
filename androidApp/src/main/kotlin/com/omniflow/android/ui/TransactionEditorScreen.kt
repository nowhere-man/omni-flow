package com.omniflow.android.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    modifier: Modifier = Modifier,
) {
    val selectedCategory = state.categories.firstOrNull { it.id == state.categoryId }
    val primaryId = selectedCategory?.parentId ?: selectedCategory?.id
    val primaryCategories = state.categories.filter { it.parentId == null && it.type == state.type }
    val secondaryCategories = state.categories.filter { it.parentId == primaryId && it.type == state.type }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TransactionType.entries.forEach { type ->
                    FilterChip(
                        selected = state.type == type,
                        onClick = { onType(type) },
                        label = { Text(if (type == TransactionType.EXPENSE) "支出" else "收入") },
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectionMenu(
                    label = state.ledgers.firstOrNull { it.id == state.ledgerId }?.name ?: "选择账本",
                    values = state.ledgers,
                    valueLabel = Ledger::name,
                    onSelected = { onLedger(it.id) },
                    modifier = Modifier.weight(1f),
                )
                SelectionMenu(
                    label = state.accounts.firstOrNull { it.id == state.accountId }?.name ?: "选择账户",
                    values = state.accounts,
                    valueLabel = Account::name,
                    onSelected = { onAccount(it.id) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            Text("一级分类", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            CategoryGrid(primaryCategories, primaryId) { category -> onCategory(category.id) }
        }
        if (secondaryCategories.isNotEmpty()) {
            item {
                Text("二级分类", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                ChipRows(secondaryCategories, state.categoryId, Category::name) { onCategory(it.id) }
            }
        }
        if (state.tags.isNotEmpty()) {
            item {
                Text("标签", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                ChipRows(state.tags, null, { it.name }, state.selectedTagIds) { onTag(it.id) }
            }
        }
        item {
            OutlinedTextField(
                value = state.note,
                onValueChange = onNote,
                label = { Text("备注") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
        }
        item {
            DateAndExcluded(state, onDate, onExcluded)
        }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        state.amountInput.ifBlank { "0" },
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Keypad(onAmountKey)
                }
            }
        }
        state.error?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error) } }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.editingId == null) {
                    OutlinedButton(onClick = onSaveAgain, enabled = !state.isSaving, modifier = Modifier.weight(1f)) {
                        Text("保存再记")
                    }
                } else {
                    OutlinedButton(onClick = onDelete, enabled = !state.isSaving, modifier = Modifier.weight(1f)) {
                        Text("删除")
                    }
                }
                Button(onClick = onDone, enabled = !state.isSaving, modifier = Modifier.weight(1f)) {
                    Text(if (state.isSaving) "保存中…" else "完成")
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun CategoryGrid(categories: List<Category>, selectedId: String?, onSelected: (Category) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { category ->
                    FilterChip(
                        selected = selectedId == category.id,
                        onClick = { onSelected(category) },
                        label = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                SvgIcon(category.iconKey ?: "category", Modifier.size(22.dp))
                                Text(category.name, maxLines = 1)
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun <T> ChipRows(
    values: List<T>,
    selectedId: String?,
    label: (T) -> String,
    selectedIds: Set<String> = emptySet(),
    onSelected: (T) -> Unit,
) where T : Any {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        values.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { value ->
                    val id = when (value) {
                        is Category -> value.id
                        is com.omniflow.shared.domain.model.Tag -> value.id
                        else -> ""
                    }
                    FilterChip(
                        selected = selectedId == id || id in selectedIds,
                        onClick = { onSelected(value) },
                        label = { Text(label(value), maxLines = 1) },
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
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
        Text("不计入收支")
        Spacer(Modifier.width(8.dp))
        Switch(checked = state.isExcluded, onCheckedChange = onExcluded)
    }
}

@Composable
private fun Keypad(onKey: (String) -> Unit) {
    val keys = listOf(
        listOf("1", "2", "3", "+"),
        listOf("4", "5", "6", "-"),
        listOf("7", "8", "9", "⌫"),
        listOf(".", "0"),
    )
    keys.forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { key ->
                OutlinedButton(onClick = { onKey(key) }, modifier = Modifier.weight(1f)) {
                    Text(key, style = MaterialTheme.typography.titleLarge)
                }
            }
            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun <T> SelectionMenu(
    label: String,
    values: List<T>,
    valueLabel: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(label, maxLines = 1) }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            values.forEach { value ->
                DropdownMenuItem(
                    text = { Text(valueLabel(value)) },
                    onClick = { expanded = false; onSelected(value) },
                )
            }
        }
    }
}
