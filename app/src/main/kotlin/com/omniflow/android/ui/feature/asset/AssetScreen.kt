package com.omniflow.android.ui.feature.asset

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.MorePage
import com.omniflow.android.ui.OmniTextField
import com.omniflow.android.ui.designsystem.components.OmniBarSegment
import com.omniflow.android.ui.designsystem.components.OmniEmptyState
import com.omniflow.android.ui.designsystem.components.OmniSettingsGroup
import com.omniflow.android.ui.designsystem.components.OmniSubPageScaffold
import com.omniflow.android.ui.expenseColor
import com.omniflow.android.ui.incomeColor
import com.omniflow.core.domain.model.AccountRole

@Composable
internal fun AssetScreen(
    viewModel: AssetViewModel,
    onBack: () -> Unit,
    onPage: (MorePage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val incColor = incomeColor()
    val expColor = expenseColor()

    val segmentItems = remember(state.segments, incColor, expColor) {
        state.segments.map { s ->
            OmniBarSegment(
                key = s.key,
                weight = s.weight,
                color = if (s.isLiability) expColor else incColor,
                label = s.label,
            )
        }
    }

    AssetScreenContent(
        state = state,
        segmentItems = segmentItems,
        onSelectRole = { viewModel.selectRole(it) },
        onSegmentClick = { viewModel.selectSegment(it.key) },
        onQuickEdit = { viewModel.setEditingAccount(it) },
        onOpenEditNotes = { viewModel.setShowEditNotesDialog(true) },
        onBack = onBack,
        onPage = onPage,
        modifier = modifier,
    )

    // Quick Edit Bottom Sheet
    state.editingAccount?.let { account ->
        QuickEditAccountSheet(
            account = account,
            onDismiss = { viewModel.setEditingAccount(null) },
            onConfirm = { balance, availableLimit, profitLoss ->
                viewModel.quickUpdateAccount(
                    accountId = account.id,
                    balance = balance,
                    availableLimit = availableLimit,
                    profitLoss = profitLoss,
                )
            },
        )
    }

    // Monthly Notes Dialog
    if (state.showEditNotesDialog) {
        var tempNotes by remember(state.monthlyNotes) { mutableStateOf(state.monthlyNotes) }
        AlertDialog(
            onDismissRequest = { viewModel.setShowEditNotesDialog(false) },
            title = { Text("记录本月对账重大收支与备忘") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "备忘将同步在对账中心按月展示（如：家庭内部转账、公积金收入、大额数码支出等）。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OmniTextField(
                        value = tempNotes,
                        onValueChange = { tempNotes = it },
                        label = "备忘详情",
                        placeholder = "输入重大财务事项...",
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateMonthlyNotes(tempNotes)
                    },
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowEditNotesDialog(false) }) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
internal fun AssetScreenContent(
    state: AssetUiState,
    segmentItems: List<OmniBarSegment>,
    onSelectRole: (AccountRole?) -> Unit,
    onSegmentClick: (OmniBarSegment) -> Unit,
    onQuickEdit: (com.omniflow.core.domain.model.Account) -> Unit,
    onOpenEditNotes: () -> Unit,
    onBack: () -> Unit,
    onPage: (MorePage) -> Unit,
    modifier: Modifier = Modifier,
) {
    OmniSubPageScaffold(
        title = "资产管理",
        onBack = onBack,
        modifier = modifier,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Role switcher row
            item(key = "role_switcher") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = state.selectedRole == null,
                        onClick = { onSelectRole(null) },
                        label = { Text("家庭合并") },
                    )
                    AccountRole.entries.forEach { role ->
                        FilterChip(
                            selected = state.selectedRole == role,
                            onClick = { onSelectRole(role) },
                            label = { Text(role.label) },
                        )
                    }
                }
            }

            if (state.activeAccounts.isEmpty()) {
                item {
                    OmniEmptyState(
                        status = "暂无资产数据",
                        value = "添加账户后，在此查看资产构成与负债分布",
                        actionLabel = "去管理账户",
                        onAction = { onPage(MorePage.ACCOUNTS) },
                        modifier = Modifier.padding(top = 40.dp),
                    )
                }
            } else {
                // Hero Card
                item(key = "asset_hero") {
                    AssetHeroCard(
                        summary = state.summary,
                        totalCreditLimit = state.totalCreditLimit,
                        totalDebt = state.totalDebt,
                        selectedRole = state.selectedRole,
                        segmentItems = segmentItems,
                        selectedKey = state.selectedSegmentKey,
                        onSegmentClick = onSegmentClick,
                    )
                }

                // Monthly Notes Card
                item(key = "monthly_notes") {
                    MonthlyNotesCard(
                        notes = state.monthlyNotes,
                        onEditNotes = onOpenEditNotes,
                    )
                }

                // Section 1: Credit Cards (Grouped via OmniSettingsGroup)
                if (state.creditCards.isNotEmpty()) {
                    item(key = "credit_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "信用卡与可用额度",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "点击快速对账",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    item(key = "credit_group") {
                        OmniSettingsGroup {
                            state.creditCards.forEach { card ->
                                item {
                                    AssetAccountRow(
                                        account = card,
                                        onQuickEdit = onQuickEdit,
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 2: Savings & Cash (Grouped via OmniSettingsGroup)
                if (state.savingsAndWallets.isNotEmpty()) {
                    item(key = "savings_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "活期存款与电子钱包",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "点击修改余额",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    item(key = "savings_group") {
                        OmniSettingsGroup {
                            state.savingsAndWallets.forEach { acc ->
                                item {
                                    AssetAccountRow(
                                        account = acc,
                                        onQuickEdit = onQuickEdit,
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 3: Investments (Grouped via OmniSettingsGroup)
                if (state.investments.isNotEmpty()) {
                    item(key = "invest_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "证券投资与理财",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "点击更新市值",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    item(key = "invest_group") {
                        OmniSettingsGroup {
                            state.investments.forEach { inv ->
                                item {
                                    AssetAccountRow(
                                        account = inv,
                                        onQuickEdit = onQuickEdit,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
