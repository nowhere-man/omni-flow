package com.omniflow.android.ui.feature.ledger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.asRmb
import com.omniflow.android.ui.designsystem.components.OmniSheetScaffold
import com.omniflow.android.ui.designsystem.tokens.OmniShapeTokens
import com.omniflow.android.ui.designsystem.tokens.OmniSpacingTokens
import com.omniflow.android.ui.expenseColor
import com.omniflow.android.ui.incomeColor
import com.omniflow.core.domain.model.Ledger
import com.omniflow.core.domain.model.LedgerCoverSpec
import com.omniflow.core.domain.model.LedgerOverview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LedgerSheet(
    ledger: Ledger?,
    onDismiss: () -> Unit,
    onSave: (name: String, cover: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember(ledger) { mutableStateOf(ledger?.name.orEmpty()) }
    var coverKey by remember(ledger) {
        mutableStateOf(ledger?.coverKey ?: LedgerCoverSpec.fallback(ledger?.id.orEmpty()).encode())
    }
    var showCoverStudio by remember { mutableStateOf(false) }

    OmniSheetScaffold(
        title = if (ledger == null) "新建账本" else "编辑账本",
        onDismiss = onDismiss,
        onSave = { onSave(name.trim(), coverKey) },
        saveEnabled = name.isNotBlank(),
        saveText = "保存",
        cancelText = "取消",
        onCancel = onDismiss,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 实时拟物小封面
            Box(modifier = Modifier.width(110.dp)) {
                OmniTactileBook(
                    name = name.ifBlank { "新账本" },
                    coverKey = coverKey,
                    ledgerId = ledger?.id.orEmpty(),
                )
            }

            Spacer(Modifier.height(OmniSpacingTokens.medium))

            OutlinedButton(
                onClick = { showCoverStudio = true },
                shape = OmniShapeTokens.button,
            ) {
                Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("定制封面与纹样")
            }

            Spacer(Modifier.height(OmniSpacingTokens.large))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(20) },
                label = { Text("账本名称") },
                singleLine = true,
                shape = OmniShapeTokens.textField,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (showCoverStudio) {
        LedgerCoverStudioSheet(
            initialCoverKey = coverKey,
            ledgerName = name,
            onDismiss = { showCoverStudio = false },
            onSaveCover = { coverKey = it },
            ledgerId = ledger?.id.orEmpty(),
        )
    }
}

/**
 * 账本详情抽屉 (大封面 + 现有汇总与属性 + 设为默认/封面工坊/删除，严禁虚构默认币种)。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LedgerDetailSheet(
    ledger: Ledger,
    stat: LedgerOverview?,
    isDefault: Boolean,
    onDismiss: () -> Unit,
    onSetDefault: () -> Unit,
    onOpenCoverStudio: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OmniSheetScaffold(
        title = "账本详情",
        onDismiss = onDismiss,
        cancelText = "关闭",
        onCancel = onDismiss,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 大封面展示
            Box(modifier = Modifier.width(140.dp)) {
                OmniTactileBook(
                    name = ledger.name,
                    coverKey = ledger.coverKey,
                    ledgerId = ledger.id,
                    isDefault = isDefault,
                )
            }

            Spacer(Modifier.height(OmniSpacingTokens.medium))

            // 现有字段与汇总（绝无「默认币种」）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(OmniSpacingTokens.small),
            ) {
                StatTile(
                    label = "总支出",
                    value = stat?.totalExpense?.asRmb() ?: "¥0.00",
                    modifier = Modifier.weight(1f),
                    color = expenseColor(),
                )
                StatTile(
                    label = "总收入",
                    value = stat?.totalIncome?.asRmb() ?: "¥0.00",
                    modifier = Modifier.weight(1f),
                    color = incomeColor(),
                )
                StatTile(
                    label = "流水笔数",
                    value = "${stat?.transactionCount ?: 0} 笔",
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(OmniSpacingTokens.large))

            // 操作组
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(OmniSpacingTokens.small),
            ) {
                if (!isDefault) {
                    OutlinedButton(
                        onClick = onSetDefault,
                        modifier = Modifier.fillMaxWidth(),
                        shape = OmniShapeTokens.button,
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("设为默认账本")
                    }
                }

                OutlinedButton(
                    onClick = onOpenCoverStudio,
                    modifier = Modifier.fillMaxWidth(),
                    shape = OmniShapeTokens.button,
                ) {
                    Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("封面工坊 (纹样与自定义图)")
                }

                OutlinedButton(
                    onClick = onRename,
                    modifier = Modifier.fillMaxWidth(),
                    shape = OmniShapeTokens.button,
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("修改账本名称")
                }

                Button(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    shape = OmniShapeTokens.button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("删除账本")
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    ElevatedCard(
        shape = OmniShapeTokens.mediumSmall,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OmniSpacingTokens.small),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = color)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConfirmDeleteLedgerDialog(
    ledger: Ledger,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    OmniSheetScaffold(
        title = "删除账本",
        onDismiss = onDismiss,
        onSave = onConfirm,
        saveText = "确认删除",
        cancelText = "取消",
        onCancel = onDismiss,
    ) {
        Text(
            text = "确认删除账本“${ledger.name}”？\n\n该账本下的交易、分类、标签和规则都会一并删除，本地封面图片也将被清理，且操作无法撤销。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
