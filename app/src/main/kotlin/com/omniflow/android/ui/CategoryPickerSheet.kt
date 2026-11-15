package com.omniflow.android.ui

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.designsystem.components.OmniSheetScaffold
import com.omniflow.android.ui.designsystem.tokens.OmniShapeTokens
import com.omniflow.android.ui.feature.editor.components.CategoryTile
import com.omniflow.core.domain.model.Category
import com.omniflow.core.domain.model.TransactionType

/**
 * 统一分类选择抽屉（承载导入/批量修改与搜索筛选双重语义）。
 * 基于 OmniSheetScaffold 标准抽屉构建。
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

    OmniSheetScaffold(
        title = title,
        onDismiss = onDismiss,
        cancelText = "取消",
        onCancel = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = listOf(TransactionType.EXPENSE, TransactionType.INCOME)
                options.forEachIndexed { index, opt ->
                    SegmentedButton(
                        selected = type == opt,
                        onClick = {
                            type = opt
                            primaryId = null
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        label = { Text(if (opt == TransactionType.EXPENSE) "支出" else "收入") },
                    )
                }
            }

            if (primaryCategories.isEmpty()) {
                Text(
                    text = "当前账本还没有该类型的分类",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.heightIn(max = 260.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(primaryCategories, key = { it.id }) { category ->
                        val hasChildren = categories.any { it.parentId == category.id && it.type == type }
                        CategoryTile(
                            category = category,
                            selected = primaryId == category.id,
                            onClick = {
                                if (primaryId == category.id || !hasChildren) {
                                    onSelected(category.id, type)
                                } else {
                                    primaryId = category.id
                                }
                            },
                            modifier = Modifier.height(76.dp),
                        )
                    }
                }
            }

            if (secondaryCategories.isNotEmpty()) {
                Text("细分到二级分类", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    secondaryCategories.forEach { category ->
                        FilterChip(
                            selected = selectedCategoryId == category.id,
                            onClick = { onSelected(category.id, type) },
                            label = { Text(category.name) },
                            shape = OmniShapeTokens.small,
                        )
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                    onClick = {
                        val currentPrimary = primaryId
                        if (currentPrimary != null) {
                            onSelected(currentPrimary, type)
                        } else {
                            onDismiss()
                        }
                    },
                    shape = OmniShapeTokens.textButton,
                ) {
                    Text("完成")
                }
            }
        }
    }
}

/**
 * 搜索筛选专用重载：支持清除分类与全部/二级穿透选择。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CategoryPickerSheet(
    categories: List<Category>,
    initialType: TransactionType?,
    selectedPrimaryName: String,
    selectedSecondaryName: String,
    onDismiss: () -> Unit,
    onSelected: (primary: Category?, secondary: Category?) -> Unit,
    title: String = "选择分类",
) {
    var type by remember {
        mutableStateOf(
            when (initialType) {
                TransactionType.INCOME -> TransactionType.INCOME
                else -> TransactionType.EXPENSE
            },
        )
    }

    var selectedPrimary by remember {
        mutableStateOf(
            categories.firstOrNull {
                it.parentId == null && it.name == selectedPrimaryName && (initialType == null || it.type == type)
            },
        )
    }

    var selectedSecondary by remember {
        mutableStateOf(
            categories.firstOrNull {
                it.parentId != null && it.name == selectedSecondaryName && (initialType == null || it.type == type)
            },
        )
    }

    val primaryCategories = categories.filter { it.parentId == null && it.type == type }
    val secondaryCategories = selectedPrimary?.let { p ->
        categories.filter { it.parentId == p.id && it.type == type }
    }.orEmpty()

    OmniSheetScaffold(
        title = title,
        onDismiss = onDismiss,
        trailingAction = {
            TextButton(
                onClick = {
                    onSelected(null, null)
                    onDismiss()
                },
                shape = OmniShapeTokens.textButton,
            ) {
                Text("清除分类")
            }
        },
        cancelText = "取消",
        onCancel = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (initialType == null || initialType == TransactionType.EXPENSE || initialType == TransactionType.INCOME) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val options = listOf(TransactionType.EXPENSE, TransactionType.INCOME)
                    options.forEachIndexed { index, opt ->
                        SegmentedButton(
                            selected = type == opt,
                            onClick = {
                                type = opt
                                selectedPrimary = null
                                selectedSecondary = null
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                            label = { Text(if (opt == TransactionType.EXPENSE) "支出" else "收入") },
                        )
                    }
                }
            }

            if (primaryCategories.isEmpty()) {
                Text(
                    text = "暂无可用分类",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.heightIn(max = 240.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(primaryCategories, key = { it.id }) { category ->
                        val hasChildren = categories.any { it.parentId == category.id && it.type == type }
                        CategoryTile(
                            category = category,
                            selected = selectedPrimary?.id == category.id,
                            onClick = {
                                if (selectedPrimary?.id == category.id) {
                                    if (!hasChildren) {
                                        onSelected(category, null)
                                        onDismiss()
                                    }
                                } else {
                                    selectedPrimary = category
                                    selectedSecondary = null
                                    if (!hasChildren) {
                                        onSelected(category, null)
                                        onDismiss()
                                    }
                                }
                            },
                            modifier = Modifier.height(76.dp),
                        )
                    }
                }
            }

            if (secondaryCategories.isNotEmpty()) {
                Text(
                    text = "二级分类（可选）",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterChip(
                        selected = selectedSecondary == null,
                        onClick = {
                            selectedSecondary = null
                            selectedPrimary?.let {
                                onSelected(it, null)
                                onDismiss()
                            }
                        },
                        label = { Text("全部") },
                        shape = OmniShapeTokens.small,
                    )
                    secondaryCategories.forEach { sub ->
                        FilterChip(
                            selected = selectedSecondary?.id == sub.id,
                            onClick = {
                                selectedSecondary = sub
                                selectedPrimary?.let { p ->
                                    onSelected(p, sub)
                                    onDismiss()
                                }
                            },
                            label = { Text(sub.name) },
                            shape = OmniShapeTokens.small,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = {
                        val p = selectedPrimary
                        if (p != null) {
                            onSelected(p, selectedSecondary)
                        }
                        onDismiss()
                    },
                    shape = OmniShapeTokens.textButton,
                ) {
                    Text("完成")
                }
            }
        }
    }
}
