package com.omniflow.android.ui

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.hourMinuteText

/**
 * 账本 / 账户 / 分类的详情页：概览 + 最近明细 + 查看全部。
 * 三者的差别只在头部信息，所以共用同一个骨架。
 */
@Composable
internal fun EntityDetailScreen(
    state: EntityDetailUiState,
    moreState: MoreUiState,
    onLoad: () -> Unit,
    onOpenTransaction: (String) -> Unit,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(state.kind, state.entityId) { onLoad() }
    val recent = state.items.take(10)
    LazyColumn(
        modifier = modifier.fillMaxSize().readableContentWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { EntityDetailHeader(state, moreState) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryTile("支出", state.summary.expenseTotal, MaterialTheme.colorScheme.error, Modifier.weight(1f))
                SummaryTile("收入", state.summary.incomeTotal, MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                SummaryTile(
                    "结余",
                    state.summary.netIncome,
                    if (state.summary.netIncome.minor >= 0) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    Modifier.weight(1f),
                )
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "最近明细",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "共 ${state.count} 笔",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        when {
            state.isLoading -> item {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            state.error != null -> item {
                Text(state.error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 24.dp))
            }

            recent.isEmpty() -> item {
                Text(
                    "还没有相关交易",
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> {
                item {
                    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceVariant) {
                        Column(Modifier.padding(horizontal = 12.dp)) {
                            recent.forEachIndexed { index, item ->
                                TransactionRow(
                                    iconKey = item.categoryIconKey,
                                    title = item.categoryDisplayName,
                                    subtitle = item.note,
                                    amount = item.amount,
                                    type = item.type,
                                    timeText = item.occurredAt.hourMinuteText(),
                                    onClick = { onOpenTransaction(item.id) },
                                )
                                if (index != recent.lastIndex) HorizontalDivider()
                            }
                        }
                    }
                }
                if (state.count > recent.size) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            TextButton(onClick = onSeeAll) { Text("查看全部 ${state.count} 笔") }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(88.dp)) }
    }
}

@Composable
private fun EntityDetailHeader(state: EntityDetailUiState, moreState: MoreUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            when (state.kind) {
                EntityDetailKind.LEDGER -> {
                    val ledger = moreState.ledgers.firstOrNull { it.id == state.entityId }
                    LedgerCoverBox(ledger?.coverKey, Modifier.size(56.dp), iconSize = 28)
                    Spacer(Modifier.size(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            ledger?.name ?: "账本",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            if (moreState.defaultLedgerId == state.entityId) "默认账本" else "账本",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                EntityDetailKind.ACCOUNT -> {
                    val account = moreState.accounts.firstOrNull { it.id == state.entityId }
                    RoundIcon(account?.iconKey, size = 56)
                    Spacer(Modifier.size(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            account?.name ?: "账户",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            listOfNotNull(
                                account?.type?.detailLabel,
                                account?.cardNumber?.takeLast(4)?.takeIf(String::isNotBlank)?.let { "尾号 $it" },
                                if (account?.includeInTotalAssets == false) "不计入总资产" else null,
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    account?.let {
                        Text(
                            it.balance.asRmb(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (it.balance.minor < 0) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }

                EntityDetailKind.CATEGORY -> {
                    val category = moreState.categories.firstOrNull { it.id == state.entityId }
                    RoundIcon(category?.iconKey, size = 56)
                    Spacer(Modifier.size(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            category?.name ?: "分类",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val children = moreState.categories.count { it.parentId == state.entityId }
                        Text(
                            if (children > 0) "$children 个二级分类" else "无二级分类",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryTile(label: String, amount: Money, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Surface(modifier = modifier, shape = MaterialTheme.shapes.medium, color = color.copy(alpha = 0.12f)) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                amount.asRmb(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private val com.omniflow.core.domain.model.AccountType.detailLabel: String
    get() = when (this) {
        com.omniflow.core.domain.model.AccountType.CASH -> "现金"
        com.omniflow.core.domain.model.AccountType.DEBIT_CARD -> "储蓄卡"
        com.omniflow.core.domain.model.AccountType.CREDIT_CARD -> "信用卡"
        com.omniflow.core.domain.model.AccountType.E_WALLET -> "电子钱包"
        com.omniflow.core.domain.model.AccountType.INVESTMENT -> "投资账户"
    }
