package com.omniflow.android.ui.feature.batch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.OmniText
import com.omniflow.android.ui.designsystem.components.OmniSheetScaffold
import com.omniflow.android.ui.designsystem.tokens.OmniShapeTokens
import com.omniflow.android.ui.surfaceInset
import com.omniflow.core.domain.model.Account
import com.omniflow.core.domain.model.Ledger

@Composable
fun BatchDeleteConfirmDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("批量删除账目") },
        text = {
            Text(
                "确定要删除选中的 $count 笔账目吗？\n删除后所涉及的账户余额将自动回退，转账账目将同时回退转出与转入账户余额。",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) {
                Text("删除 ($count)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchAccountPickerDialog(
    accounts: List<Account>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    OmniSheetScaffold(
        title = "批量修改账户",
        onDismiss = onDismiss,
        cancelText = "取消",
        onCancel = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "请选择要迁入的目标账户（转账账目将自动跳过）：",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            accounts.forEach { account ->
                Surface(
                    onClick = { onSelect(account.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = OmniShapeTokens.small,
                    color = surfaceInset(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = account.name,
                            style = OmniText.bodyRow,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchLedgerPickerDialog(
    ledgers: List<Ledger>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    OmniSheetScaffold(
        title = "批量修改账本",
        onDismiss = onDismiss,
        cancelText = "取消",
        onCancel = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "请选择要移入的目标账本：",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ledgers.forEach { ledger ->
                Surface(
                    onClick = { onSelect(ledger.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = OmniShapeTokens.small,
                    color = surfaceInset(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = ledger.name,
                            style = OmniText.bodyRow,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}
