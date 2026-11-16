package com.omniflow.android.ui.feature.account

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.omniflow.android.ui.OmniTextField
import com.omniflow.android.ui.asRmb
import com.omniflow.android.ui.designsystem.components.OmniSheetScaffold
import com.omniflow.android.ui.designsystem.tokens.CategoryIconOptions
import com.omniflow.android.ui.expenseColor
import com.omniflow.android.ui.label
import com.omniflow.android.ui.toDecimal
import com.omniflow.android.ui.toMoneyOrNull
import com.omniflow.core.domain.model.Account
import com.omniflow.core.domain.model.AccountId
import com.omniflow.core.domain.model.AccountRole
import com.omniflow.core.domain.model.AccountType
import com.omniflow.core.domain.model.Money

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun AccountSheet(
    account: Account?,
    onDismiss: () -> Unit,
    onDelete: ((AccountId) -> Unit)? = null,
    onSave: (
        name: String,
        type: AccountType,
        icon: String,
        card: String?,
        note: String?,
        balance: Money,
        included: Boolean,
        role: AccountRole,
        creditLimit: Money,
        availableLimit: Money?,
    ) -> Unit,
) {
    var name by remember(account) { mutableStateOf(account?.name.orEmpty()) }
    var type by remember(account) { mutableStateOf(account?.type ?: AccountType.CASH) }
    var role by remember(account) { mutableStateOf(account?.ownerRole ?: AccountRole.HUSBAND) }
    var icon by remember(account) { mutableStateOf(account?.iconKey ?: "wallet-cards") }
    var card by remember(account) { mutableStateOf(account?.cardNumber.orEmpty()) }
    var note by remember(account) { mutableStateOf(account?.note.orEmpty()) }
    var balance by remember(account) { mutableStateOf(account?.balance?.toDecimal().orEmpty()) }
    var creditLimitStr by remember(account) {
        mutableStateOf(if ((account?.creditLimit?.minor ?: 0L) > 0) account?.creditLimit?.toDecimal().orEmpty() else "")
    }
    var availableLimitStr by remember(account) {
        mutableStateOf(account?.availableLimit?.toDecimal().orEmpty())
    }
    var included by remember(account) { mutableStateOf(account?.includeInTotalAssets ?: true) }
    var balanceError by remember(account) { mutableStateOf<String?>(null) }
    var confirmDelete by remember(account) { mutableStateOf(false) }

    OmniSheetScaffold(
        title = if (account == null) "新建账户" else "编辑账户",
        onDismiss = onDismiss,
        onSave = {
            val parsedBalance = if (type == AccountType.CREDIT_CARD) {
                // For credit cards, if available limit and credit limit are entered, debt is limit - avail
                val lim = creditLimitStr.toMoneyOrNull() ?: Money.Zero
                val avail = availableLimitStr.toMoneyOrNull()
                if (avail != null && lim.minor > avail.minor) {
                    Money(-(lim.minor - avail.minor))
                } else {
                    balance.toMoneyOrNull() ?: Money.Zero
                }
            } else {
                balance.toMoneyOrNull()
            }

            if (parsedBalance == null) {
                balanceError = "请输入有效余额，最多两位小数"
            } else {
                balanceError = null
                val lim = creditLimitStr.toMoneyOrNull() ?: Money.Zero
                val avail = availableLimitStr.toMoneyOrNull()
                onSave(
                    name.trim(),
                    type,
                    icon,
                    card.ifBlank { null },
                    note.ifBlank { null },
                    parsedBalance,
                    included,
                    role,
                    lim,
                    avail,
                )
            }
        },
        saveEnabled = name.isNotBlank(),
    ) {
        OmniTextField(name, { name = it }, label = "账户名称")

        Text("所属角色", style = MaterialTheme.typography.labelLarge)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AccountRole.entries.forEach { entry ->
                FilterChip(
                    selected = role == entry,
                    onClick = { role = entry },
                    label = { Text(entry.label) },
                )
            }
        }

        Text("类型", style = MaterialTheme.typography.labelLarge)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            AccountType.entries.forEach { entry ->
                FilterChip(selected = type == entry, onClick = { type = entry }, label = { Text(entry.label) })
            }
        }

        if (type == AccountType.CREDIT_CARD) {
            OmniTextField(
                value = creditLimitStr,
                onValueChange = { creditLimitStr = it },
                label = "信用卡总授信额度",
                placeholder = "如 100000.00",
            )
            OmniTextField(
                value = availableLimitStr,
                onValueChange = { availableLimitStr = it },
                label = "当前可用额度",
                placeholder = "如 10000.00",
            )
            // Live debt preview
            val lim = creditLimitStr.toMoneyOrNull() ?: Money.Zero
            val avail = availableLimitStr.toMoneyOrNull()
            if (avail != null && lim.minor >= avail.minor) {
                val computedDebt = Money(lim.minor - avail.minor)
                Text(
                    text = "当前欠款 = 总额度 - 可用额度: ${computedDebt.asRmb()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = expenseColor(),
                )
            }
        } else {
            OmniTextField(
                value = balance,
                onValueChange = { balance = it; balanceError = null },
                label = if (type == AccountType.INVESTMENT) "证券持仓市值" else "当前余额",
                placeholder = "0.00",
                supportingText = balanceError,
            )
        }

        Text("图标", style = MaterialTheme.typography.labelLarge)
        IconPickerGrid(CategoryIconOptions, icon) { icon = it }

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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun CalibrateBalanceDialog(
    account: Account,
    onDismiss: () -> Unit,
    onConfirm: (Money) -> Unit,
) {
    var text by remember(account) { mutableStateOf(account.balance.toDecimal()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("校准“${account.name}”余额") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "系统将生成一笔校准差异记录，使账户余额与实际金额一致。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OmniTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        error = null
                    },
                    label = "实际余额",
                    placeholder = "0.00",
                    supportingText = error,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsed = text.toMoneyOrNull()
                    if (parsed == null) {
                        error = "请输入有效金额"
                    } else {
                        onConfirm(parsed)
                    }
                },
            ) {
                Text("确认")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
