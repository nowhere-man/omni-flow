package com.omniflow.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.designsystem.components.OmniEmptyState
import com.omniflow.android.ui.designsystem.components.OmniSubPageScaffold
import com.omniflow.android.ui.designsystem.tokens.OmniShapeTokens
import com.omniflow.core.domain.model.Category
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.hourMinuteText

/**
 * 分类详情独立页：概览 + 二级分类列表 + 最近明细 + 查看全部。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CategoryDetailScreen(
    state: EntityDetailUiState,
    moreState: MoreUiState,
    onBack: () -> Unit,
    onLoad: () -> Unit,
    onOpenTransaction: (String) -> Unit,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(state.entityId) { onLoad() }
    val category = moreState.categories.firstOrNull { it.id == state.entityId }
    val subcategories = moreState.categories.filter { it.parentId == state.entityId }
    val recent = state.items.take(10)

    OmniSubPageScaffold(
        title = category?.name ?: "分类详情",
        onBack = onBack,
        modifier = modifier,
    ) {
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            category == null -> {
                OmniEmptyState(message = state.error ?: "未找到分类信息")
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .readableContentWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item { CategoryDetailHeader(category, subcategories) }

                    if (subcategories.isNotEmpty()) {
                        item {
                            Text(
                                "二级分类",
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        item {
                            Surface(
                                shape = OmniShapeTokens.card,
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                            ) {
                                FlowRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    subcategories.forEach { child ->
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        ) {
                                            Text(
                                                text = child.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SummaryTile("支出", state.summary.expenseTotal, MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
                            SummaryTile("收入", state.summary.incomeTotal, incomeColor(), Modifier.weight(1f))
                            SummaryTile(
                                "结余",
                                state.summary.netIncome,
                                if (state.summary.netIncome.minor >= 0) incomeColor() else expenseColor(),
                                Modifier.weight(1f),
                            )
                        }
                    }

                    item {
                        Text(
                            "最近明细",
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    when {
                        state.error != null -> item {
                            OmniEmptyState(message = state.error)
                        }
                        recent.isEmpty() -> item {
                            OmniEmptyState(message = "还没有相关交易")
                        }
                        else -> {
                            item {
                                Surface(shape = OmniShapeTokens.card, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                                    Column(Modifier.padding(horizontal = 12.dp)) {
                                        recent.forEachIndexed { index, item ->
                                            TransactionRow(
                                                iconKey = item.categoryIconKey,
                                                title = item.categoryDisplayName,
                                                subtitle = item.note,
                                                amount = item.amount,
                                                type = item.type,
                                                timeText = item.occurredAt.hourMinuteText(),
                                                onClick = { onOpenTransaction(item.id) },
                                            )
                                            if (index != recent.lastIndex) HorizontalDivider()
                                        }
                                    }
                                }
                            }
                            if (state.count > recent.size) {
                                item {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                        TextButton(onClick = onSeeAll) { Text("查看全部流水") }
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(Modifier.height(88.dp)) }
                }
            }
        }
    }
}

@Composable
private fun CategoryDetailHeader(category: Category?, subcategories: List<Category>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = OmniShapeTokens.card,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIcon(category?.iconKey, size = 56)
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    category?.name ?: "分类",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (subcategories.isEmpty()) "无二级分类" else "${subcategories.size} 个二级分类",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SummaryTile(label: String, amount: Money, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Surface(modifier = modifier, shape = OmniShapeTokens.medium, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = OmniText.caption, color = mutedContent())
            Text(
                amount.asRmb(),
                style = OmniText.amountPrimary,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
