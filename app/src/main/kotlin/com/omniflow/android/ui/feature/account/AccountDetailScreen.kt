package com.omniflow.android.ui.feature.account

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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
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
import com.omniflow.android.ui.TransactionRow
import com.omniflow.android.ui.asRmb
import com.omniflow.android.ui.designsystem.components.OmniBadgeIcon
import com.omniflow.android.ui.designsystem.tokens.SemanticBadgeType
import com.omniflow.android.ui.designsystem.tokens.getCategoryIcon
import com.omniflow.android.ui.expenseColor
import com.omniflow.android.ui.incomeColor
import com.omniflow.android.ui.label
import com.omniflow.android.ui.mutedContent
import com.omniflow.android.ui.readableContentWidth
import com.omniflow.android.ui.surfaceCard
import com.omniflow.core.domain.model.Account
import com.omniflow.core.domain.model.AccountId
import com.omniflow.core.domain.model.AccountType
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.hourMinuteText

@Composable
internal fun AccountDetailScreen(
    accountId: AccountId,
    viewModel: AccountViewModel,
    onBack: () -> Unit,
    onOpenTransaction: (String) -> Unit,
    onSeeAll: (AccountId) -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(accountId) {
        viewModel.loadAccountDetail(accountId)
    }

    val state by viewModel.detailState.collectAsState()
    var showCalibrateDialog by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }

    val account = state.account

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
                text = account?.name ?: "账户详情",
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

        if (account == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = state.error ?: "未找到账户信息",
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
                AccountOverviewCard(
                    account = account,
                    onCalibrateClick = { showCalibrateDialog = true },
                    onEditClick = { showEditSheet = true },
                )
            }

            item {
                AccountTransactionSummaryRow(
                    expenseTotal = state.summary.expenseTotal,
                    incomeTotal = state.summary.incomeTotal,
                    netIncome = state.summary.netIncome,
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "该账户流水",
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
                        text = "该账户暂无交易记录",
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
                            TextButton(onClick = { onSeeAll(accountId) }) {
                                Text("查看全部 ${state.totalCount} 笔流水")
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(88.dp)) }
        }
    }

    if (showCalibrateDialog && account != null) {
        CalibrateBalanceDialog(
            currentBalance = account.balance,
            onDismiss = { showCalibrateDialog = false },
            onCalibrate = { newBalance ->
                viewModel.calibrateBalance(accountId, newBalance)
                showCalibrateDialog = false
            },
        )
    }

    if (showEditSheet && account != null) {
        AccountSheet(
            account = account,
            onDismiss = { showEditSheet = false },
            onDelete = { id ->
                viewModel.deleteAccount(id) {
                    showEditSheet = false
                    onBack()
                }
            },
            onSave = { name, type, icon, card, note, balance, included ->
                viewModel.updateAccount(account.id, name, type, icon, card, note, balance, included)
                showEditSheet = false
            },
        )
    }
}

@Composable
private fun AccountOverviewCard(
    account: Account,
    onCalibrateClick: () -> Unit,
    onEditClick: () -> Unit,
) {
    val isLiability = account.balance.minor < 0
    val badgeType = when (account.type) {
        AccountType.CASH -> SemanticBadgeType.FINANCIAL
        AccountType.DEBIT_CARD -> SemanticBadgeType.FINANCIAL
        AccountType.CREDIT_CARD -> SemanticBadgeType.PLANNING
        AccountType.E_WALLET -> SemanticBadgeType.DATA_SYNC
        AccountType.INVESTMENT -> SemanticBadgeType.AI
    }

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
                OmniBadgeIcon(
                    icon = getCategoryIcon(account.iconKey),
                    badgeType = badgeType,
                )
                Spacer(Modifier.size(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    val info = listOfNotNull(
                        account.type.label,
                        account.cardNumber?.takeLast(4)?.let { "尾号 $it" },
                        if (!account.includeInTotalAssets) "不计入总资产" else null,
                    ).joinToString(" · ")
                    Text(
                        text = info,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column {
                Text(
                    text = "当前余额",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = account.balance.asRmb(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isLiability) expenseColor() else MaterialTheme.colorScheme.onSurface,
                )
                account.note?.takeIf(String::isNotBlank)?.let { note ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "备注：$note",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onCalibrateClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null)
                    Spacer(Modifier.size(4.dp))
                    Text("调整余额")
                }
                OutlinedButton(
                    onClick = onEditClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.size(4.dp))
                    Text("编辑账户")
                }
            }
        }
    }
}

@Composable
private fun AccountTransactionSummaryRow(
    expenseTotal: Money,
    incomeTotal: Money,
    netIncome: Money,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SummaryTile("支出", expenseTotal, expenseColor(), Modifier.weight(1f))
        SummaryTile("收入", incomeTotal, incomeColor(), Modifier.weight(1f))
        SummaryTile(
            "结余",
            netIncome,
            if (netIncome.minor >= 0) incomeColor() else expenseColor(),
            Modifier.weight(1f),
        )
    }
}

@Composable
private fun SummaryTile(label: String, amount: Money, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
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
