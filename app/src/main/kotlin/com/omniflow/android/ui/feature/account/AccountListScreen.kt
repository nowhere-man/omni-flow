package com.omniflow.android.ui.feature.account

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.asRmb
import com.omniflow.android.ui.OmniText
import com.omniflow.android.ui.designsystem.components.OmniEmptyState
import com.omniflow.android.ui.designsystem.components.OmniSubPageScaffold
import com.omniflow.android.ui.designsystem.tokens.OmniColorTokens
import com.omniflow.android.ui.designsystem.tokens.accountCardGradient
import com.omniflow.android.ui.designsystem.tokens.accountTypeBadgeColors
import com.omniflow.android.ui.designsystem.tokens.getCategoryIcon
import com.omniflow.android.ui.expenseColor
import com.omniflow.android.ui.label
import com.omniflow.core.domain.model.Account
import com.omniflow.core.domain.model.AccountId
import com.omniflow.core.domain.model.AccountType
import com.omniflow.core.domain.model.Money

@Composable
internal fun AccountListScreen(
    viewModel: AccountViewModel,
    onOpenAccount: (AccountId) -> Unit,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val state by viewModel.listState.collectAsState()
    var showNewAccountSheet by remember { mutableStateOf(false) }
    var excludedExpanded by remember { mutableStateOf(false) }

    val activeAccounts = state.accounts.filter { it.deletedAt == null }
    val includedAccounts = activeAccounts.filter { it.includeInTotalAssets }
    val excludedAccounts = activeAccounts.filterNot { it.includeInTotalAssets }

    val includedGroups = AccountType.entries.mapNotNull { type ->
        val matching = includedAccounts.filter { it.type == type }
        if (matching.isEmpty()) null else type to matching
    }

    OmniSubPageScaffold(
        title = "账户管理",
        onBack = onBack,
        actions = {
            IconButton(onClick = viewModel::toggleHideAmount) {
                Icon(
                    imageVector = if (state.hideAmount) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (state.hideAmount) "显示金额" else "隐藏金额",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        modifier = modifier,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            state.error?.let { error ->
                item {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            if (activeAccounts.isEmpty()) {
                item {
                    OmniEmptyState(
                        status = "还没有账户",
                        value = "添加您的第一个银行卡、钱包或现金账户",
                        actionLabel = "新建账户",
                        onAction = { showNewAccountSheet = true },
                        modifier = Modifier.padding(top = 40.dp),
                    )
                }
            } else {
                includedGroups.forEach { (type, accounts) ->
                    val subtotal = accounts.fold(Money.Zero) { acc, a -> acc + a.balance }
                    item(key = "header_${type.name}") {
                        ThinSubtotalStrip(type = type, subtotal = subtotal, hideAmount = state.hideAmount)
                    }
                    items(accounts, key = { it.id }) { account ->
                        OmniBankCard(
                            account = account,
                            hideAmount = state.hideAmount,
                            onClick = { onOpenAccount(account.id) },
                        )
                    }
                }

                if (excludedAccounts.isNotEmpty()) {
                    item(key = "excluded_zone") {
                        val excludedTotal = excludedAccounts.fold(Money.Zero) { acc, a -> acc + a.balance }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { excludedExpanded = !excludedExpanded }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "不计入总资产",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Icon(
                                        imageVector = if (excludedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                Text(
                                    text = if (state.hideAmount) "••••••" else excludedTotal.asRmb(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            AnimatedVisibility(visible = excludedExpanded) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    excludedAccounts.forEach { account ->
                                        OmniBankCard(
                                            account = account,
                                            hideAmount = state.hideAmount,
                                            onClick = { onOpenAccount(account.id) },
                                            modifier = Modifier.fillMaxWidth(),
                                            alpha = 0.85f,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item(key = "dashed_add") {
                    DashedNewAccountCard(onClick = { showNewAccountSheet = true })
                }
            }
        }
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
