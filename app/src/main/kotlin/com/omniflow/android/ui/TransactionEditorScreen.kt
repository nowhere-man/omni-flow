package com.omniflow.android.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import com.omniflow.core.domain.model.Account
import com.omniflow.core.domain.model.Category
import com.omniflow.core.domain.model.Ledger
import com.omniflow.core.domain.model.TransactionType
import kotlin.math.ceil
import kotlin.math.roundToInt
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
    sheetHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val selectedCategory = state.categories.firstOrNull { it.id == state.categoryId }
    val primaryId = selectedCategory?.parentId ?: selectedCategory?.id
    val primaryCategories = state.categories.filter { it.parentId == null && it.type == state.type }
    val secondaryCategories = state.categories.filter { it.parentId == primaryId && it.type == state.type }
    val selectedPrimary = state.categories.firstOrNull { it.id == primaryId }
    val selectedSecondary = selectedCategory?.takeIf { it.parentId != null }
    var showSecondaryDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    // 备注聚焦时让出数字键盘的位置给系统输入法，否则面板会被两块键盘一起挤没。
    var noteFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    // 矮屏放不下三行分类：先保证键盘和录入面板完整，分类降到两行分页翻
    val categoryRows = if (sheetHeight < 700.dp) 2 else 3

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(sheetHeight)
            .imePadding(),
    ) {
        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 账本单独占一行，和首页、统计页一致
            LedgerScopePill(
                label = state.ledgers.firstOrNull { it.id == state.ledgerId }?.name ?: "选择账本",
                ledgers = state.ledgers,
                onAll = null,
                onSelected = { onLedger(it.id) },
            )
            TransactionModeSwitch(
                selected = state.type,
                onSelected = onType,
                modifier = Modifier.fillMaxWidth(),
            )
            if (primaryCategories.isEmpty()) {
                Text("选择账本后加载分类", style = OmniText.caption, color = mutedContent())
                Spacer(Modifier.weight(1f))
            } else {
                // 网格拿走剩余高度但封顶在三行；富余的部分自然落在它和录入面板之间，
                // 矮屏则由网格自己收紧，不会把键盘挤出屏幕。
                PagedCategoryGrid(
                    categories = primaryCategories,
                    selectedId = primaryId,
                    onSelected = onCategory,
                    onReordered = onReorderPrimary,
                    resetSignal = state.error,
                    rowsPerPage = categoryRows,
                    modifier = Modifier.weight(1f),
                )
            }
            EditorAttributeRow(
                state = state,
                onAccount = { onAccount(it.id) },
                onTag = onTag,
                moreExpanded = showMoreMenu,
                onMoreExpanded = { showMoreMenu = it },
                onDate = onDate,
                onExcluded = onExcluded,
            )
            TransactionEntryPanel(
                state = state,
                primary = selectedPrimary,
                selectedSecondary = selectedSecondary,
                secondaryCategories = secondaryCategories,
                onSecondary = onCategory,
                onAddSecondary = { showSecondaryDialog = true },
                onNote = onNote,
                onDate = onDate,
                onNoteFocusChanged = { noteFocused = it },
            )
            Spacer(Modifier.height(4.dp))
        }
        if (noteFocused) {
            NoteInputBar(onDone = { focusManager.clearFocus() })
        } else {
            TransactionFooter(state, onAmountKey, onSaveAgain, onDone)
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

/**
 * 收支切换。以前支出用 errorContainer、收入用 tertiaryContainer，两块高饱和色块跟整屏都不搭；
 * 改成和统计页、搜索页同一个分段控件，选中态只靠一层更亮的表面区分。
 */
@Composable
private fun TransactionModeSwitch(
    selected: TransactionType,
    onSelected: (TransactionType) -> Unit,
    modifier: Modifier = Modifier,
) = OmniSegmented(
    options = TransactionType.entries,
    selected = selected,
    label = { if (it == TransactionType.EXPENSE) "支出" else "收入" },
    onSelected = onSelected,
    modifier = modifier,
)

/** 账户、标签和「更多」（时间 / 不计入统计）单独一行，把录入面板压到三行。 */
@Composable
private fun EditorAttributeRow(
    state: TransactionEditorUiState,
    onAccount: (Account) -> Unit,
    onTag: (String) -> Unit,
    moreExpanded: Boolean,
    onMoreExpanded: (Boolean) -> Unit,
    onDate: (kotlinx.datetime.Instant) -> Unit,
    onExcluded: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val dateTime = state.occurredAt.toLocalDateTime(ChinaTimeZone)
    val selectedAccount = state.accounts.firstOrNull { it.id == state.accountId }
    var accountExpanded by remember { mutableStateOf(false) }
    var tagExpanded by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().height(38.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box {
            EditorChip(
                label = selectedAccount?.name ?: "选择账户",
                onClick = { accountExpanded = true },
                leading = {
                    if (selectedAccount == null) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(15.dp))
                    } else {
                        SvgIcon(selectedAccount.iconKey, Modifier.size(15.dp))
                    }
                },
            )
            DropdownMenu(expanded = accountExpanded, onDismissRequest = { accountExpanded = false }) {
                state.accounts.forEach { account ->
                    DropdownMenuItem(
                        text = { Text(account.name) },
                        onClick = { accountExpanded = false; onAccount(account) },
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        if (state.tags.isNotEmpty()) {
            Box {
                EditorChip(
                    label = if (state.selectedTagIds.isEmpty()) "标签" else "标签 ${state.selectedTagIds.size}",
                    onClick = { tagExpanded = true },
                    selected = state.selectedTagIds.isNotEmpty(),
                )
                DropdownMenu(expanded = tagExpanded, onDismissRequest = { tagExpanded = false }) {
                    state.tags.forEach { tag ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    tag.name,
                                    fontWeight = if (tag.id in state.selectedTagIds) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                            onClick = { onTag(tag.id) },
                        )
                    }
                }
            }
        }
        Box {
            EditorChip(label = "⋯", onClick = { onMoreExpanded(true) })
            DropdownMenu(expanded = moreExpanded, onDismissRequest = { onMoreExpanded(false) }) {
                DropdownMenuItem(
                    text = { Text("时间 ${dateTime.hour.twoDigits()}:${dateTime.minute.twoDigits()}") },
                    onClick = {
                        onMoreExpanded(false)
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
                )
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("不计入统计", modifier = Modifier.weight(1f))
                            Spacer(Modifier.width(8.dp))
                            Switch(checked = state.isExcluded, onCheckedChange = null)
                        }
                    },
                    onClick = { onExcluded(!state.isExcluded) },
                )
            }
        }
    }
}

@Composable
private fun EditorChip(
    label: String,
    onClick: () -> Unit,
    selected: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.height(34.dp),
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            leading?.invoke()
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * 一级分类固定 [rowsPerPage] 行 5 列一页，超出横向翻页，高度恒定，不会再把下面的录入面板挤没。
 * 长按仍可拖动排序：[detectDragGesturesAfterLongPress] 只在长按后消费手势，
 * 普通横滑交给 pager；拖拽期间关掉 pager 的手势，避免翻页抢走正在进行的拖动。
 */
@Composable
private fun PagedCategoryGrid(
    categories: List<Category>,
    selectedId: String?,
    onSelected: (String?) -> Unit,
    onReordered: (List<String>) -> Unit,
    resetSignal: Any?,
    rowsPerPage: Int,
    modifier: Modifier = Modifier,
) {
    val columns = 5
    val perPage = columns * rowsPerPage
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    var width by remember { mutableIntStateOf(0) }
    // 乐观重排：成功时上游会推新顺序回来；失败时 error 变化触发这里回滚到数据库里的顺序。
    var ordered by remember(categories, resetSignal) { mutableStateOf(categories) }
    var draggedId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var changed by remember { mutableStateOf(false) }
    val pageCount = ceil(ordered.size / perPage.toFloat()).toInt().coerceAtLeast(1)
    val pagerState = rememberPagerState(pageCount = { pageCount })

    // 格子高度按分到的空间算、封顶 72dp：矮屏自动收紧不会把键盘挤出去，
    // 高屏用不完的部分留在网格下方，正好成为分类和录入面板之间的留白。
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val dotsHeight = if (pageCount > 1) 16.dp else 0.dp
        val cellHeight = ((maxHeight - dotsHeight) / rowsPerPage).coerceIn(MinCategoryCell, MaxCategoryCell)
        val cellHeightPx = with(density) { cellHeight.toPx() }
        Column(Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().height(cellHeight * rowsPerPage.toFloat()),
            userScrollEnabled = draggedId == null,
            verticalAlignment = Alignment.Top,
        ) { page ->
            val pageItems = ordered.drop(page * perPage).take(perPage)
            val pageRows = ceil(pageItems.size / columns.toFloat()).toInt().coerceAtLeast(1)
            Box(Modifier.fillMaxSize().onSizeChanged { width = it.width }) {
                if (width > 0) {
                    val cellWidthPx = width.toFloat() / columns
                    val cellWidth = with(density) { cellWidthPx.toDp() }
                    fun position(index: Int) = IntOffset(
                        x = ((index % columns) * cellWidthPx).roundToInt(),
                        y = ((index / columns) * cellHeightPx).roundToInt(),
                    )
                    pageItems.forEachIndexed { indexInPage, category ->
                        key(category.id) {
                            val target = position(indexInPage)
                            val animatedTarget by animateIntOffsetAsState(target, spring(), label = "category-position")
                            val dragging = draggedId == category.id
                            val scale by animateFloatAsState(if (dragging) 1.08f else 1f, spring(), label = "category-scale")
                            CategoryTile(
                                category = category,
                                selected = selectedId == category.id,
                                dragging = dragging,
                                onClick = { onSelected(category.id) },
                                modifier = Modifier
                                    .offset {
                                        if (dragging) {
                                            IntOffset(target.x + dragOffset.x.roundToInt(), target.y + dragOffset.y.roundToInt())
                                        } else animatedTarget
                                    }
                                    .width(cellWidth)
                                    .height(cellHeight)
                                    .padding(2.dp)
                                    .zIndex(if (dragging) 2f else 0f)
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        shadowElevation = if (dragging) 4.dp.toPx() else 0f
                                    }
                                    .pointerInput(category.id, width, page) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                draggedId = category.id
                                                dragOffset = Offset.Zero
                                                changed = false
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            },
                                            onDragCancel = {
                                                ordered = categories
                                                draggedId = null
                                                dragOffset = Offset.Zero
                                                changed = false
                                            },
                                            onDragEnd = {
                                                if (changed) onReordered(ordered.map(Category::id))
                                                draggedId = null
                                                dragOffset = Offset.Zero
                                                changed = false
                                            },
                                            onDrag = { change, amount ->
                                                change.consume()
                                                val pageOffset = page * perPage
                                                val currentIndex = ordered.indexOfFirst { it.id == category.id }
                                                if (currentIndex < 0) return@detectDragGesturesAfterLongPress
                                                val currentInPage = currentIndex - pageOffset
                                                if (currentInPage !in pageItems.indices) return@detectDragGesturesAfterLongPress
                                                dragOffset += amount
                                                val oldBase = position(currentInPage)
                                                val centerX = oldBase.x + dragOffset.x + cellWidthPx / 2f
                                                val centerY = oldBase.y + dragOffset.y + cellHeightPx / 2f
                                                val column = (centerX / cellWidthPx).toInt().coerceIn(0, columns - 1)
                                                val row = (centerY / cellHeightPx).toInt().coerceIn(0, pageRows - 1)
                                                // 只在当前页内重排，跨页拖动不处理
                                                val targetInPage = (row * columns + column).coerceIn(0, pageItems.lastIndex)
                                                if (targetInPage != currentInPage) {
                                                    ordered = ordered.toMutableList()
                                                        .apply { add(targetInPage + pageOffset, removeAt(currentIndex)) }
                                                    val newBase = position(targetInPage)
                                                    dragOffset += Offset(
                                                        (oldBase.x - newBase.x).toFloat(),
                                                        (oldBase.y - newBase.y).toFloat(),
                                                    )
                                                    changed = true
                                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                }
                                            },
                                        )
                                    },
                            )
                        }
                    }
                }
            }
        }
        if (pageCount > 1) {
            Row(
                Modifier.fillMaxWidth().height(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(pageCount) { page ->
                    val active = pagerState.currentPage == page
                    Box(
                        Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (active) 7.dp else 5.dp)
                            .clip(CircleShape)
                            .background(
                                if (active) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                            ),
                    )
                }
            }
        }
        }
    }
}

@Composable
internal fun CategoryTile(
    category: Category,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dragging: Boolean = false,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f) else Color.Transparent,
        border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        tonalElevation = if (dragging) 8.dp else 0.dp,
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = if (selected) Color.Transparent else surfaceInset(),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        SvgIcon(
                            categoryIconKey(category.iconKey),
                            Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Text(
                    category.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

/**
 * 录入面板常驻：金额只画在这里，以前要选完一级分类才出现，
 * 导致没选分类时按数字完全没有反馈。
 */
@Composable
private fun TransactionEntryPanel(
    state: TransactionEditorUiState,
    primary: Category?,
    selectedSecondary: Category?,
    secondaryCategories: List<Category>,
    onSecondary: (String?) -> Unit,
    onAddSecondary: () -> Unit,
    onNote: (String) -> Unit,
    onDate: (kotlinx.datetime.Instant) -> Unit,
    onNoteFocusChanged: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val dateTime = state.occurredAt.toLocalDateTime(ChinaTimeZone)
    val amountColor = amountColor(state.type)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(OmniRadius.medium),
        color = surfaceCard(),
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (primary != null) {
                    Surface(
                        modifier = Modifier.size(28.dp),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            SvgIcon(categoryIconKey(primary.iconKey), Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = primary?.let { p -> selectedSecondary?.let { "${p.name} - ${it.name}" } ?: p.name } ?: "先选择分类",
                    style = OmniText.bodyRow.copy(fontWeight = FontWeight.Medium),
                    color = if (primary == null) mutedContent() else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    state.amountInput.ifBlank { "0" },
                    style = OmniText.amountPrimary,
                    color = amountColor,
                    maxLines = 1,
                )
            }
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            if (primary != null) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    secondaryCategories.forEach { category ->
                        FilterChip(
                            selected = category.id == selectedSecondary?.id,
                            onClick = { onSecondary(if (category.id == selectedSecondary?.id) primary.id else category.id) },
                            label = { Text(category.name, maxLines = 1) },
                        )
                    }
                    Surface(onClick = onAddSecondary, shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                        Icon(Icons.Default.Add, contentDescription = "新建二级分类", modifier = Modifier.padding(8.dp).size(16.dp))
                    }
                }
            }
            Row(Modifier.fillMaxWidth().height(38.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.weight(1f).height(34.dp),
                    color = surfaceInset(),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.EditNote,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(6.dp))
                        Box(Modifier.weight(1f)) {
                            if (state.note.isBlank()) {
                                Text("备注", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                            BasicTextField(
                                value = state.note,
                                onValueChange = onNote,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { onNoteFocusChanged(it.isFocused) },
                                singleLine = true,
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                textStyle = TextStyle(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                ),
                            )
                        }
                    }
                }
                Spacer(Modifier.width(6.dp))
                Surface(
                    onClick = {
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                onDate(LocalDateTime(LocalDate(year, month + 1, day), dateTime.time).toInstant(ChinaTimeZone))
                            },
                            dateTime.year,
                            dateTime.monthNumber - 1,
                            dateTime.dayOfMonth,
                        ).show()
                    },
                    modifier = Modifier.height(34.dp),
                    color = surfaceInset(),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Row(
                        Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "选择日期", modifier = Modifier.size(16.dp))
                        // 始终显示日期，以前当天只剩一个光秃秃的图标，看不出会记到哪天
                        Text(
                            "${dateTime.monthNumber}月${dateTime.dayOfMonth}日",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

/** 备注聚焦时替换数字键盘的确认条。 */
@Composable
private fun NoteInputBar(onDone: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "输入备注后点完成回到数字键盘",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDone) { Text("完成") }
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
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 2.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 6.dp, vertical = 6.dp),
        ) {
            Keypad(
                state.isSaving,
                onAmountKey,
                onSaveAgain,
                onDone,
            )
        }
    }
}

@Composable
private fun Keypad(
    isSaving: Boolean,
    onKey: (String) -> Unit,
    onAction: () -> Unit,
    onDone: () -> Unit,
) {
    val rows = listOf(
        listOf("1", "2", "3", "+"),
        listOf("4", "5", "6", "-"),
        listOf("7", "8", "9", "再记"),
        listOf(".", "0", "退格", "完成"),
    )
    rows.forEachIndexed { rowIndex, row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            row.forEach { key ->
                val isDone = key == "完成"
                val isAction = rowIndex == 2 && key == "再记"
                val isOperator = key == "+" || key == "-"
                val color = when {
                    isDone -> MaterialTheme.colorScheme.primary
                    isAction || isOperator -> MaterialTheme.colorScheme.secondaryContainer
                    else -> surfaceInset()
                }
                val contentColor = if (isDone) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                Surface(
                    onClick = when {
                        isDone -> onDone
                        isAction -> onAction
                        else -> { { onKey(key) } }
                    },
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = color,
                    contentColor = contentColor,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            if (isDone && isSaving) "保存中" else key,
                            style = if (isAction || key == "退格") MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
        if (rowIndex != rows.lastIndex) Spacer(Modifier.height(4.dp))
    }
}

private fun Int.twoDigits(): String = toString().padStart(2, '0')

/** 分类格子的高度区间：再高图标和文字就散了，再矮点不动。 */
private val MaxCategoryCell = 72.dp
private val MinCategoryCell = 56.dp
