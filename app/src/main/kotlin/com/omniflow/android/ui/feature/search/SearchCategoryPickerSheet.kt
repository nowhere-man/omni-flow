package com.omniflow.android.ui.feature.search

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
import androidx.compose.foundation.horizontalScroll
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
import com.omniflow.android.ui.OmniSegmented
import com.omniflow.android.ui.feature.editor.components.CategoryTile
import com.omniflow.core.domain.model.Category
import com.omniflow.core.domain.model.TransactionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchCategoryPickerSheet(
    categories: List<Category>,
    initialType: TransactionType?,
    selectedPrimaryName: String,
    selectedSecondaryName: String,
    onDismiss: () -> Unit,
    onSelected: (primary: Category?, secondary: Category?) -> Unit,
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

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "选择分类",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                TextButton(
                    onClick = {
                        onSelected(null, null)
                        onDismiss()
                    },
                ) {
                    Text("清除分类")
                }
            }

            if (initialType == null || initialType == TransactionType.EXPENSE || initialType == TransactionType.INCOME) {
                OmniSegmented(
                    options = listOf(TransactionType.EXPENSE, TransactionType.INCOME),
                    selected = type,
                    label = { if (it == TransactionType.EXPENSE) "支出" else "收入" },
                    onSelected = {
                        type = it
                        selectedPrimary = null
                        selectedSecondary = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
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
                                    // Clicking already selected primary
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
                ) {
                    Text("完成")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
