package com.omniflow.android.ui.feature.ledger

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.LedgerCoverBox
import com.omniflow.android.ui.OmniRadius
import com.omniflow.android.ui.asRmb
import com.omniflow.android.ui.designsystem.components.OmniSettingsDivider
import com.omniflow.android.ui.designsystem.components.OmniSettingsGroup
import com.omniflow.android.ui.expenseColor
import com.omniflow.android.ui.incomeColor
import com.omniflow.android.ui.mutedContent
import com.omniflow.core.domain.model.Ledger
import com.omniflow.core.domain.model.LedgerId

@Composable
internal fun LedgerListScreen(
    viewModel: LedgerViewModel,
    onOpenLedger: (LedgerId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.listState.collectAsState()
    var showNewSheet by remember { mutableStateOf(false) }
    var editingLedger by remember { mutableStateOf<Ledger?>(null) }
    var deletingLedger by remember { mutableStateOf<Ledger?>(null) }
    var menuFor by remember { mutableStateOf<LedgerId?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Button(
                onClick = { showNewSheet = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.size(4.dp))
                Text("新建账本")
            }
        }

        state.error?.let { error ->
            item {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        if (state.ledgers.isEmpty()) {
            item {
                Text(
                    text = "暂无账本",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            item {
                OmniSettingsGroup(title = "全部账本 · ${state.ledgers.size}") {
                    state.ledgers.forEachIndexed { index, ledger ->
                        val stat = state.ledgerStats[ledger.id]
                        val isDefault = state.defaultLedgerId == ledger.id

                        ListItem(
                            modifier = Modifier.clickable { onOpenLedger(ledger.id) },
                            leadingContent = {
                                LedgerCoverBox(ledger.coverKey, Modifier.size(48.dp), iconSize = 24)
                            },
                            headlineContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = ledger.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (isDefault) {
                                        Spacer(Modifier.size(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(OmniRadius.small),
                                            color = MaterialTheme.colorScheme.primary,
                                        ) {
                                            Text(
                                                text = "默认",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                            )
                                        }
                                    }
                                }
                            },
                            supportingContent = {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "${stat?.transactionCount ?: 0} 笔账目",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = mutedContent(),
                                    )
                                    if (stat != null && stat.transactionCount > 0) {
                                        Text(
                                            text = "支出 ${stat.totalExpense.asRmb()} · 收入 ${stat.totalIncome.asRmb()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = mutedContent(),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = "结余 ${stat.netBalance.asRmb()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = if (stat.netBalance.minor >= 0) incomeColor() else expenseColor(),
                                        )
                                    }
                                }
                            },
                            trailingContent = {
                                Box {
                                    TextButton(onClick = { menuFor = ledger.id }) {
                                        Text("⋯", style = MaterialTheme.typography.titleMedium)
                                    }
                                    DropdownMenu(
                                        expanded = menuFor == ledger.id,
                                        onDismissRequest = { menuFor = null },
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("编辑") },
                                            onClick = {
                                                menuFor = null
                                                editingLedger = ledger
                                            },
                                        )
                                        if (!isDefault) {
                                            DropdownMenuItem(
                                                text = { Text("设为默认账本") },
                                                onClick = {
                                                    menuFor = null
                                                    viewModel.setDefaultLedger(ledger.id)
                                                },
                                            )
                                        }
                                        DropdownMenuItem(
                                            text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                                            onClick = {
                                                menuFor = null
                                                deletingLedger = ledger
                                            },
                                        )
                                    }
                                }
                            },
                        )

                        if (index < state.ledgers.lastIndex) {
                            OmniSettingsDivider()
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(88.dp)) }
    }

    if (showNewSheet || editingLedger != null) {
        LedgerSheet(
            ledger = editingLedger,
            onDismiss = {
                showNewSheet = false
                editingLedger = null
            },
            onSave = { name, cover ->
                if (editingLedger != null) {
                    viewModel.updateLedger(editingLedger!!.id, name, cover)
                } else {
                    viewModel.createLedger(name, cover)
                }
                showNewSheet = false
                editingLedger = null
            },
        )
    }

    deletingLedger?.let { ledger ->
        ConfirmDeleteLedgerDialog(
            ledger = ledger,
            onDismiss = { deletingLedger = null },
            onConfirm = {
                deletingLedger = null
                viewModel.deleteLedger(ledger.id)
            },
        )
    }
}
