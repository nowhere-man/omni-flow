package com.omniflow.android.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.designsystem.components.OmniEmptyState
import com.omniflow.android.ui.designsystem.components.OmniProgressRing
import com.omniflow.android.ui.designsystem.components.OmniSheetScaffold
import com.omniflow.android.ui.designsystem.components.OmniSubPageScaffold
import com.omniflow.android.ui.designsystem.tokens.OmniColorTokens
import com.omniflow.android.ui.designsystem.tokens.categoryBadgeColors
import com.omniflow.android.ui.designsystem.tokens.getCategoryIcon
import com.omniflow.core.domain.model.AppTimeZone
import com.omniflow.core.domain.model.BudgetProgress
import com.omniflow.core.domain.model.Category
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.TransactionType
import com.omniflow.core.domain.model.calculateDailyAllowance
import kotlinx.datetime.Clock
import kotlinx.datetime.toLocalDateTime

@Composable
internal fun BudgetPage(
    state: MoreUiState,
    viewModel: OmniFlowViewModel,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf<BudgetProgress?>(null) }
    var creating by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<BudgetProgress?>(null) }

    val overall = state.budgets.firstOrNull { it.budget.isOverall }
    val perCategory = state.budgets.filterNot { it.budget.isOverall }

    OmniSubPageScaffold(
        title = "预算管理",
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
                contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
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

                item(key = "budget_hero") {
                    if (overall == null) {
                        OmniEmptyState(
                            status = "未设置总预算",
                            value = "设置每月总预算，盯住整体开销并提供每日可用建议",
                            actionLabel = "设置总预算",
                            onAction = { creating = true },
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    } else {
                        BudgetHeroRingCard(
                            progress = overall,
                            onClick = { editing = overall },
                        )
                    }
                }

                if (perCategory.isNotEmpty()) {
                    item(key = "category_budget_header") {
                        Text(
                            text = "分类预算",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                        )
                    }

                    items(perCategory, key = { it.budget.id }) { progress ->
                        CategoryBudgetBarCard(
                            progress = progress,
                            onClick = { editing = progress },
                            onDelete = { confirmDelete = progress },
                        )
                    }
                }

                if (overall != null) {
                    item(key = "dashed_add_category_budget") {
                        DashedNewBudgetCard(onClick = { creating = true })
                    }
                }
            }
        }
    }

    if (creating || editing != null) {
        BudgetEditSheet(
            editing = editing,
            categories = state.categories.filter { it.parentId == null && it.type == TransactionType.EXPENSE },
            usedCategoryIds = state.budgets.mapNotNullTo(mutableSetOf()) { it.budget.categoryId },
            hasOverall = overall != null,
            onDismiss = { creating = false; editing = null },
        ) { categoryId, amount ->
            viewModel.saveBudget(editing?.budget?.id, categoryId, amount)
            creating = false
            editing = null
        }
    }

    confirmDelete?.let { progress ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("删除「${progress.name}」预算？") },
            text = { Text("只删除预算设置，已记录的交易不受影响。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = null
                    viewModel.deleteBudget(progress.budget.id)
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun BudgetHeroRingCard(
    progress: BudgetProgress,
    onClick: () -> Unit,
) {
    val today = remember { Clock.System.now().toLocalDateTime(AppTimeZone).date }
    val dailyAllowance = remember(progress.remaining, today) {
        calculateDailyAllowance(progress.remaining, today)
    }

    // Health color: pastel green (< 0.8) -> warning orange (0.8..1.0) -> desaturated red (> 1.0)
    val ringColor = when {
        progress.isOverspent -> OmniColorTokens.ExpenseLight
        progress.ratio >= 0.8f -> OmniColorTokens.BadgePlanningContainerLight
        else -> OmniColorTokens.IncomeLight
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "本月总预算",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (progress.isOverspent) "已超支" else "正常",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (progress.isOverspent) expenseColor() else incomeColor(),
                )
            }

            // OmniProgressRing total-budget ring
            OmniProgressRing(
                progress = progress.ratio.coerceIn(0f, 1f),
                modifier = Modifier.size(160.dp),
                strokeWidth = 14.dp,
                color = ringColor,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = if (progress.isOverspent) "已超支" else "剩余可用",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = progress.remaining.asRmb(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (progress.isOverspent) expenseColor() else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // 「今日可花」 daily-allowance capsule
            Surface(
                shape = CircleShape,
                color = if (progress.isOverspent) OmniColorTokens.ExpenseLight.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            ) {
                Text(
                    text = if (progress.isOverspent) {
                        "本月已超支，暂无每日建议额度"
                    } else {
                        "今日可花 ${dailyAllowance.dailyAllowance.asRmb()} · 本月剩 ${dailyAllowance.remainingDays} 天"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (progress.isOverspent) expenseColor() else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }

            Text(
                text = "已用 ${progress.spent.asRmb()} / 预算 ${progress.budget.amount.asRmb()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
