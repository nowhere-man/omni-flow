package com.omniflow.android.ui.feature.editor.components

import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.OmniFlowViewModel
import com.omniflow.android.ui.TransactionEditorUiState
import com.omniflow.android.ui.designsystem.tokens.getCategoryIcon
import com.omniflow.core.domain.model.Account
import com.omniflow.core.domain.model.AppTimeZone
import com.omniflow.core.domain.model.TransactionType
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

@Composable
fun EditorAttributeRow(
    state: TransactionEditorUiState,
    onAccount: (Account) -> Unit,
    onTargetAccount: (Account) -> Unit,
    onTag: (String) -> Unit,
    moreExpanded: Boolean,
    onMoreExpanded: (Boolean) -> Unit,
    onDate: (Instant) -> Unit,
    onExcluded: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dateTime = state.occurredAt.toLocalDateTime(AppTimeZone)
    val selectedAccount = state.accounts.firstOrNull { it.id == state.accountId }
    val selectedTarget = state.accounts.firstOrNull { it.id == state.targetAccountId }
    var accountExpanded by remember { mutableStateOf(false) }
    var targetAccountExpanded by remember { mutableStateOf(false) }
    var tagExpanded by remember { mutableStateOf(false) }

    val isTransfer = state.type == TransactionType.TRANSFER

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (isTransfer) {
            // 转账模式：显示「转出账户 → 转入账户」联动
            Box(Modifier.weight(1f)) {
                EditorChip(
                    label = selectedAccount?.let { "${it.name} (${it.balance.asFormattedRmb()})" } ?: "转出账户",
                    onClick = { accountExpanded = true },
                    leading = {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(15.dp))
                    },
                )
                AccountDropdownMenu(
                    expanded = accountExpanded,
                    onDismiss = { accountExpanded = false },
                    accounts = state.accounts,
                    onSelect = { onAccount(it) },
                )
            }

            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "转至",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Box(Modifier.weight(1f)) {
                EditorChip(
                    label = selectedTarget?.let { "${it.name} (${it.balance.asFormattedRmb()})" } ?: "转入账户",
                    onClick = { targetAccountExpanded = true },
                    leading = {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(15.dp))
                    },
                )
                AccountDropdownMenu(
                    expanded = targetAccountExpanded,
                    onDismiss = { targetAccountExpanded = false },
                    accounts = state.accounts.filter { it.id != state.accountId },
                    onSelect = { onTargetAccount(it) },
                )
            }
        } else {
            // 普通收支模式：显示账户（带当前余额）
            Box {
                EditorChip(
                    label = selectedAccount?.let { "${it.name} (${it.balance.asFormattedRmb()})" } ?: "选择账户",
                    onClick = { accountExpanded = true },
                    leading = {
                        if (selectedAccount == null) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(15.dp))
                        } else {
                            Icon(getCategoryIcon(selectedAccount.iconKey), contentDescription = null, modifier = Modifier.size(15.dp))
                        }
                    },
                )
                AccountDropdownMenu(
                    expanded = accountExpanded,
                    onDismiss = { accountExpanded = false },
                    accounts = state.accounts,
                    onSelect = { onAccount(it) },
                )
            }
            Spacer(Modifier.weight(1f))
            if (state.tags.isNotEmpty()) {
                Box {
                    EditorChip(
                        label = if (state.selectedTagIds.isEmpty()) "标签" else "标签 ${state.selectedTagIds.size}",
                        onClick = { tagExpanded = true },
                        selected = state.selectedTagIds.isNotEmpty(),
                    )
                    DropdownMenu(expanded = tagExpanded, onDismissRequest = { tagExpanded = false }) {
                        state.tags.forEach { tag ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        tag.name,
                                        fontWeight = if (tag.id in state.selectedTagIds) FontWeight.Bold else FontWeight.Normal,
                                    )
                                },
                                onClick = { onTag(tag.id) },
                            )
                        }
                    }
                }
            }
        }

        Box {
            EditorChip(label = "⋯", onClick = { onMoreExpanded(true) })
            DropdownMenu(expanded = moreExpanded, onDismissRequest = { onMoreExpanded(false) }) {
                DropdownMenuItem(
                    text = { Text("时间 ${dateTime.hour.twoDigits()}:${dateTime.minute.twoDigits()}") },
                    onClick = {
                        onMoreExpanded(false)
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                onDate(LocalDateTime(dateTime.date, LocalTime(hour, minute)).toInstant(AppTimeZone))
                            },
                            dateTime.hour,
                            dateTime.minute,
                            true,
                        ).show()
                    },
                )
                if (!isTransfer) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("不计入统计", modifier = Modifier.weight(1f))
                                Spacer(Modifier.width(8.dp))
                                Switch(checked = state.isExcluded, onCheckedChange = null)
                            }
                        },
                        onClick = { onExcluded(!state.isExcluded) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    accounts: List<Account>,
    onSelect: (Account) -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        accounts.forEach { account ->
            DropdownMenuItem(
                text = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(account.name)
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = account.balance.asFormattedRmb(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                onClick = {
                    onDismiss()
                    onSelect(account)
                },
            )
        }
    }
}

@Composable
fun EditorChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(34.dp),
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            leading?.invoke()
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun Int.twoDigits(): String = toString().padStart(2, '0')
