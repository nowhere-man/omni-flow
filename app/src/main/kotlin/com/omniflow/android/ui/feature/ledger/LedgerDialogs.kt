package com.omniflow.android.ui.feature.ledger

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.omniflow.android.ui.LedgerCoverPicker
import com.omniflow.android.ui.LedgerCovers
import com.omniflow.android.ui.ManagementSheet
import com.omniflow.android.ui.OmniTextField
import com.omniflow.core.domain.model.Ledger

@Composable
internal fun LedgerSheet(
    ledger: Ledger?,
    onDismiss: () -> Unit,
    onSave: (name: String, cover: String?) -> Unit,
) {
    var name by remember(ledger) { mutableStateOf(ledger?.name.orEmpty()) }
    var cover by remember(ledger) { mutableStateOf(ledger?.coverKey ?: LedgerCovers.first().key) }

    ManagementSheet(
        title = if (ledger == null) "新建账本" else "编辑账本",
        onDismiss = onDismiss,
        onSave = { onSave(name.trim(), cover) },
        saveEnabled = name.isNotBlank(),
    ) {
        OmniTextField(name, { name = it }, label = "账本名称")
        Text("封面", style = MaterialTheme.typography.labelLarge)
        LedgerCoverPicker(cover) { cover = it }
    }
}

@Composable
internal fun ConfirmDeleteLedgerDialog(
    ledger: Ledger,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除账本“${ledger.name}”？") },
        text = { Text("该账本下的交易、分类、标签和规则都会一并删除，且无法撤销。") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("删除", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
