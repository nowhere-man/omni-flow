package com.omniflow.android.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omniflow.shared.domain.model.Account
import com.omniflow.shared.domain.model.Category
import com.omniflow.shared.domain.model.Ledger
import com.omniflow.shared.domain.model.TransactionType
import kotlin.math.abs
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

@Composable
internal fun TransactionEditorScreen(
    state: TransactionEditorUiState,
    onType: (TransactionType) -> Unit,
    onLedger: (String?) -> Unit,
    onAccount: (String?) -> Unit,
    onCategory: (String?) -> Unit,
    onReorderPrimary: (List<String>) -> Unit,
    onCreateSecondary: (String) -> Unit,
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
    val secondaryCategories = state.categories.filter { it.parentId == primaryId && it.type == state.type }
    val ledgerName = state.ledgers.firstOrNull { it.id == state.ledgerId }?.name ?: "选择账本"
    val accountName = state.accounts.firstOrNull { it.id == state.accountId }?.name ?: "选择账户"
    var reorderMode by remember { mutableStateOf(false) }
    var showSecondaryDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            TransactionFooter(state, onAmountKey, onSaveAgain, onDone, onDelete)
        },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(contentPadding).padding(horizontal = 12.dp),
            contentPadding = PaddingValues(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("记一笔", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "关闭记账") }
                }
            }
            item { TransactionModeSwitch(state.type, onType) }
            item {
                CompactSelectionRow(
                    ledgerName,
                    accountName,
                    state.ledgers,
                    state.accounts,
                    onLedger = { onLedger(it.id) },
                    onAccount = { onAccount(it.id) },
                )
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("分类", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { reorderMode = !reorderMode }) {
                        Icon(Icons.Default.DragIndicator, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (reorderMode) "完成排序" else "调整顺序")
                    }
                }
                if (primaryCategories.isEmpty()) {
                    Text("选择账本后加载分类", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    ReorderableCategoryGrid(
                        primaryCategories,
                        primaryId,
                        reorderMode,
                        onCategory,
                        onReorderPrimary,
                    )
                }
            }
            if (primaryId != null) {
                item {
                    SecondaryCategoryRow(
                        secondaryCategories,
                        state.categoryId,
                        onCategory,
                        onAdd = { showSecondaryDialog = true },
                    )
                }
            }
            if (state.tags.isNotEmpty()) {
                item { CompactTagRow(state.tags, state.selectedTagIds, onTag) }
            }
            item {
                CompactEntryDetails(state, onNote, onDate, onExcluded)
            }
        }
    }

    if (showSecondaryDialog) {
        NewSecondaryCategoryDialog(
            primaryName = state.categories.firstOrNull { it.id == primaryId }?.name.orEmpty(),
            onDismiss = { showSecondaryDialog = false },
            onCreate = { name ->
                onCreateSecondary(name)
                showSecondaryDialog = false
            },
        )
    }
}

@Composable
private fun TransactionModeSwitch(selected: TransactionType, onSelected: (TransactionType) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(Modifier.padding(3.dp)) {
            TransactionType.entries.forEach { type ->
                val isSelected = type == selected
                Text(
                    if (type == TransactionType.EXPENSE) "支出" else "收入",
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { onSelected(type) }
                        .padding(vertical = 8.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
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
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CompactMenu("账本", ledgerName, ledgers, Ledger::name, onLedger, Modifier.weight(1f))
            Spacer(Modifier.width(1.dp).height(28.dp).background(MaterialTheme.colorScheme.outlineVariant))
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
        TextButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth().height(44.dp)) {
            Text("$label · ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            values.forEach { item ->
                DropdownMenuItem(
                    text = { Text(valueLabel(item)) },
                    onClick = { expanded = false; onSelected(item) },
                )
            }
        }
    }
}

@Composable
private fun ReorderableCategoryGrid(
    categories: List<Category>,
    selectedId: String?,
    reorderMode: Boolean,
    onSelected: (String?) -> Unit,
    onReordered: (List<String>) -> Unit,
) {
    val columns = 6
    val density = LocalDensity.current
    val rowStep = with(density) { 62.dp.toPx() }
    var width by remember { mutableIntStateOf(0) }
    var ordered by remember(categories) { mutableStateOf(categories) }
    var draggedId by remember { mutableStateOf<String?>(null) }
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }
    var changed by remember { mutableStateOf(false) }
    val dragModifier = if (!reorderMode) Modifier else Modifier.pointerInput(width, categories) {
        detectDragGesturesAfterLongPress(
            onDragStart = { offset ->
                if (width > 0) {
                    val column = (offset.x / (width.toFloat() / columns)).toInt().coerceIn(0, columns - 1)
                    val row = (offset.y / rowStep).toInt().coerceAtLeast(0)
                    draggedId = ordered.getOrNull(row * columns + column)?.id
                }
            },
            onDragCancel = { draggedId = null; dragX = 0f; dragY = 0f },
            onDragEnd = {
                if (changed) onReordered(ordered.map(Category::id))
                draggedId = null
                dragX = 0f
                dragY = 0f
                changed = false
            },
            onDrag = { change, amount ->
                change.consume()
                val id = draggedId ?: return@detectDragGesturesAfterLongPress
                val currentIndex = ordered.indexOfFirst { it.id == id }
                if (currentIndex < 0 || width == 0) return@detectDragGesturesAfterLongPress
                dragX += amount.x
                dragY += amount.y
                val cellWidth = width.toFloat() / columns
                val offset = when {
                    abs(dragY) > rowStep / 2f -> if (dragY > 0) columns else -columns
                    abs(dragX) > cellWidth / 2f -> if (dragX > 0) 1 else -1
                    else -> 0
                }
                if (offset != 0) {
                    val target = (currentIndex + offset).coerceIn(0, ordered.lastIndex)
                    if (target != currentIndex) {
                        ordered = ordered.toMutableList().apply { add(target, removeAt(currentIndex)) }
                        changed = true
                    }
                    dragX = 0f
                    dragY = 0f
                }
            },
        )
    }

    Column(
        modifier = dragModifier.fillMaxWidth().onSizeChanged { width = it.width },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ordered.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { category ->
                    CategoryTile(
                        category,
                        selected = selectedId == category.id,
                        dragging = draggedId == category.id,
                        reorderMode = reorderMode,
                        onClick = { onSelected(category.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun CategoryTile(
    category: Category,
    selected: Boolean,
    dragging: Boolean,
    reorderMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(58.dp).alpha(if (dragging) 0.55f else 1f),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
        onClick = onClick,
        enabled = !reorderMode,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                SvgIcon(colorCategoryIconKey(category.iconKey), Modifier.size(32.dp))
                Text(
                    category.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                )
            }
            if (reorderMode) {
                Icon(
                    Icons.Default.DragIndicator,
                    contentDescription = "拖动${category.name}调整顺序",
                    modifier = Modifier.align(Alignment.TopEnd).size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun SecondaryCategoryRow(
    categories: List<Category>,
    selectedId: String?,
    onSelected: (String?) -> Unit,
    onAdd: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("二级", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        categories.forEach { category ->
            FilterChip(
                selected = category.id == selectedId,
                onClick = { onSelected(category.id) },
                label = { Text(category.name, maxLines = 1) },
            )
        }
        Surface(onClick = onAdd, shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
            Icon(Icons.Default.Add, contentDescription = "新建二级分类", modifier = Modifier.padding(10.dp).size(18.dp))
        }
    }
}

@Composable
private fun CompactTagRow(
    tags: List<com.omniflow.shared.domain.model.Tag>,
    selectedIds: Set<String>,
    onSelected: (String) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("标签", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        tags.forEach { tag ->
            FilterChip(
                selected = tag.id in selectedIds,
                onClick = { onSelected(tag.id) },
                label = { Text(tag.name, maxLines = 1) },
            )
        }
    }
}

@Composable
private fun CompactEntryDetails(
    state: TransactionEditorUiState,
    onNote: (String) -> Unit,
    onDate: (kotlinx.datetime.Instant) -> Unit,
    onExcluded: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val dateTime = state.occurredAt.toLocalDateTime(ChinaTimeZone)
    val today = Clock.System.now().toLocalDateTime(ChinaTimeZone).date
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(
                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.weight(1f)) {
                        if (state.note.isBlank()) {
                            Text("添加备注", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        BasicTextField(
                            value = state.note,
                            onValueChange = onNote,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface),
                        )
                    }
                }
            }
            Surface(
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            onDate(LocalDateTime(dateTime.date, LocalTime(hour, minute)).toInstant(ChinaTimeZone))
                        },
                        dateTime.hour,
                        dateTime.minute,
                        true,
                    ).show()
                },
                modifier = Modifier.height(44.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(12.dp),
            ) {
                Box(Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                    Text("${dateTime.hour.twoDigits()}:${dateTime.minute.twoDigits()}", fontWeight = FontWeight.Bold)
                }
            }
        }
        Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = {
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        onDate(LocalDateTime(LocalDate(year, month + 1, day), dateTime.time).toInstant(ChinaTimeZone))
                    },
                    dateTime.year,
                    dateTime.monthNumber - 1,
                    dateTime.dayOfMonth,
                ).show()
            }) {
                Icon(Icons.Default.CalendarMonth, contentDescription = "选择日期", modifier = Modifier.size(18.dp))
                if (dateTime.date != today) {
                    Spacer(Modifier.width(5.dp))
                    Text("${dateTime.monthNumber.twoDigits()}-${dateTime.dayOfMonth.twoDigits()}")
                }
            }
            Spacer(Modifier.weight(1f))
            Text("不计入收支", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(6.dp))
            Switch(checked = state.isExcluded, onCheckedChange = onExcluded)
        }
    }
}

@Composable
private fun NewSecondaryCategoryDialog(
    primaryName: String,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$primaryName · 新建二级分类") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("分类名称") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        },
        confirmButton = {
            Button(onClick = { onCreate(name.trim()) }, enabled = name.isNotBlank()) { Text("创建") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun TransactionFooter(
    state: TransactionEditorUiState,
    onAmountKey: (String) -> Unit,
    onSaveAgain: () -> Unit,
    onDone: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shadowElevation = 10.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("金额", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                Spacer(Modifier.weight(1f))
                Text(
                    "¥${state.amountInput.ifBlank { "0" }}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Keypad(
                state.isSaving,
                if (state.editingId == null) "再记" else "删除",
                onAmountKey,
                if (state.editingId == null) onSaveAgain else onDelete,
                onDone,
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
    val rows = listOf(
        listOf("1", "2", "3", "+"),
        listOf("4", "5", "6", "-"),
        listOf("7", "8", "9", actionLabel),
        listOf(".", "0", "⌫", "完成"),
    )
    rows.forEachIndexed { rowIndex, row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            row.forEach { key ->
                val isDone = key == "完成"
                val isAction = rowIndex == 2 && key == actionLabel
                val isOperator = key == "+" || key == "-"
                val color = when {
                    isDone -> MaterialTheme.colorScheme.primary
                    isAction && actionLabel == "删除" -> MaterialTheme.colorScheme.errorContainer
                    isAction || isOperator -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                val contentColor = if (isDone) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                Surface(
                    onClick = when {
                        isDone -> onDone
                        isAction -> onAction
                        else -> { { onKey(key) } }
                    },
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(13.dp),
                    color = color,
                    contentColor = contentColor,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            if (isDone && isSaving) "保存中" else key,
                            style = if (isAction) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
                            fontWeight = if (isDone || isAction || isOperator) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
            }
        }
        if (rowIndex != rows.lastIndex) Spacer(Modifier.height(6.dp))
    }
}

private fun Int.twoDigits(): String = toString().padStart(2, '0')
