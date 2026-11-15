package com.omniflow.android.ui.feature.rule

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.MoreUiState
import com.omniflow.android.ui.OmniRadius
import com.omniflow.android.ui.OmniText
import com.omniflow.android.ui.OmniTextField
import com.omniflow.android.ui.designsystem.components.OmniSheetScaffold
import com.omniflow.android.ui.designsystem.tokens.OmniShapeTokens
import com.omniflow.android.ui.mutedContent
import com.omniflow.android.ui.surfaceInset
import com.omniflow.core.domain.model.Rule
import com.omniflow.core.domain.model.RuleActionType
import com.omniflow.core.domain.model.RuleConditionType
import com.omniflow.core.domain.model.TransactionType

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun RuleEditSheet(
    rule: Rule?,
    initialCondition: RuleConditionType?,
    state: MoreUiState,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)?,
    onSave: (name: String, condition: RuleConditionType, conditionValue: String, action: RuleActionType, actionValue: String, priority: Int) -> Unit,
) {
    var name by remember(rule) { mutableStateOf(rule?.name.orEmpty()) }
    var condition by remember(rule, initialCondition) {
        mutableStateOf(rule?.conditionType ?: initialCondition ?: RuleConditionType.NOTE_CONTAINS)
    }
    var conditionValue by remember(rule) {
        mutableStateOf(
            rule?.conditionValue ?: when (condition) {
                RuleConditionType.NOTE_CONTAINS -> ""
                RuleConditionType.TRANSACTION_TYPE -> TransactionType.EXPENSE.name
                RuleConditionType.TRANSACTION_SOURCE -> "微信"
            },
        )
    }
    var action by remember(rule) { mutableStateOf(rule?.actionType ?: RuleActionType.SET_CATEGORY) }
    var actionValue by remember(rule) {
        mutableStateOf(rule?.actionValue ?: state.categories.firstOrNull()?.id.orEmpty())
    }
    var priorityText by remember(rule) { mutableStateOf((rule?.priority ?: ((state.rules.maxOfOrNull { it.priority } ?: 0) + 10)).toString()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    OmniSheetScaffold(
        title = if (rule == null) "新建规则" else "编辑规则",
        onDismiss = onDismiss,
        saveEnabled = name.isNotBlank(),
        onSave = {
            val trimmedName = name.trim()
            val trimmedCondValue = conditionValue.trim()
            val priority = priorityText.toIntOrNull() ?: 10

            when {
                trimmedName.isEmpty() -> errorMessage = "规则名称不能为空"
                trimmedCondValue.isEmpty() -> errorMessage = "条件内容不能为空"
                action == RuleActionType.SET_CATEGORY && state.categories.none { it.id == actionValue } -> errorMessage = "请选择有效分类"
                else -> {
                    onSave(trimmedName, condition, trimmedCondValue, action, actionValue, priority)
                    onDismiss()
                }
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 规则名称
            OmniTextField(
                value = name,
                onValueChange = { name = it; errorMessage = null },
                label = "规则名称",
                placeholder = "例如：星巴克自动归类、工资入账",
            )

            // IF 积木块：条件定义
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = surfaceInset(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        "IF (当满足条件时)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )

                    // 条件类型 Chip
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RuleConditionType.entries.forEach { condType ->
                            FilterChip(
                                selected = condition == condType,
                                onClick = {
                                    condition = condType
                                    conditionValue = when (condType) {
                                        RuleConditionType.NOTE_CONTAINS -> ""
                                        RuleConditionType.TRANSACTION_TYPE -> TransactionType.EXPENSE.name
                                        RuleConditionType.TRANSACTION_SOURCE -> "微信"
                                    }
                                    errorMessage = null
                                },
                                label = {
                                    Text(
                                        when (condType) {
                                            RuleConditionType.NOTE_CONTAINS -> "备注包含"
                                            RuleConditionType.TRANSACTION_TYPE -> "收支类型"
                                            RuleConditionType.TRANSACTION_SOURCE -> "来源平台"
                                        },
                                    )
                                },
                                shape = OmniShapeTokens.mediumSmall,
                            )
                        }
                    }

                    // 条件值输入
                    when (condition) {
                        RuleConditionType.NOTE_CONTAINS -> {
                            OmniTextField(
                                value = conditionValue,
                                onValueChange = { conditionValue = it; errorMessage = null },
                                label = "包含关键词",
                                placeholder = "例如：星巴克、全家、滴滴",
                            )
                        }
                        RuleConditionType.TRANSACTION_TYPE -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    TransactionType.EXPENSE.name to "支出",
                                    TransactionType.INCOME.name to "收入",
                                    TransactionType.TRANSFER.name to "转账",
                                ).forEach { (typeVal, typeLabel) ->
                                    FilterChip(
                                        selected = conditionValue == typeVal,
                                        onClick = { conditionValue = typeVal },
                                        label = { Text(typeLabel) },
                                        shape = OmniShapeTokens.mediumSmall,
                                    )
                                }
                            }
                        }
                        RuleConditionType.TRANSACTION_SOURCE -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("微信", "支付宝", "云闪付", "银行").forEach { src ->
                                    FilterChip(
                                        selected = conditionValue == src,
                                        onClick = { conditionValue = src },
                                        label = { Text(src) },
                                        shape = OmniShapeTokens.mediumSmall,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // THEN 积木块：动作定义
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = surfaceInset(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        "THEN (执行动作)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RuleActionType.entries.forEach { actType ->
                            FilterChip(
                                selected = action == actType,
                                onClick = {
                                    action = actType
                                    if (actType == RuleActionType.SET_CATEGORY && actionValue.isEmpty()) {
                                        actionValue = state.categories.firstOrNull()?.id.orEmpty()
                                    }
                                    errorMessage = null
                                },
                                label = {
                                    Text(
                                        when (actType) {
                                            RuleActionType.SET_CATEGORY -> "设置分类"
                                            RuleActionType.SET_EXCLUDED -> "设为不计入收支"
                                            RuleActionType.EXCLUDE -> "排除不入账"
                                        },
                                    )
                                },
                                shape = OmniShapeTokens.mediumSmall,
                            )
                        }
                    }

                    if (action == RuleActionType.SET_CATEGORY) {
                        Text("选择目标分类", style = OmniText.caption, color = mutedContent())
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            state.categories.forEach { category ->
                                FilterChip(
                                    selected = actionValue == category.id,
                                    onClick = { actionValue = category.id },
                                    label = { Text(category.name) },
                                    shape = OmniShapeTokens.mediumSmall,
                                )
                            }
                        }
                    }
                }
            }

            // 优先级设置
            OmniTextField(
                value = priorityText,
                onValueChange = { priorityText = it.filter(Char::isDigit) },
                label = "优先级数值 (数值越小越先执行)",
                placeholder = "10",
            )

            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = OmniText.caption)
            }

            if (onDelete != null) {
                OutlinedButton(
                    onClick = {
                        onDelete()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = OmniShapeTokens.button,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("删除此规则")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
