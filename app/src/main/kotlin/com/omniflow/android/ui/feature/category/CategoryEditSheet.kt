package com.omniflow.android.ui.feature.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.NullableValueMenu
import com.omniflow.android.ui.designsystem.components.OmniSheetScaffold
import com.omniflow.android.ui.designsystem.tokens.CategoryIconOptions
import com.omniflow.android.ui.designsystem.tokens.categoryBadgeColors
import com.omniflow.android.ui.designsystem.tokens.getCategoryIcon
import com.omniflow.core.domain.model.Category
import com.omniflow.core.domain.model.TransactionType

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun CategoryEditSheet(
    category: Category?,
    presetParentId: String?,
    presetType: TransactionType,
    ledgerId: String,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onDelete: (String) -> Unit,
    onSave: (parentId: String?, name: String, iconKey: String?, type: TransactionType) -> Unit,
) {
    var type by remember(category) { mutableStateOf(category?.type ?: presetType) }
    var parentId by remember(category) { mutableStateOf(category?.parentId ?: presetParentId) }
    var name by remember(category) { mutableStateOf(category?.name.orEmpty()) }
    var icon by remember(category) { mutableStateOf(category?.iconKey ?: CategoryIconOptions.first().key) }
    var confirmDelete by remember(category) { mutableStateOf(false) }

    val primaryCandidates = remember(categories, ledgerId, type, category) {
        categories.filter {
            it.ledgerId == ledgerId && it.parentId == null && it.type == type && it.id != category?.id
        }
    }

    val badgeColors = categoryBadgeColors(icon)

    OmniSheetScaffold(
        title = if (category == null) "新建分类" else "编辑分类",
        onDismiss = onDismiss,
        saveText = "保存",
        saveEnabled = name.isNotBlank(),
        onSave = {
            onSave(parentId, name.trim(), if (parentId == null) icon else null, type)
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Live Preview Tile
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(badgeColors.container),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(icon),
                            contentDescription = null,
                            tint = badgeColors.content,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Text(
                        text = name.ifBlank { "分类名称" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Type segmented buttons
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = type == TransactionType.EXPENSE,
                    onClick = { type = TransactionType.EXPENSE; parentId = null },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) {
                    Text("支出")
                }
                SegmentedButton(
                    selected = type == TransactionType.INCOME,
                    onClick = { type = TransactionType.INCOME; parentId = null },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) {
                    Text("收入")
                }
            }

            // Parent hierarchy menu
            NullableValueMenu(
                label = primaryCandidates.firstOrNull { it.id == parentId }?.name ?: "作为一级分类",
                allLabel = "作为一级分类",
                values = primaryCandidates,
                valueLabel = Category::name,
                onAll = { parentId = null },
                onSelected = { parentId = it.id },
            )

            // Name field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("分类名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )

            // Icon grid for primary categories
            if (parentId == null) {
                Text(
                    text = "选择图标",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(CategoryIconOptions) { option ->
                        val isSelected = icon == option.key
                        val optionColors = categoryBadgeColors(option.key)
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) optionColors.container else MaterialTheme.colorScheme.surfaceContainerLow)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(14.dp),
                                )
                                .clickable { icon = option.key },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(option.key),
                                contentDescription = option.label,
                                tint = if (isSelected) optionColors.content else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }
                }
            }

            // Delete action
            if (category != null) {
                TextButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("删除分类", color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (confirmDelete && category != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除分类「${category.name}」？") },
            text = { Text("已经记到这个分类下的交易不会被删除，但会失去分类。此操作无法撤销。") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete(category.id) }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } },
        )
    }
}
