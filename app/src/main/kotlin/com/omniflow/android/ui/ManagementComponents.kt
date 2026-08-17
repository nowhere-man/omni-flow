package com.omniflow.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omniflow.core.domain.model.Ledger

/**
 * 管理页统一的新建/编辑表单。以前用 AlertDialog，字段一多就挤，
 * 图标选择还得再叠一层对话框；底部弹层空间够，也和记账页、分类选择器的交互一致。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ManagementSheet(
    title: String,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    saveEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消") }
                Spacer(Modifier.size(8.dp))
                Button(onClick = onSave, enabled = saveEnabled) { Text("保存") }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

/** 图标选择网格，账户和分类共用；以前账户是把 `wallet-cards` 这种 key 字符串直接列给用户看。 */
@Composable
internal fun IconPickerGrid(
    options: List<CategoryIconOption>,
    selectedKey: String,
    modifier: Modifier = Modifier,
    onSelected: (String) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(56.dp),
        modifier = modifier.heightIn(max = 200.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(options, key = { it.key }) { option ->
            val selected = option.key == selectedKey
            Surface(
                onClick = { onSelected(option.key) },
                modifier = Modifier.size(56.dp),
                shape = MaterialTheme.shapes.medium,
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else surfaceInset(),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    SvgIcon(
                        categoryIconKey(option.key),
                        Modifier.size(26.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

internal data class LedgerCover(val key: String, val label: String, val start: Color, val end: Color, val iconKey: String)

/**
 * 内置账本封面。存的仍然是 [Ledger.coverKey] 这个字符串，
 * 所以不需要图片存储、也不影响备份恢复；以前这里是让用户手输 key 的裸文本框。
 */
internal val LedgerCovers = listOf(
    LedgerCover("cover-daily", "日常", Color(0xFF6C8AE4), Color(0xFF8FB0F5), "shopping-bag"),
    LedgerCover("cover-family", "家庭", Color(0xFFE4826C), Color(0xFFF5AC93), "house"),
    LedgerCover("cover-travel", "旅行", Color(0xFF44B0A6), Color(0xFF7FD6CB), "plane"),
    LedgerCover("cover-work", "工作", Color(0xFF7A6CE4), Color(0xFFA79BF5), "briefcase-business"),
    LedgerCover("cover-study", "学习", Color(0xFFCE7BB8), Color(0xFFEBA6D8), "books"),
    LedgerCover("cover-car", "爱车", Color(0xFF4E7CA8), Color(0xFF83AACF), "car"),
    LedgerCover("cover-fitness", "健康", Color(0xFF66A85B), Color(0xFF9BD08F), "fitness"),
    LedgerCover("cover-invest", "理财", Color(0xFFC79A45), Color(0xFFE8C583), "chart-line"),
)

internal fun ledgerCover(coverKey: String?): LedgerCover =
    LedgerCovers.firstOrNull { it.key == coverKey } ?: LedgerCovers.first()

/** 账本封面块，列表卡片和详情头图共用。 */
@Composable
internal fun LedgerCoverBox(
    coverKey: String?,
    modifier: Modifier = Modifier,
    iconSize: Int = 24,
) {
    val cover = ledgerCover(coverKey)
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(Brush.linearGradient(listOf(cover.start, cover.end))),
        contentAlignment = Alignment.Center,
    ) {
        SvgIcon(categoryIconKey(cover.iconKey), Modifier.size(iconSize.dp), tint = Color.White)
    }
}

@Composable
internal fun LedgerCoverPicker(selectedKey: String?, onSelected: (String) -> Unit) {
    val selected = ledgerCover(selectedKey).key
    LazyVerticalGrid(
        columns = GridCells.Adaptive(72.dp),
        modifier = Modifier.heightIn(max = 180.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(LedgerCovers, key = { it.key }) { cover ->
            val isSelected = cover.key == selected
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Surface(
                    onClick = { onSelected(cover.key) },
                    modifier = Modifier
                        .size(64.dp)
                        .border(
                            width = if (isSelected) 2.dp else 0.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = MaterialTheme.shapes.medium,
                        ),
                    shape = MaterialTheme.shapes.medium,
                    color = Color.Transparent,
                ) {
                    LedgerCoverBox(cover.key, Modifier.padding(3.dp), iconSize = 26)
                }
                Text(cover.label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}

/** 分类/标签/规则页顶部的账本选择条，取代原来那个裸 ValueMenu。 */
@Composable
internal fun LedgerPickerBar(
    ledgers: List<Ledger>,
    selectedId: String?,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = ledgers.firstOrNull { it.id == selectedId }
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Surface(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(OmniRadius.medium),
            color = surfaceCard(),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (selected != null) {
                    LedgerCoverBox(selected.coverKey, Modifier.size(32.dp), iconSize = 18)
                } else {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(24.dp), tint = mutedContent())
                }
                Spacer(Modifier.size(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("当前账本", style = OmniText.caption, color = mutedContent())
                    Text(
                        selected?.name ?: "选择账本",
                        style = OmniText.titleRow,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(20.dp), tint = mutedContent())
            }
        }
        androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ledgers.forEach { ledger ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(ledger.name) },
                    onClick = { expanded = false; onSelected(ledger.id) },
                    leadingIcon = { LedgerCoverBox(ledger.coverKey, Modifier.size(28.dp), iconSize = 16) },
                )
            }
        }
    }
}

/** 管理页统一的分组标题。 */
@Composable
internal fun ManagementGroupHeader(title: String, trailing: String? = null) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, modifier = Modifier.weight(1f), style = OmniText.caption, color = mutedContent())
        trailing?.let { Text(it, style = OmniText.caption, color = mutedContent()) }
    }
}

/** 圆形图标底，账户行和分类行共用。 */
@Composable
internal fun RoundIcon(iconKey: String?, modifier: Modifier = Modifier, size: Int = 40) {
    Surface(
        modifier = modifier.size(size.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            SvgIcon(categoryIconKey(iconKey), Modifier.size((size * 0.55).toInt().dp))
        }
    }
}
