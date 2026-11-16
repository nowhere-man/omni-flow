package com.omniflow.android.ui.feature.account

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.OmniText
import com.omniflow.android.ui.asRmb
import com.omniflow.android.ui.designsystem.components.OmniEmptyState
import com.omniflow.android.ui.designsystem.components.OmniSubPageScaffold
import com.omniflow.android.ui.expenseColor
import com.omniflow.android.ui.incomeColor
import com.omniflow.core.domain.model.AccountId
import com.omniflow.core.domain.model.AccountRole
import com.omniflow.core.domain.model.AccountType
import com.omniflow.core.domain.model.CreditBufferSummary
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

    val activeAccounts = state.filteredAccounts.filter { it.deletedAt == null }
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
            // Top Hero Card: Family Credit Buffer Summary (Credits sheet representation)
            item(key = "credit_buffer_hero") {
                CreditBufferHeroCard(
                    creditBuffer = state.creditBuffer,
                    selectedRole = state.selectedRole,
                    hideAmount = state.hideAmount,
                )
            }

            // Role Filter Chips Row
            item(key = "role_filter_row") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = state.selectedRole == null,
                        onClick = { viewModel.selectRole(null) },
                        label = { Text("全部") },
                    )
                    AccountRole.entries.forEach { role ->
                        FilterChip(
                            selected = state.selectedRole == role,
                            onClick = { viewModel.selectRole(role) },
                            label = { Text(role.label) },
                        )
                    }
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

            if (activeAccounts.isEmpty()) {
                item {
                    OmniEmptyState(
                        status = "暂无相关账户",
                        value = "点击下方按钮添加新账户或切换角色筛选",
                        actionLabel = "新建账户",
                        onAction = { showNewAccountSheet = true },
                        modifier = Modifier.padding(top = 24.dp),
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
            onSave = { name, type, icon, card, note, balance, included, role, creditLimit, availableLimit ->
                viewModel.createAccount(
                    name = name,
                    type = type,
                    iconKey = icon,
                    cardNumber = card,
                    note = note,
                    balance = balance,
                    includeInTotalAssets = included,
                    role = role,
                    creditLimit = creditLimit,
                    availableLimit = availableLimit,
                )
                showNewAccountSheet = false
            },
        )
    }
}

@Composable
private fun CreditBufferHeroCard(
    creditBuffer: CreditBufferSummary,
    selectedRole: AccountRole?,
    hideAmount: Boolean,
) {
    val displayLimit = when (selectedRole) {
        AccountRole.HUSBAND -> creditBuffer.husband.limit
        AccountRole.WIFE -> creditBuffer.wife.limit
        AccountRole.FAMILY -> creditBuffer.family.limit
        null -> creditBuffer.totalLimit
    }

    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val secondary = MaterialTheme.colorScheme.secondary
    val roleColor = { role: AccountRole ->
        when (role) {
            AccountRole.HUSBAND -> primary
            AccountRole.WIFE -> tertiary
            AccountRole.FAMILY -> secondary
        }
    }

    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Text(
                        text = if (selectedRole == null) "家庭授信总储备" else "${selectedRole.label}授信储备",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Text(
                    text = if (hideAmount) "••••••" else displayLimit.asRmb(),
                    style = OmniText.amountHero,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            if (creditBuffer.totalLimit.minor > 0) {
                if (selectedRole == null) {
                    // Split bar - 3 segments
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                        ) {
                            creditBuffer.portions.forEach { portion ->
                                if (portion.ratio > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .weight(portion.ratio)
                                            .fillMaxHeight()
                                            .background(roleColor(portion.role)),
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            creditBuffer.portions.forEach { portion ->
                                Text(
                                    text = "${portion.role.label} ${if (hideAmount) "••" else portion.limit.asRmb()} (${portion.percentage}%)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = roleColor(portion.role),
                                )
                            }
                        }
                    }
                } else {
                    val portion = creditBuffer.portionFor(selectedRole)
                    val color = roleColor(selectedRole)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LinearProgressIndicator(
                            progress = { portion.ratio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = color,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = StrokeCap.Round,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "${selectedRole.label} ${if (hideAmount) "••" else portion.limit.asRmb()} (${portion.percentage}%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = color,
                            )
                        }
                    }
                }
            }
        }
    }
}
