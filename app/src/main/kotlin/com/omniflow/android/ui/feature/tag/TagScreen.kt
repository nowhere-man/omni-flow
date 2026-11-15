package com.omniflow.android.ui.feature.tag

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.LedgerPickerBar
import com.omniflow.android.ui.MoreUiState
import com.omniflow.android.ui.OmniFlowViewModel
import com.omniflow.android.ui.designsystem.components.OmniEmptyState
import com.omniflow.android.ui.designsystem.components.OmniSheetScaffold
import com.omniflow.android.ui.designsystem.components.OmniSubPageScaffold
import com.omniflow.android.ui.designsystem.tokens.CategoryColorTokens
import com.omniflow.android.ui.designsystem.tokens.categoryBadgeColors
import com.omniflow.core.domain.model.Tag

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TagScreen(
    state: MoreUiState,
    viewModel: OmniFlowViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf<Tag?>(null) }
    var showNew by remember { mutableStateOf(false) }

    val activeTags = remember(state.tags) {
        state.tags.filter { it.deletedAt == null }
    }

    val pastelKeys = remember {
        listOf("utensils", "cart", "bus", "home", "gamepad", "heart-pulse", "landmark", "wallet")
    }

    OmniSubPageScaffold(
        title = "标签管理",
        onBack = onBack,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            LedgerPickerBar(state.ledgers, state.selectedLedgerId) {
                viewModel.selectMoreLedger(it)
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp),
            ) {
                state.error?.let { err ->
                    item {
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                    }
                }

                if (activeTags.isEmpty()) {
                    item {
                        OmniEmptyState(
                            status = "该账本还没有标签",
                            value = "创建标签，为流水添加多维分析视角",
                            actionLabel = "新建标签",
                            onAction = { showNew = true },
                            modifier = Modifier.padding(top = 40.dp),
                        )
                    }
                } else {
                    item {
                        // Flowing chip wall
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            activeTags.forEachIndexed { index, tag ->
                                val colorKey = pastelKeys[index % pastelKeys.size]
                                val badgeColors = categoryBadgeColors(colorKey)
                                val isProminent = (index % 3 == 0)

                                TagCapsuleChip(
                                    tag = tag,
                                    badgeColors = badgeColors,
                                    isProminent = isProminent,
                                    onClick = { editing = tag },
                                )
                            }

                            // Dashed add chip in the wall
                            DashedNewTagChip(onClick = { showNew = true })
                        }
                    }
                }
            }
        }
    }

    val ledgerId = state.selectedLedgerId
    if ((showNew || editing != null) && ledgerId != null) {
        TagEditSheet(
            tag = editing,
            onDismiss = {
                showNew = false
                editing = null
            },
            onDelete = { id ->
                viewModel.deleteTag(id)
                editing = null
            },
            onSave = { name ->
                viewModel.saveTag(editing?.id, ledgerId, name)
                showNew = false
                editing = null
            },
        )
    }
}

@Composable
private fun TagCapsuleChip(
    tag: Tag,
    badgeColors: com.omniflow.android.ui.designsystem.tokens.CategoryBadgeColors,
    isProminent: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = badgeColors.container,
        modifier = Modifier.height(if (isProminent) 38.dp else 32.dp),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (isProminent) 14.dp else 10.dp,
                vertical = if (isProminent) 6.dp else 4.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "#",
                style = if (isProminent) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = badgeColors.content.copy(alpha = 0.7f),
            )
            Text(
                text = tag.name,
                style = if (isProminent) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                fontWeight = if (isProminent) FontWeight.SemiBold else FontWeight.Medium,
                color = badgeColors.content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DashedNewTagChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strokeColor = MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 40.dp)
            .height(38.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRoundRect(
                color = strokeColor,
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f),
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(17.dp.toPx()),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = "新建标签",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun TagEditSheet(
    tag: Tag?,
    onDismiss: () -> Unit,
    onDelete: (String) -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember(tag) { mutableStateOf(tag?.name.orEmpty()) }
    var confirmDelete by remember(tag) { mutableStateOf(false) }

    OmniSheetScaffold(
        title = if (tag == null) "新建标签" else "编辑标签",
        onDismiss = onDismiss,
        saveText = "保存",
        saveEnabled = name.isNotBlank(),
        onSave = { onSave(name.trim()) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("标签名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )

            if (tag != null) {
                TextButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("删除标签", color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (confirmDelete && tag != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除标签「${tag.name}」？") },
            text = { Text("已经绑定此标签的交易不会被删除，仅移除该标签。") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete(tag.id) }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } },
        )
    }
}
