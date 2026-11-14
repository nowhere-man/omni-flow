package com.omniflow.android.ui.feature.account

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import com.omniflow.android.ui.designsystem.components.OmniExpressiveSwitch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.IconPickerGrid
import com.omniflow.android.ui.ManagementSheet
import com.omniflow.android.ui.OmniTextField
import com.omniflow.android.ui.asRmb
import com.omniflow.android.ui.designsystem.tokens.CategoryIconOptions
import com.omniflow.android.ui.label
import com.omniflow.android.ui.toDecimal
import com.omniflow.android.ui.toMoneyOrNull
import com.omniflow.core.domain.model.Account
import com.omniflow.core.domain.model.AccountId
import com.omniflow.core.domain.model.AccountType
import com.omniflow.core.domain.model.Money

@Composable
internal fun AccountSheet(
    account: Account?,
    onDismiss: () -> Unit,
    onDelete: ((AccountId) -> Unit)? = null,
    onSave: (name: String, type: AccountType, icon: String, card: String?, note: String?, balance: Money, included: Boolean) -> Unit,
) {
    var name by remember(account) { mutableStateOf(account?.name.orEmpty()) }
    var type by remember(account) { mutableStateOf(account?.type ?: AccountType.CASH) }
    var icon by remember(account) { mutableStateOf(account?.iconKey ?: "wallet-cards") }
    var card by remember(account) { mutableStateOf(account?.cardNumber.orEmpty()) }
    var note by remember(account) { mutableStateOf(account?.note.orEmpty()) }
    var balance by remember(account) { mutableStateOf(account?.balance?.toDecimal().orEmpty()) }
    var included by remember(account) { mutableStateOf(account?.includeInTotalAssets ?: true) }
    var balanceError by remember(account) { mutableStateOf<String?>(null) }
    var confirmDelete by remember(account) { mutableStateOf(false) }

    ManagementSheet(
        title = if (account == null) "新建账户" else "编辑账户",
        onDismiss = onDismiss,
        onSave = {
            val parsed = balance.toMoneyOrNull()
            if (parsed == null) {
                balanceError = "请输入有效余额，最多两位小数"
            } else {
                balanceError = null
                onSave(name.trim(), type, icon, card.ifBlank { null }, note.ifBlank { null }, parsed, included)
            }
        },
        saveEnabled = name.isNotBlank(),
    ) {
        OmniTextField(name, { name = it }, label = "账户名称")
        Text("类型", style = MaterialTheme.typography.labelLarge)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            AccountType.entries.forEach { entry ->
                FilterChip(selected = type == entry, onClick = { type = entry }, label = { Text(entry.label) })
            }
        }
        Text("图标", style = MaterialTheme.typography.labelLarge)
        IconPickerGrid(CategoryIconOptions, icon) { icon = it }
        OmniTextField(
            value = balance,
            onValueChange = { balance = it; balanceError = null },
            label = "当前余额",
            placeholder = "0.00",
            supportingText = balanceError,
        )
        OmniTextField(card, { card = it }, label = "卡号（可选）")
        OmniTextField(note, { note = it }, label = "备注（可选）")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("计入总资产")
                Text("关闭后该账户不参与净资产统计", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OmniExpressiveSwitch(checked = included, onCheckedChange = { included = it })
        }
        if (account != null && onDelete != null) {
            TextButton(onClick = { confirmDelete = true }) {
                Text("删除账户", color = MaterialTheme.colorScheme.error)
            }
        }
    }
    if (confirmDelete && account != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除账户“${account.name}”？") },
            text = { Text("此操作无法撤销。") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete(account.id) }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } },
        )
    }
}

@Composable
internal fun CalibrateBalanceDialog(
    currentBalance: Money,
    onDismiss: () -> Unit,
    onCalibrate: (Money) -> Unit,
) {
    var input by remember { mutableStateOf(currentBalance.toDecimal()) }
    var error by remember { mutableStateOf<String?>(null) }
    val newBalance = input.toMoneyOrNull()
    val delta = if (newBalance != null) newBalance - currentBalance else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("调整账户余额") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "当前系统记录余额：${currentBalance.asRmb()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OmniTextField(
                    value = input,
                    onValueChange = { input = it; error = null },
                    label = "实际余额",
                    placeholder = "0.00",
                    supportingText = error,
                )
                if (delta != null && delta.minor != 0L) {
                    val prefix = if (delta.minor > 0) "+" else ""
                    Text(
                        "将产生校准差额：$prefix${delta.asRmb()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsed = input.toMoneyOrNull()
                    if (parsed == null) {
                        error = "请输入有效金额"
                    } else {
                        onCalibrate(parsed)
                    }
                },
                enabled = input.isNotBlank(),
            ) {
                Text("确认调整")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
