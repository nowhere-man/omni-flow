package com.omniflow.android.ui.feature.asset

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.OmniTextField
import com.omniflow.android.ui.asRmb
import com.omniflow.android.ui.expenseColor
import com.omniflow.android.ui.feature.account.RoleBadge
import com.omniflow.android.ui.toDecimal
import com.omniflow.android.ui.toMoneyOrNull
import com.omniflow.core.domain.model.Account
import com.omniflow.core.domain.model.AccountType
import com.omniflow.core.domain.model.Money

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QuickEditAccountSheet(
    account: Account,
    onDismiss: () -> Unit,
    onConfirm: (balance: Money, availableLimit: Money?, profitLoss: Money) -> Unit,
) {
    val isCredit = account.type == AccountType.CREDIT_CARD
    val isInvest = account.type == AccountType.INVESTMENT

    var availText by remember(account) {
        mutableStateOf(account.availableLimit?.toDecimal() ?: if (isCredit) account.creditLimit.toDecimal() else "")
    }
    var balanceText by remember(account) {
        mutableStateOf(account.balance.toDecimal())
    }
    var plText by remember(account) {
        mutableStateOf(account.profitLoss.toDecimal())
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "更新对账信息 · ${account.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "修改后自动保存并记录到对账中心审计台账",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                RoleBadge(role = account.ownerRole)
            }

            if (isCredit) {
                Text(
                    text = "授信总额度: ${account.creditLimit.asRmb()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )

                OmniTextField(
                    value = availText,
                    onValueChange = { availText = it },
                    label = "当前可用额度（元）",
                    placeholder = "如 10000.00",
                )

                val avail = availText.toMoneyOrNull()
                if (avail != null) {
                    val debtMinor = (account.creditLimit.minor - avail.minor).coerceAtLeast(0L)
                    Text(
                        text = "自动计算本期欠款: ${Money(debtMinor).asRmb()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = expenseColor(),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            } else if (isInvest) {
                OmniTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    label = "当前持仓总市值（元）",
                    placeholder = "0.00",
                )
                OmniTextField(
                    value = plText,
                    onValueChange = { plText = it },
                    label = "本月累计盈亏（元，可选）",
                    placeholder = "0.00",
                )
            } else {
                OmniTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    label = "存款最新余额（元）",
                    placeholder = "0.00",
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (isCredit) {
                            val avail = availText.toMoneyOrNull() ?: account.creditLimit
                            val debtMinor = (account.creditLimit.minor - avail.minor).coerceAtLeast(0L)
                            onConfirm(Money(-debtMinor), avail, Money.Zero)
                        } else if (isInvest) {
                            val bal = balanceText.toMoneyOrNull() ?: Money.Zero
                            val pl = plText.toMoneyOrNull() ?: Money.Zero
                            onConfirm(bal, null, pl)
                        } else {
                            val bal = balanceText.toMoneyOrNull() ?: Money.Zero
                            onConfirm(bal, null, Money.Zero)
                        }
                    },
                ) {
                    Text("确认更新")
                }
            }
        }
    }
}
