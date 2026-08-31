package com.omniflow.android.ui

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omniflow.core.domain.model.Category
import com.omniflow.core.domain.model.ImportGroupMode
import com.omniflow.core.domain.model.ImportItemBucket
import com.omniflow.core.domain.model.ImportItemGroup
import com.omniflow.core.domain.model.ImportCategoryOrigin
import com.omniflow.core.domain.model.ImportPreviewItem
import com.omniflow.core.domain.model.ImportPreviewState
import com.omniflow.core.domain.model.TransactionDetailDisplayMode
import com.omniflow.core.domain.model.TransactionType
import com.omniflow.core.domain.model.countIn
import com.omniflow.core.domain.model.dateOrNull
import com.omniflow.core.domain.model.groups
import com.omniflow.core.domain.model.hourMinuteText
import com.omniflow.core.domain.model.itemsIn
import com.omniflow.core.domain.model.supportsSourceGrouping
import kotlinx.datetime.LocalDate

/** 顶部筛选 chip，与 [ImportItemBucket] 的映射。 */
enum class ImportFilter(val label: String, val buckets: Set<ImportItemBucket>) {
    IMPORTABLE("待导入", setOf(ImportItemBucket.READY, ImportItemBucket.PENDING)),
    PENDING("待分类", setOf(ImportItemBucket.PENDING)),
    SUSPECTED("疑似重复", setOf(ImportItemBucket.SUSPECTED_DUPLICATE)),
    NEUTRAL("不计收支", setOf(ImportItemBucket.NEUTRAL)),
    EXISTING("已存在", setOf(ImportItemBucket.EXISTING)),
}

/** 预览明细的日期范围筛选；开启后列表只展示区间内明细，确认入账也只入账区间内。 */
data class ImportDateFilterState(
    val enabled: Boolean,
    val start: LocalDate,
    val end: LocalDate,
)

private data class PickerTarget(val itemIds: Set<String>, val title: String, val type: TransactionType)

@Composable
internal fun ImportPreviewSection(
    preview: ImportPreviewState,
    state: MoreUiState,
    viewModel: OmniFlowViewModel,
    modifier: Modifier = Modifier,
) {
    // 日期筛选只影响展示与入账口径：会话里始终存全量，改区间即时生效、无需重新解析。
    val dateFilter = state.importDateFilter
    val scoped = remember(preview, dateFilter) {
        preview.copy(items = preview.itemsIn(dateFilter?.takeIf { it.enabled }?.let { exportRange(it.start, it.end) }))
    }
    val sourceGrouping = scoped.supportsSourceGrouping
    val mode = if (sourceGrouping) state.importGroupMode else ImportGroupMode.DATE
    val collapsed = rememberSaveable(preview.sessionId, saver = collapsedKeysSaver) { mutableStateOf(emptySet<String>()) }
    var displayMode by rememberSaveable(preview.sessionId) { mutableStateOf(TransactionDetailDisplayMode.LIST) }
    var picker by remember { mutableStateOf<PickerTarget?>(null) }
    val groups = remember(scoped, mode, state.importFilter) {
        scoped.groups(mode, state.importFilter.buckets)
    }

    Column(modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            dateFilter?.let {
                item(key = "date-filter") { ImportDateFilterBar(it, viewModel::setImportDateFilter) }
            }
            item { ImportSummary(scoped, state, viewModel) }
            item {
                ImportToolbar(
                    mode = mode,
                    sourceGroupingAvailable = sourceGrouping,
                    displayMode = displayMode,
                    onMode = viewModel::setImportGroupMode,
                    onDisplayMode = { displayMode = it },
                )
            }
            if (groups.isEmpty()) {
                item {
                    Text(
                        emptyMessage(state.importFilter),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            groups.forEach { group ->
                val isCollapsed = group.key in collapsed.value
                item(key = "header-${group.key}") {
                    ImportGroupHeader(
                        group = group,
                        mode = mode,
                        categories = state.categories,
                        collapsed = isCollapsed,
                        readOnly = state.importFilter == ImportFilter.EXISTING,
                        onToggle = {
                            collapsed.value = if (isCollapsed) {
                                collapsed.value - group.key
                            } else {
                                collapsed.value + group.key
                            }
                        },
                        onPickCategory = {
                            picker = PickerTarget(
                                itemIds = group.items.mapTo(mutableSetOf()) { it.id },
                                title = "整组「${group.label}」共 ${group.items.size} 笔",
                                type = group.items.firstOrNull()?.type ?: TransactionType.EXPENSE,
                            )
                        },
                        onIncludeAll = {
                            viewModel.setImportSkipped(group.items.mapTo(mutableSetOf()) { it.id }, false)
                        },
                    )
                }
                if (!isCollapsed) {
                    when (displayMode) {
                        TransactionDetailDisplayMode.LIST -> items(group.items, key = { it.id }) { item ->
                            ImportItemRow(
                                item = item,
                                categories = state.categories,
                                onClick = {
                                    picker = PickerTarget(
                                        itemIds = setOf(item.id),
                                        title = item.note ?: item.raw.counterparty ?: "这一笔",
                                        type = item.type ?: TransactionType.EXPENSE,
                                    )
                                },
                                onToggleSkip = { viewModel.setImportSkipped(setOf(item.id), !item.isSkipped) },
                            )
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                        }

                        TransactionDetailDisplayMode.CARD -> items(group.items.chunked(2)) { row ->
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                row.forEach { item ->
                                    val info = state.categories.displayInfo(item.categoryId)
                                    TransactionTile(
                                        iconKey = info.iconKey,
                                        title = info.name,
                                        subtitle = item.note ?: item.raw.note,
                                        amount = item.raw.amount,
                                        type = item.type,
                                        timeText = item.raw.occurredAt.hourMinuteText(),
                                        modifier = Modifier.weight(1f),
                                        dimmed = item.isSkipped,
                                        titleMuted = item.categoryId == null,
                                        onClick = {
                                            picker = PickerTarget(
                                                itemIds = setOf(item.id),
                                                title = item.note ?: "这一笔",
                                                type = item.type ?: TransactionType.EXPENSE,
                                            )
                                        },
                                    )
                                }
                                repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
        }
        ImportActionBar(scoped, state, viewModel)
    }

    picker?.let { target ->
        CategoryPickerSheet(
            title = target.title,
            categories = state.categories,
            selectedCategoryId = scoped.items.firstOrNull { it.id in target.itemIds }?.categoryId,
            initialType = target.type,
            onDismiss = { picker = null },
            onSelected = { categoryId, type ->
                viewModel.setImportCategory(target.itemIds, categoryId, type)
                picker = null
            },
        )
    }
}

/** 预览明细顶部的日期筛选条；默认按明细最早/最晚日期预填并开启。 */
@Composable
private fun ImportDateFilterBar(
    filter: ImportDateFilterState,
    onFilter: (ImportDateFilterState) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "按日期筛选",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Switch(
                    checked = filter.enabled,
                    onCheckedChange = { onFilter(filter.copy(enabled = it)) },
                )
            }
            if (filter.enabled) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExportDateButton("开始", filter.start, Modifier.weight(1f)) { onFilter(filter.copy(start = it)) }
                    ExportDateButton("结束", filter.end, Modifier.weight(1f)) { onFilter(filter.copy(end = it)) }
                }
            }
        }
    }
}

@Composable
private fun ImportSummary(preview: ImportPreviewState, state: MoreUiState, viewModel: OmniFlowViewModel) {
    val accountName = preview.items.firstNotNullOfOrNull { item ->
        state.accounts.firstOrNull { it.id == item.accountId }?.name
    }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.large,
        color = surfaceCard(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "将导入 ${preview.importableItems.size} 笔",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "支出 ${preview.expenseTotal.asRmb()} · 收入 ${preview.incomeTotal.asRmb()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (preview.aiSuggestedCount > 0) {
                Text(
                    "AI 建议了 ${preview.aiSuggestedCount} 笔的分类，确认前请抽查",
                    style = OmniText.caption,
                    color = mutedContent(),
                )
            }
            preview.suggestionError?.let { failure ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(failure, Modifier.weight(1f), style = OmniText.caption, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = viewModel::resuggestImport) { Text("重试", style = OmniText.caption) }
                }
            }
            Text(
                accountName?.let { "账户 $it（按账单支付方式自动匹配）" } ?: "未匹配到账户，请先在「账户」里创建",
                style = MaterialTheme.typography.bodySmall,
                color = if (accountName == null) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ImportFilter.entries.forEach { filter ->
                    val count = preview.countIn(*filter.buckets.toTypedArray())
                    if (count > 0 || filter == ImportFilter.IMPORTABLE) {
                        FilterChip(
                            selected = state.importFilter == filter,
                            onClick = { viewModel.setImportFilter(filter) },
                            label = { Text("${filter.label} $count") },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportToolbar(
    mode: ImportGroupMode,
    sourceGroupingAvailable: Boolean,
    displayMode: TransactionDetailDisplayMode,
    onMode: (ImportGroupMode) -> Unit,
    onDisplayMode: (TransactionDetailDisplayMode) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (sourceGroupingAvailable) {
            FilterChip(
                selected = mode == ImportGroupMode.SOURCE,
                onClick = { onMode(ImportGroupMode.SOURCE) },
                label = { Text("按分类") },
            )
            FilterChip(
                selected = mode == ImportGroupMode.DATE,
                onClick = { onMode(ImportGroupMode.DATE) },
                label = { Text("按日期") },
            )
        } else {
            Text(
                "该账单没有分类字段，按日期分组",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = {
            onDisplayMode(
                if (displayMode == TransactionDetailDisplayMode.LIST) {
                    TransactionDetailDisplayMode.CARD
                } else {
                    TransactionDetailDisplayMode.LIST
                },
            )
        }) {
            Icon(
                if (displayMode == TransactionDetailDisplayMode.LIST) {
                    Icons.Default.ViewModule
                } else {
                    Icons.AutoMirrored.Filled.List
                },
                contentDescription = "切换明细展示方式",
            )
        }
    }
}

@Composable
private fun ImportGroupHeader(
    group: ImportItemGroup,
    mode: ImportGroupMode,
    categories: List<Category>,
    collapsed: Boolean,
    readOnly: Boolean,
    onToggle: () -> Unit,
    onPickCategory: () -> Unit,
    onIncludeAll: () -> Unit,
) {
    val label = if (mode == ImportGroupMode.DATE) {
        group.dateOrNull()?.displayName() ?: group.label
    } else {
        group.label
    }
    val assigned = group.categoryIds
    val assignment = when {
        assigned.size == 1 && assigned.first() != null -> "已归到「${categories.displayInfo(assigned.first()).name}」"
        assigned.size == 1 -> "待分类"
        else -> "${assigned.count { it != null }} 种分类"
    }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        onClick = onToggle,
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${group.items.size} 笔",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    if (collapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                    contentDescription = if (collapsed) "展开" else "折叠",
                    modifier = Modifier.size(20.dp),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    assignment,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (assigned == setOf(null)) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // 组头的收支色和首页日期分组同一套，不用会随主题漂移的 error/tertiary
                if (group.expenseTotal.minor != 0L) {
                    Text("支 ${group.expenseTotal.asCompactRmb()}", style = OmniText.caption, color = mutedContent())
                }
                if (group.incomeTotal.minor != 0L) {
                    Spacer(Modifier.width(8.dp))
                    Text("收 ${group.incomeTotal.asCompactRmb()}", style = OmniText.caption, color = incomeColor())
                }
            }
            if (!readOnly) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onPickCategory) { Text("整组归类") }
                    if (group.items.all(ImportPreviewItem::isSkipped)) {
                        TextButton(onClick = onIncludeAll) { Text("整组加回") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportItemRow(
    item: ImportPreviewItem,
    categories: List<Category>,
    onClick: () -> Unit,
    onToggleSkip: () -> Unit,
) {
    val info = categories.displayInfo(item.categoryId)
    Box(Modifier.padding(horizontal = 16.dp)) {
        TransactionRow(
            iconKey = info.iconKey,
            title = info.name,
            subtitle = item.note ?: item.raw.note,
            amount = item.raw.amount,
            type = item.type,
            timeText = item.raw.occurredAt.hourMinuteText(),
            dimmed = item.isSkipped,
            titleMuted = item.categoryId == null,
            onClick = onClick,
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.categoryOrigin == ImportCategoryOrigin.AI) AiBadge()
                    TextButton(onClick = onToggleSkip) {
                        Text(
                            if (item.isSkipped) "加回" else "排除",
                            style = OmniText.caption,
                        )
                    }
                }
            },
        )
    }
}

/** 只在 AI 填过分类的行上出现，提醒这条是猜的、值得看一眼。 */
@Composable
private fun AiBadge() {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
        Text(
            "AI",
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            style = OmniText.caption,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun ImportActionBar(preview: ImportPreviewState, state: MoreUiState, viewModel: OmniFlowViewModel) {
    Surface(tonalElevation = 3.dp) {
        Column {
            if (state.isImporting) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(
                    "正在入账，请稍候…",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            state.error?.let { error ->
                Text(
                    error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (preview.pendingCount > 0) "还有 ${preview.pendingCount} 笔待分类" else "全部已归类",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (preview.pendingCount > 0) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    if (preview.skippedCount > 0) {
                        Text(
                            "不导入 ${preview.skippedCount} 笔",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                TextButton(onClick = viewModel::cancelImport) { Text("取消") }
                Button(
                    onClick = viewModel::commitImport,
                    enabled = preview.isReadyToCommit && !state.isImporting && preview.importableItems.isNotEmpty(),
                ) { Text("确认入账 ${preview.importableItems.size} 笔") }
            }
        }
    }
}

private fun emptyMessage(filter: ImportFilter): String = when (filter) {
    ImportFilter.IMPORTABLE -> "没有可导入的明细"
    ImportFilter.PENDING -> "没有待分类的明细"
    ImportFilter.SUSPECTED -> "没有疑似重复的明细"
    ImportFilter.NEUTRAL -> "没有不计收支的明细"
    ImportFilter.EXISTING -> "没有已存在的明细"
}

internal data class CategoryDisplayInfo(val name: String, val iconKey: String?)

/** 分类显示名统一用「一级分类 - 二级分类」，图标取一级分类的。 */
internal fun List<Category>.displayInfo(categoryId: String?): CategoryDisplayInfo {
    val category = firstOrNull { it.id == categoryId } ?: return CategoryDisplayInfo("待分类", null)
    val primary = category.parentId?.let { parentId -> firstOrNull { it.id == parentId } } ?: category
    val name = if (primary.id == category.id) category.name else "${primary.name} - ${category.name}"
    return CategoryDisplayInfo(name, primary.iconKey ?: category.iconKey)
}

private val collapsedKeysSaver = androidx.compose.runtime.saveable.Saver<
    androidx.compose.runtime.MutableState<Set<String>>,
    List<String>,
    >(
    save = { it.value.toList() },
    restore = { mutableStateOf(it.toSet()) },
)
