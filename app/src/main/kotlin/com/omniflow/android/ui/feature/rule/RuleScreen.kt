package com.omniflow.android.ui.feature.rule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.LedgerPickerBar
import com.omniflow.android.ui.MoreUiState
import com.omniflow.android.ui.OmniFlowViewModel
import com.omniflow.android.ui.OmniText
import com.omniflow.android.ui.ReorderableColumn
import com.omniflow.android.ui.designsystem.components.OmniEmptyState
import com.omniflow.android.ui.designsystem.components.OmniSubPageScaffold
import com.omniflow.android.ui.designsystem.tokens.OmniColorTokens
import com.omniflow.android.ui.mutedContent
import com.omniflow.core.domain.model.Rule
import com.omniflow.core.domain.model.RuleConditionType

/**
 * 规则管理页（缺陷 8.3 根治版）：
 * 规则句子卡陈列 + 40dp 圆形徽章 + 拖拽排序 + 模板新建 + 零规则计数违规。
 */
@Composable
fun RuleScreen(
    state: MoreUiState,
    viewModel: OmniFlowViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf<Rule?>(null) }
    var showTemplatePicker by remember { mutableStateOf(false) }
    var selectedTemplate by remember { mutableStateOf<RuleConditionType?>(null) }
    var showEditSheet by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<Rule?>(null) }

    val ledgerId = state.selectedLedgerId ?: state.ledgers.firstOrNull()?.id
    val orderedRules = state.rules.sortedBy(Rule::priority)

    OmniSubPageScaffold(
        title = "规则管理",
        onBack = onBack,
        modifier = modifier,
        actions = {
            if (orderedRules.isNotEmpty() && ledgerId != null) {
                Surface(
                    onClick = { showTemplatePicker = true },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "新建规则",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 顶栏账本切换条（图标圆形化）
            LedgerPickerBar(state.ledgers, ledgerId) {
                viewModel.selectMoreLedger(it)
            }

            if (orderedRules.isEmpty()) {
                OmniEmptyState(
                    status = "暂无自动规则",
                    value = "创建规则后，导入账单时将自动完成分类或排除记账",
                    actionLabel = "新建第一条规则",
                    onAction = { showTemplatePicker = true },
                    modifier = Modifier.weight(1f),
                )
            } else {
                ReorderableColumn(
                    items = orderedRules,
                    key = Rule::id,
                    rowHeight = 78.dp,
                    modifier = Modifier.weight(1f),
                    onReordered = { reordered -> viewModel.reorderRules(reordered.map(Rule::id)) },
                ) { rule, dragging ->
                    RuleSentenceCard(
                        rule = rule,
                        categories = state.categories,
                        dragging = dragging,
                        onEdit = {
                            editing = rule
                            selectedTemplate = null
                            showEditSheet = true
                        },
                    )
                }
            }
        }
    }

    // 模板选择弹窗
    if (showTemplatePicker) {
        RuleTemplatePickerDialog(
            onDismiss = { showTemplatePicker = false },
            onSelect = { template ->
                selectedTemplate = template
                editing = null
                showTemplatePicker = false
                showEditSheet = true
            },
        )
    }

    // 规则编辑/新建抽屉
    if (showEditSheet && ledgerId != null) {
        RuleEditSheet(
            rule = editing,
            initialCondition = selectedTemplate,
            state = state,
            onDismiss = {
                showEditSheet = false
                editing = null
                selectedTemplate = null
            },
            onDelete = editing?.let { rule ->
                { confirmDelete = rule }
            },
            onSave = { name, condition, conditionValue, action, actionValue, priority ->
                viewModel.saveRule(
                    editing?.id,
                    ledgerId,
                    name,
                    condition,
                    conditionValue,
                    action,
                    actionValue,
                    priority,
                )
            },
        )
    }

    // 删除确认弹窗
    confirmDelete?.let { rule ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("删除规则") },
            text = { Text("确定要删除规则「${rule.name}」吗？已完成的交易记录不会受到影响。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRule(rule.id)
                        confirmDelete = null
                    },
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
private fun RuleTemplatePickerDialog(
    onDismiss: () -> Unit,
    onSelect: (RuleConditionType) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "选择规则模板",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TemplateOptionRow(
                    title = "商户或备注关键词",
                    desc = "如交易备注包含“星巴克”时自动归入餐饮",
                    icon = Icons.Default.Store,
                    onClick = { onSelect(RuleConditionType.NOTE_CONTAINS) },
                )
                TemplateOptionRow(
                    title = "按收支类型分类",
                    desc = "如收入类型交易自动归入薪资或奖金",
                    icon = Icons.Default.SwapHoriz,
                    onClick = { onSelect(RuleConditionType.TRANSACTION_TYPE) },
                )
                TemplateOptionRow(
                    title = "按支付来源平台分类",
                    desc = "如微信或支付宝支付的特定流水规则",
                    icon = Icons.Default.Description,
                    onClick = { onSelect(RuleConditionType.TRANSACTION_SOURCE) },
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun TemplateOptionRow(
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
                Text(desc, style = OmniText.caption, color = mutedContent())
            }
        }
    }
}
