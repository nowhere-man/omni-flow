package com.omniflow.android.ui.feature.category

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ContextualFlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.LedgerPickerBar
import com.omniflow.android.ui.MoreUiState
import com.omniflow.android.ui.OmniFlowViewModel
import com.omniflow.android.ui.designsystem.components.OmniEmptyState
import com.omniflow.android.ui.designsystem.components.OmniSubPageScaffold
import com.omniflow.android.ui.designsystem.tokens.categoryBadgeColors
import com.omniflow.android.ui.designsystem.tokens.getCategoryIcon
import com.omniflow.android.ui.label
import com.omniflow.core.domain.model.Category
import com.omniflow.core.domain.model.TransactionType

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CategoryScreen(
    state: MoreUiState,
    viewModel: OmniFlowViewModel,
    onBack: () -> Unit,
    onOpenCategory: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf<Category?>(null) }
    var newParentId by remember { mutableStateOf<String?>(null) }
    var showNew by remember { mutableStateOf(false) }
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var expandedIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    val primaries = remember(state.categories, type) {
        state.categories.filter { it.parentId == null && it.type == type }
    }

    OmniSubPageScaffold(
        title = "分类管理",
        onBack = onBack,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            LedgerPickerBar(state.ledgers, state.selectedLedgerId) {
                viewModel.selectMoreLedger(it)
            }

            // Segmented switch for Expense / Income
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
                SegmentedButton(
                    selected = type == TransactionType.EXPENSE,
                    onClick = { type = TransactionType.EXPENSE },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) {
                    Text("支出分类")
                }
                SegmentedButton(
                    selected = type == TransactionType.INCOME,
                    onClick = { type = TransactionType.INCOME },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) {
                    Text("收入分类")
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.error?.let { err ->
                    item {
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                if (primaries.isEmpty()) {
                    item {
                        OmniEmptyState(
                            status = "暂无${type.label}分类",
                            value = "点击下方按钮创建第一个一级分类",
                            actionLabel = "新建${type.label}分类",
                            onAction = {
                                newParentId = null
                                showNew = true
                            },
                            modifier = Modifier.padding(top = 32.dp),
                        )
                    }
                } else {
                    items(primaries, key = { it.id }) { primary ->
                        val children = state.categories.filter { it.parentId == primary.id }
                        val isExpanded = primary.id in expandedIds

                        PrimaryCategoryTreeCard(
                            primary = primary,
                            children = children,
                            expanded = isExpanded,
                            onToggle = {
                                expandedIds = if (isExpanded) expandedIds - primary.id else expandedIds + primary.id
                            },
                            onOpen = { onOpenCategory(primary.id) },
                            onEdit = { editing = primary },
                            onEditChild = { editing = it },
                            onAddChild = {
                                newParentId = primary.id
                                showNew = true
                            },
                        )
                    }

                    item(key = "dashed_add_primary") {
                        DashedNewCategoryCard(
                            label = "新建${type.label}分类",
                            onClick = {
                                newParentId = null
                                showNew = true
                            },
                        )
                    }
                }
            }
        }
    }

    val ledgerId = state.selectedLedgerId
    if ((showNew || editing != null) && ledgerId != null) {
        CategoryEditSheet(
            category = editing,
            presetParentId = newParentId,
            presetType = type,
            ledgerId = ledgerId,
            categories = state.categories,
            onDismiss = {
                showNew = false
                editing = null
                newParentId = null
            },
            onDelete = { id ->
                viewModel.deleteCategory(id)
                editing = null
            },
            onSave = { parent, name, icon, catType ->
                viewModel.saveCategory(editing?.id, ledgerId, parent, name, icon, catType)
                showNew = false
                editing = null
                newParentId = null
            },
        )
    }
}
