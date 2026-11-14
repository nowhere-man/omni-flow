package com.omniflow.android.ui.feature.ledger

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.omniflow.android.ui.TransactionRow
import com.omniflow.android.ui.asRmb
import com.omniflow.android.ui.expenseColor
import com.omniflow.android.ui.incomeColor
import com.omniflow.android.ui.mutedContent
import com.omniflow.android.ui.readableContentWidth
import com.omniflow.android.ui.surfaceCard
import com.omniflow.core.domain.model.Ledger
import com.omniflow.core.domain.model.LedgerId
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.hourMinuteText

@Composable
internal fun LedgerDetailScreen(
    ledgerId: LedgerId,
    viewModel: LedgerViewModel,
    onBack: () -> Unit,
    onOpenTransaction: (String) -> Unit,
    onSeeAll: (LedgerId) -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(ledgerId) {
        viewModel.loadLedgerDetail(ledgerId)
    }

    val state by viewModel.detailState.collectAsState()
    var showEditSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val ledger = state.ledger

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            Text(
                text = ledger?.name ?: "账本详情",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        if (ledger == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = state.error ?: "未找到账本信息",
                    color = MaterialTheme.colorScheme.error,
                )
            }
            return
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .readableContentWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                LedgerOverviewCard(
                    ledger = ledger,
                    isDefault = state.isDefault,
                    onEditClick = { showEditSheet = true },
                    onSetDefaultClick = { viewModel.setDefaultLedger(ledger.id) },
                    onDeleteClick = { showDeleteDialog = true },
                )
            }

            item {
                LedgerCycleSummaryRow(
                    expense = state.overview.totalExpense,
                    income = state.overview.totalIncome,
                    netBalance = state.overview.netBalance,
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "该账本流水",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "共 ${state.totalCount} 笔",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (state.transactions.isEmpty()) {
                item {
                    Text(
                        text = "该账本暂无交易记录",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                val recent = state.transactions.take(15)
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = surfaceCard(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(horizontal = 12.dp)) {
                            recent.forEachIndexed { index, item ->
                                val tx = item.transaction
                                TransactionRow(
                                    iconKey = item.primaryCategoryId,
                                    title = item.primaryCategoryName,
                                    subtitle = tx.note,
                                    amount = tx.amount,
                                    type = tx.type,
                                    timeText = tx.occurredAt.hourMinuteText(),
                                    onClick = { onOpenTransaction(tx.id) },
                                )
                                if (index < recent.lastIndex) {
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }

                if (state.totalCount > recent.size) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            TextButton(onClick = { onSeeAll(ledgerId) }) {
                                Text("查看全部 ${state.totalCount} 笔流水")
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(88.dp)) }
        }
    }

    if (showEditSheet && ledger != null) {
        LedgerSheet(
            ledger = ledger,
            onDismiss = { showEditSheet = false },
            onSave = { name, cover ->
                viewModel.updateLedger(ledger.id, name, cover)
                showEditSheet = false
            },
        )
    }

    if (showDeleteDialog && ledger != null) {
        ConfirmDeleteLedgerDialog(
            ledger = ledger,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                viewModel.deleteLedger(ledger.id) {
                    showDeleteDialog = false
                    onBack()
                }
            },
        )
    }
}

@Composable
private fun LedgerOverviewCard(
    ledger: Ledger,
    isDefault: Boolean,
    onEditClick: () -> Unit,
    onSetDefaultClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LedgerCoverBox(ledger.coverKey, Modifier.size(56.dp), iconSize = 28)
                Spacer(Modifier.size(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = ledger.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(2.dp))
                    if (isDefault) {
                        Surface(
                            shape = RoundedCornerShape(OmniRadius.small),
                            color = MaterialTheme.colorScheme.primary,
                        ) {
                            Text(
                                text = "默认账本",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    } else {
                        Text(
                            text = "普通账本",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onEditClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("重命名/封面")
                }
                if (!isDefault) {
                    OutlinedButton(
                        onClick = onSetDefaultClick,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("设为默认")
                    }
                }
                OutlinedButton(
                    onClick = onDeleteClick,
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun LedgerCycleSummaryRow(
    expense: Money,
    income: Money,
    netBalance: Money,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Tile("本期支出", expense, expenseColor(), Modifier.weight(1f))
        Tile("本期收入", income, incomeColor(), Modifier.weight(1f))
        Tile(
            "本期结余",
            netBalance,
            if (netBalance.minor >= 0) incomeColor() else expenseColor(),
            Modifier.weight(1f),
        )
    }
}

@Composable
private fun Tile(label: String, amount: Money, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Surface(modifier = modifier, shape = MaterialTheme.shapes.medium, color = surfaceCard()) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = mutedContent())
            Text(
                amount.asRmb(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
