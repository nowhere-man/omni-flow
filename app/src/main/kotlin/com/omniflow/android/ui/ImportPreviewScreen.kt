package com.omniflow.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.designsystem.components.OmniEmptyState
import com.omniflow.core.domain.model.ImportGroupMode
import com.omniflow.core.domain.model.ImportItemBucket
import com.omniflow.core.domain.model.ImportPreviewState
import com.omniflow.core.domain.model.TransactionDetailDisplayMode
import com.omniflow.core.domain.model.TransactionType
import com.omniflow.core.domain.model.countIn
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
                    OmniEmptyState(
                        message = emptyMessage(state.importFilter),
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
                                title = "整组「${group.label}」",
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

@Composable
private fun ImportSummary(preview: ImportPreviewState, state: MoreUiState, viewModel: OmniFlowViewModel) {
    val accountName = preview.items.firstNotNullOfOrNull { item ->
        state.accounts.firstOrNull { it.id == item.accountId }?.name
    }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
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

private val collapsedKeysSaver = androidx.compose.runtime.saveable.Saver<
    androidx.compose.runtime.MutableState<Set<String>>,
    List<String>,
    >(
    save = { it.value.toList() },
    restore = { mutableStateOf(it.toSet()) },
)
