package com.omniflow.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.designsystem.components.OmniExpressiveSwitch
import com.omniflow.android.ui.designsystem.tokens.OmniShapeTokens
import com.omniflow.core.domain.model.Category
import com.omniflow.core.domain.model.ImportGroupMode
import com.omniflow.core.domain.model.ImportCategoryOrigin
import com.omniflow.core.domain.model.ImportItemGroup
import com.omniflow.core.domain.model.ImportPreviewItem
import com.omniflow.core.domain.model.TransactionDetailDisplayMode
import com.omniflow.core.domain.model.dateOrNull
import com.omniflow.core.domain.model.hourMinuteText

/** 预览明细顶部的日期筛选条；默认按明细最早/最晚日期预填并开启。 */
@Composable
internal fun ImportDateFilterBar(
    filter: ImportDateFilterState,
    onFilter: (ImportDateFilterState) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
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
                OmniExpressiveSwitch(
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
internal fun ImportToolbar(
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
internal fun ImportGroupHeader(
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
        shape = OmniShapeTokens.medium,
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
internal fun ImportItemRow(
    item: ImportPreviewItem,
    categories: List<Category>,
    onClick: () -> Unit,
    onToggleSkip: () -> Unit,
) {
    val info = categories.displayInfo(item.categoryId)
    Surface(
        shape = OmniShapeTokens.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
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
            modifier = Modifier.padding(horizontal = 12.dp),
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
internal fun AiBadge() {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
        Text(
            "AI",
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            style = OmniText.caption,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

internal fun emptyMessage(filter: ImportFilter): String = when (filter) {
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
