package com.omniflow.android.ui.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.asRmb
import com.omniflow.android.ui.designsystem.components.OmniSettingsGroup
import com.omniflow.android.ui.designsystem.components.OmniSettingsRow
import com.omniflow.android.ui.designsystem.tokens.CategoryIconOptions
import com.omniflow.android.ui.designsystem.tokens.OmniColorTokens
import com.omniflow.android.ui.designsystem.tokens.SemanticBadgeType
import com.omniflow.android.ui.designsystem.tokens.getCategoryIcon
import com.omniflow.android.ui.expenseColor
import com.omniflow.android.ui.label
import com.omniflow.core.domain.model.Account
import com.omniflow.core.domain.model.AccountGroup
import com.omniflow.core.domain.model.AccountId
import com.omniflow.core.domain.model.AccountSummary
import com.omniflow.core.domain.model.AccountType
import com.omniflow.core.domain.model.Money

@Composable
internal fun AccountListScreen(
    viewModel: AccountViewModel,
    onOpenAccount: (AccountId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.listState.collectAsState()
    var showNewAccountSheet by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            NetAssetsHeroCard(
                summary = state.summary,
                hideAmount = state.hideAmount,
                onToggleHideAmount = viewModel::toggleHideAmount,
                onNewAccount = { showNewAccountSheet = true },
            )
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

        if (state.groups.isEmpty()) {
            item {
                Text(
                    text = "暂无账户，点击上方新建账户添加",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(state.groups, key = { it.type.name }) { group ->
                AccountGroupCard(
                    group = group,
                    hideAmount = state.hideAmount,
                    onOpenAccount = onOpenAccount,
                )
            }
        }

        item { Spacer(Modifier.height(88.dp)) }
    }

    if (showNewAccountSheet) {
        AccountSheet(
            account = null,
            onDismiss = { showNewAccountSheet = false },
            onSave = { name, type, icon, card, note, balance, included ->
                viewModel.createAccount(name, type, icon, card, note, balance, included)
                showNewAccountSheet = false
            },
        )
    }
}

@Composable
private fun NetAssetsHeroCard(
    summary: AccountSummary,
    hideAmount: Boolean,
    onToggleHideAmount: () -> Unit,
    onNewAccount: () -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "净资产",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    IconButton(onClick = onToggleHideAmount) {
                        Icon(
                            imageVector = if (hideAmount) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (hideAmount) "显示金额" else "隐藏金额",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Button(onClick = onNewAccount) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("新建账户")
                }
            }

            Text(
                text = if (hideAmount) "••••••" else summary.netAssets.asRmb(),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Column {
                    Text(
                        text = "资产",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = if (hideAmount) "••••••" else summary.assets.asRmb(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Column {
                    Text(
                        text = "负债",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = if (hideAmount) "••••••" else summary.liabilities.asRmb(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (summary.liabilities.minor > 0) expenseColor() else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountGroupCard(
    group: AccountGroup,
    hideAmount: Boolean,
    onOpenAccount: (AccountId) -> Unit,
) {
    val subtotalText = if (hideAmount) "••••••" else group.subtotal.asRmb()
    val groupTitle = "${group.type.label} · ${group.accounts.size} (小计 $subtotalText)"

    OmniSettingsGroup(title = groupTitle) {
        group.accounts.forEachIndexed { index, account ->
            val isLiability = account.balance.minor < 0
            val details = listOfNotNull(
                account.cardNumber?.takeLast(4)?.takeIf(String::isNotBlank)?.let { "尾号 $it" },
                if (!account.includeInTotalAssets) "不计入总资产" else null,
                account.note?.takeIf(String::isNotBlank),
            ).joinToString(" · ")

            val badgeType = when (account.type) {
                AccountType.CASH -> SemanticBadgeType.FINANCIAL
                AccountType.DEBIT_CARD -> SemanticBadgeType.FINANCIAL
                AccountType.CREDIT_CARD -> SemanticBadgeType.PLANNING
                AccountType.E_WALLET -> SemanticBadgeType.DATA_SYNC
                AccountType.INVESTMENT -> SemanticBadgeType.AI
            }

            OmniSettingsRow(
                title = account.name,
                icon = getCategoryIcon(account.iconKey),
                badgeType = badgeType,
                subtitle = details.ifBlank { null },
                onClick = { onOpenAccount(account.id) },
                trailing = {
                    Text(
                        text = if (hideAmount) "••••••" else account.balance.asRmb(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isLiability) expenseColor() else MaterialTheme.colorScheme.onSurface,
                    )
                },
            )

            if (index < group.accounts.lastIndex) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}
