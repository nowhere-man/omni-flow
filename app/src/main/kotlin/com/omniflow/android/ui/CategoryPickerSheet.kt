package com.omniflow.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omniflow.core.domain.model.Category
import com.omniflow.core.domain.model.TransactionType

/**
 * 导入预览用的分类选择器，观感对齐记账页：先选收支类型，再选一级分类图标，
 * 有二级分类时在下方展开。选中即回调关闭，不需要额外的「确定」。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CategoryPickerSheet(
    title: String,
    categories: List<Category>,
    selectedCategoryId: String?,
    initialType: TransactionType,
    onDismiss: () -> Unit,
    onSelected: (categoryId: String, type: TransactionType) -> Unit,
) {
    var type by remember { mutableStateOf(initialType) }
    var primaryId by remember {
        mutableStateOf(
            categories.firstOrNull { it.id == selectedCategoryId }
                ?.let { it.parentId ?: it.id },
        )
    }
    val primaryCategories = categories.filter { it.parentId == null && it.type == type }
    val secondaryCategories = categories.filter { it.parentId == primaryId && it.type == type }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TransactionType.entries.forEach { entry ->
                    FilterChip(
                        selected = type == entry,
                        onClick = {
                            type = entry
                            primaryId = null
                        },
                        label = { Text(if (entry == TransactionType.EXPENSE) "支出" else "收入") },
                    )
                }
            }
            if (primaryCategories.isEmpty()) {
                Text("当前账本还没有该类型的分类", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.heightIn(max = 280.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(primaryCategories, key = { it.id }) { category ->
                        CategoryTile(
                            category = category,
                            selected = primaryId == category.id,
                            onClick = {
                                primaryId = category.id
                                // 一级分类本身就是可用分类；点了就先落一级，再按需细化到二级。
                                onSelected(category.id, type)
                            },
                            modifier = Modifier.height(76.dp),
                        )
                    }
                }
            }
            if (secondaryCategories.isNotEmpty()) {
                Text("细分到二级分类", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    secondaryCategories.forEach { category ->
                        FilterChip(
                            selected = selectedCategoryId == category.id,
                            onClick = { onSelected(category.id, type) },
                            label = { Text(category.name) },
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("完成") }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
