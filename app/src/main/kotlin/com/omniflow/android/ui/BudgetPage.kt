package com.omniflow.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omniflow.core.domain.model.BudgetProgress
import com.omniflow.core.domain.model.Category
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.TransactionType

/**
 * 月度预算：一条账本总预算 + 每个一级分类各一条。
 * 进度按当月已发生的支出算，超支时进度条和剩余金额转成支出语义色。
 */
@Composable
internal fun BudgetPage(state: MoreUiState, viewModel: OmniFlowViewModel) {
    var editing by remember { mutableStateOf<BudgetProgress?>(null) }
    var creating by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<BudgetProgress?>(null) }
    val overall = state.budgets.firstOrNull { it.budget.isOverall }
    val perCategory = state.budgets.filterNot { it.budget.isOverall }

    Column(Modifier.fillMaxSize()) {
        LedgerPickerBar(state.ledgers, state.selectedLedgerId) { viewModel.selectMoreLedger(it) }
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Button(onClick = { creating = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (overall == null) "设置总预算" else "新增分类预算")
                }
            }
            state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            item {
                if (overall == null) {
                    Text(
                        "还没有设置预算。总预算用来盯住这个月的整体开销，分类预算用来管住某一类。",
                        style = OmniText.caption,
                        color = mutedContent(),
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                } else {
                    BudgetHeroCard(overall, onClick = { editing = overall })
                }
            }
            if (perCategory.isNotEmpty()) {
                item { ManagementGroupHeader("分类预算", "${perCategory.size} 项") }
            }
            items(perCategory, key = { it.budget.id }) { progress ->
                BudgetRow(
                    progress = progress,
                    onClick = { editing = progress },
                    onDelete = { confirmDelete = progress },
                )
            }
            item { Spacer(Modifier.height(88.dp)) }
        }
    }

    if (creating || editing != null) {
        BudgetSheet(
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
                TextButton(onClick = { confirmDelete = null; viewModel.deleteBudget(progress.budget.id) }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun BudgetHeroCard(progress: BudgetProgress, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(OmniRadius.large),
        color = surfaceCard(),
    ) {
        Column(Modifier.padding(OmniSpace.l), verticalArrangement = Arrangement.spacedBy(OmniSpace.m)) {
            Text("本月总预算", style = OmniText.caption, color = mutedContent())
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    progress.remaining.asRmb(),
                    style = OmniText.amountHero,
                    color = if (progress.isOverspent) expenseColor() else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    if (progress.isOverspent) "已超支" else "可用",
                    style = OmniText.caption,
                    color = mutedContent(),
                )
            }
            BudgetBar(progress)
            Text(
                "已用 ${progress.spent.asRmb()} / 预算 ${progress.budget.amount.asRmb()}",
                style = OmniText.caption,
                color = mutedContent(),
            )
        }
    }
}

@Composable
private fun BudgetRow(progress: BudgetProgress, onClick: () -> Unit, onDelete: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(OmniRadius.medium),
        color = surfaceInset(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundIcon(progress.iconKey, size = 34)
                Spacer(Modifier.size(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(progress.name, style = OmniText.titleRow, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "已用 ${progress.spent.asRmb()} / ${progress.budget.amount.asRmb()}",
                        style = OmniText.caption,
                        color = mutedContent(),
                        maxLines = 1,
                    )
                }
                Text(
                    "${(progress.ratio * 100).toInt()}%",
                    style = OmniText.titleRow,
                    color = if (progress.isOverspent) expenseColor() else mutedContent(),
                )
                TextButton(onClick = onDelete) { Text("删除", color = MaterialTheme.colorScheme.error) }
            }
            BudgetBar(progress)
        }
    }
}

@Composable
private fun BudgetBar(progress: BudgetProgress) {
    LinearProgressIndicator(
        progress = { progress.ratio.coerceIn(0f, 1f) },
        modifier = Modifier.fillMaxWidth().height(8.dp),
        color = if (progress.isOverspent) expenseColor() else incomeColor(),
        trackColor = MaterialTheme.colorScheme.surfaceBright,
        strokeCap = StrokeCap.Round,
    )
}

@Composable
private fun BudgetSheet(
    editing: BudgetProgress?,
    categories: List<Category>,
    usedCategoryIds: Set<String>,
    hasOverall: Boolean,
    onDismiss: () -> Unit,
    onSave: (categoryId: String?, amount: Money) -> Unit,
) {
    var categoryId by remember(editing) { mutableStateOf(editing?.budget?.categoryId) }
    var amountText by remember(editing) { mutableStateOf(editing?.budget?.amount?.toDecimal().orEmpty()) }
    var error by remember(editing) { mutableStateOf<String?>(null) }
    // 编辑时范围不可改；新建时把已经有预算的分类排除掉，避免建出重复项
    val selectable = categories.filterNot { it.id in usedCategoryIds || it.id == categoryId }
    ManagementSheet(
        title = if (editing == null) "新增预算" else "编辑「${editing.name}」预算",
        onDismiss = onDismiss,
        onSave = {
            val amount = amountText.toMoneyOrNull()
            if (amount == null || amount.minor <= 0) {
                error = "请输入大于 0 的金额，最多两位小数"
            } else {
                error = null
                onSave(categoryId, amount)
            }
        },
        saveEnabled = amountText.isNotBlank(),
    ) {
        if (editing == null) {
            Text("预算范围", style = MaterialTheme.typography.labelLarge)
            NullableValueMenu(
                label = categories.firstOrNull { it.id == categoryId }?.name
                    ?: if (hasOverall) "选择一级分类" else "账本总预算",
                allLabel = "账本总预算",
                values = selectable,
                valueLabel = Category::name,
                onAll = { categoryId = null },
                onSelected = { categoryId = it.id },
            )
            if (hasOverall && categoryId == null) {
                Text("总预算已存在，保存会覆盖原来的额度。", style = OmniText.caption, color = mutedContent())
            }
        }
        OutlinedTextField(
            amountText,
            { amountText = it; error = null },
            label = { Text("每月额度") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            isError = error != null,
            supportingText = { error?.let { Text(it) } },
        )
        if (editing != null) {
            Text(
                "本月已用 ${editing.spent.asRmb()}",
                style = OmniText.caption,
                color = mutedContent(),
                textAlign = TextAlign.Start,
            )
        }
    }
}
