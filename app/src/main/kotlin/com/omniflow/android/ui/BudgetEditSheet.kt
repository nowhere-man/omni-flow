package com.omniflow.android.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.designsystem.components.OmniSheetScaffold
import com.omniflow.android.ui.designsystem.tokens.OmniShapeTokens
import com.omniflow.core.domain.model.BudgetProgress
import com.omniflow.core.domain.model.Category
import com.omniflow.core.domain.model.Money

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BudgetEditSheet(
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

    val selectable = categories.filterNot { it.id in usedCategoryIds && it.id != categoryId }

    OmniSheetScaffold(
        title = if (editing == null) "新增预算" else "编辑「${editing.name}」预算",
        onDismiss = onDismiss,
        saveText = "保存",
        saveEnabled = amountText.isNotBlank(),
        onSave = {
            val amount = amountText.toMoneyOrNull()
            if (amount == null || amount.minor <= 0) {
                error = "请输入大于 0 的金额，最多两位小数"
            } else {
                error = null
                onSave(categoryId, amount)
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (editing == null) {
                Text("预算范围", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = categoryId == null,
                        onClick = { categoryId = null },
                        label = { Text("账本总预算") },
                        shape = OmniShapeTokens.mediumSmall,
                    )
                    selectable.forEach { category ->
                        FilterChip(
                            selected = categoryId == category.id,
                            onClick = { categoryId = category.id },
                            label = { Text(category.name) },
                            shape = OmniShapeTokens.mediumSmall,
                        )
                    }
                }
                if (hasOverall && categoryId == null) {
                    Text(
                        text = "总预算已存在，保存会覆盖原来的额度。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            OmniTextField(
                value = amountText,
                onValueChange = { amountText = it; error = null },
                label = "每月额度",
                isError = error != null,
                supportingText = error,
            )

            if (editing != null) {
                Text(
                    text = "本月已用 ${editing.spent.asRmb()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
