package com.omniflow.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import com.omniflow.android.ui.designsystem.components.OmniExpressiveSwitch
import com.omniflow.android.ui.designsystem.components.OmniSettingsGroup
import com.omniflow.android.ui.designsystem.tokens.OmniShapeTokens
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.designsystem.tokens.CategoryIconOption
import com.omniflow.android.ui.designsystem.tokens.CategoryIconOptions
import com.omniflow.core.domain.model.Account
import com.omniflow.core.domain.model.AccountType
import com.omniflow.core.domain.model.Category
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.Reminder
import com.omniflow.core.domain.model.ReminderSchedule
import com.omniflow.core.domain.model.ReminderScheduleKind
import com.omniflow.core.domain.model.ReminderType
import com.omniflow.core.domain.model.Rule
import com.omniflow.core.domain.model.RuleActionType
import com.omniflow.core.domain.model.RuleConditionType
import com.omniflow.core.domain.model.Tag
import com.omniflow.core.domain.model.TransactionType

@Composable
internal fun ManagementPage(
    page: MorePage,
    state: MoreUiState,
    viewModel: OmniFlowViewModel,
    onPage: (MorePage) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenAccount: (String) -> Unit,
    onOpenCategory: (String) -> Unit,
) {
    when (page) {
        MorePage.RULES -> RuleManagement(state, viewModel)
        else -> Unit
    }
}



@Composable
private fun RuleManagement(state: MoreUiState, viewModel: OmniFlowViewModel) {
    var editing by remember { mutableStateOf<Rule?>(null) }
    var showNew by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<Rule?>(null) }
    val orderedRules = state.rules.sortedBy(Rule::priority)
    Column(Modifier.fillMaxSize()) {
        LedgerPickerBar(state.ledgers, state.selectedLedgerId) { viewModel.selectMoreLedger(it) }
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { Button(onClick = { showNew = true }, modifier = Modifier.fillMaxWidth()) { Text("新建规则") } }
            state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            if (orderedRules.isEmpty()) {
                item {
                    Text(
                        "还没有规则。规则会在导入账单时按顺序自动套用。",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        textAlign = TextAlign.Center,
                        color = mutedContent(),
                    )
                }
            } else {
                item {
                    Text("长按可拖动调整优先级，靠前的先生效", style = OmniText.caption, color = mutedContent())
                }
                item {
                    ReorderableColumn(
                        items = orderedRules,
                        key = Rule::id,
                        rowHeight = 76.dp,
                        resetSignal = state.error,
                        onReordered = { reordered -> viewModel.reorderRules(reordered.map(Rule::id)) },
                    ) { rule, dragging ->
                        RuleRow(
                            rule = rule,
                            dragging = dragging,
                            onEdit = { editing = rule },
                            onDelete = { confirmDelete = rule },
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(88.dp)) }
        }
    }
    val ledgerId = state.selectedLedgerId
    if ((showNew || editing != null) && ledgerId != null) {
        RuleDialog(editing, state, onDismiss = { showNew = false; editing = null }) { name, condition, conditionValue, action, actionValue, priority ->
            viewModel.saveRule(editing?.id, ledgerId, name, condition, conditionValue, action, actionValue, priority)
            showNew = false
            editing = null
        }
    }
    confirmDelete?.let { rule ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("删除规则“${rule.name}”？") },
            text = { Text("此操作无法撤销。") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = null; viewModel.deleteRule(rule.id) }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun RuleRow(rule: Rule, dragging: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(OmniRadius.medium),
        color = if (dragging) MaterialTheme.colorScheme.surfaceBright else surfaceInset(),
        tonalElevation = if (dragging) 6.dp else 0.dp,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${rule.priority + 1}",
                modifier = Modifier.padding(end = 12.dp),
                style = OmniText.titleRow,
                color = mutedContent(),
            )
            Column(Modifier.weight(1f)) {
                Text(rule.name, style = OmniText.titleRow, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${rule.conditionType.label}: ${rule.conditionValue} → ${rule.actionType.label}",
                    style = OmniText.caption,
                    color = mutedContent(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(onClick = onEdit) { Text("编辑") }
            TextButton(onClick = onDelete) { Text("删除", color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun RuleDialog(
    rule: Rule?,
    state: MoreUiState,
    onDismiss: () -> Unit,
    onSave: (String, RuleConditionType, String, RuleActionType, String, Int) -> Unit,
) {
    var name by remember(rule) { mutableStateOf(rule?.name.orEmpty()) }
    var condition by remember(rule) { mutableStateOf(rule?.conditionType ?: RuleConditionType.NOTE_CONTAINS) }
    var conditionValue by remember(rule) { mutableStateOf(rule?.conditionValue.orEmpty()) }
    var action by remember(rule) { mutableStateOf(rule?.actionType ?: RuleActionType.SET_CATEGORY) }
    var actionValue by remember(rule) { mutableStateOf(rule?.actionValue.orEmpty()) }
    var priority by remember(rule) { mutableStateOf((rule?.priority ?: state.rules.size).toString()) }
    var validationError by remember(rule) { mutableStateOf<String?>(null) }
    FormDialog(if (rule == null) "新建规则" else "编辑规则", onDismiss, {
        validationError = when {
            name.isBlank() -> "请输入规则名称"
            conditionValue.isBlank() -> "请输入匹配值"
            action == RuleActionType.SET_CATEGORY && state.categories.none { it.id == actionValue } -> "请选择有效分类"
            else -> null
        }
        if (validationError == null) {
            onSave(name, condition, conditionValue, action, actionValue, priority.toIntOrNull() ?: 0)
        }
    }) {
        OutlinedTextField(name, { name = it }, label = { Text("名称") }, modifier = Modifier.fillMaxWidth())
        ValueMenu(condition.label, RuleConditionType.entries, { it.label }) { selected ->
            condition = selected
            conditionValue = if (selected == RuleConditionType.TRANSACTION_TYPE) TransactionType.EXPENSE.name else ""
            validationError = null
        }
        if (condition == RuleConditionType.TRANSACTION_TYPE) {
            ValueMenu(
                TransactionType.entries.firstOrNull { it.name == conditionValue }?.label ?: "选择收支类型",
                TransactionType.entries,
                { it.label },
            ) { conditionValue = it.name }
        } else {
            OutlinedTextField(conditionValue, { conditionValue = it }, label = { Text("匹配值") }, modifier = Modifier.fillMaxWidth())
        }
        ValueMenu(action.label, RuleActionType.entries, { it.label }) { selected ->
            action = selected
            actionValue = ""
            validationError = null
        }
        if (action == RuleActionType.SET_CATEGORY) {
            ValueMenu(
                state.categories.firstOrNull { it.id == actionValue }?.name ?: "选择分类",
                state.categories,
                Category::name,
            ) { actionValue = it.id }
        }
        OutlinedTextField(priority, { priority = it.filter(Char::isDigit) }, label = { Text("优先级") }, modifier = Modifier.fillMaxWidth())
        validationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}



@Composable
private fun NumberField(label: String, value: String, onValue: (String) -> Unit) {
    OutlinedTextField(value, { onValue(it.filter(Char::isDigit)) }, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun FormDialog(
    title: String,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp), content = content) },
        confirmButton = { TextButton(onClick = onSave) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

internal val TransactionType.label: String get() = if (this == TransactionType.EXPENSE) "支出" else "收入"
internal val ReminderType.label: String get() = if (this == ReminderType.REPAYMENT) "还款提醒" else "订阅提醒"
internal val ReminderScheduleKind.label: String get() = when (this) {
    ReminderScheduleKind.FIXED_REPAYMENT_DAY -> "固定还款日"
    ReminderScheduleKind.DAYS_AFTER_STATEMENT -> "账单日后 N 天"
    ReminderScheduleKind.DAILY -> "每天"
    ReminderScheduleKind.WEEKLY -> "每周"
    ReminderScheduleKind.MONTHLY -> "每月"
    ReminderScheduleKind.YEARLY -> "每年"
}

private val RuleConditionType.label: String get() = when (this) {
    RuleConditionType.NOTE_CONTAINS -> "备注包含"
    RuleConditionType.TRANSACTION_TYPE -> "收支类型"
    RuleConditionType.TRANSACTION_SOURCE -> "来源平台"
}

private val RuleActionType.label: String get() = when (this) {
    RuleActionType.SET_CATEGORY -> "设置分类"
    RuleActionType.SET_EXCLUDED -> "设为不计入收支"
    RuleActionType.EXCLUDE -> "排除不入账"
}

internal fun String.toMoneyOrNull(): Money? {
    val value = trim().toBigDecimalOrNull() ?: return null
    if (value.scale() > 2) return null
    return runCatching { Money(value.movePointRight(2).longValueExact()) }.getOrNull()
}


