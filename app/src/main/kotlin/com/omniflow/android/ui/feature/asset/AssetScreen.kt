package com.omniflow.android.ui.feature.asset

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.MorePage
import com.omniflow.android.ui.MoreUiState
import com.omniflow.android.ui.OmniTextField
import com.omniflow.android.ui.asRmb
import com.omniflow.android.ui.designsystem.components.OmniBarSegment
import com.omniflow.android.ui.designsystem.components.OmniEmptyState
import com.omniflow.android.ui.designsystem.components.OmniSubPageScaffold
import com.omniflow.android.ui.expenseColor
import com.omniflow.android.ui.incomeColor
import com.omniflow.android.ui.label
import com.omniflow.core.SharedApp
import com.omniflow.core.domain.model.Account
import com.omniflow.core.domain.model.AccountRole
import com.omniflow.core.domain.model.AccountSummary
import com.omniflow.core.domain.model.AccountType
import com.omniflow.core.domain.model.AppTimeZone
import com.omniflow.core.domain.model.Money
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.toLocalDateTime

@Composable
internal fun AssetScreen(
    state: MoreUiState,
    sharedApp: SharedApp,
    onBack: () -> Unit,
    onPage: (MorePage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedRole by remember { mutableStateOf<AccountRole?>(null) }
    var selectedSegmentKey by remember { mutableStateOf<String?>(null) }
    var editingAccount by remember { mutableStateOf<Account?>(null) }
    var showEditNotesDialog by remember { mutableStateOf(false) }
    var monthlyNotes by remember { mutableStateOf("") }

    val today = remember { Clock.System.now().toLocalDateTime(AppTimeZone).date }
    val currentPeriod = remember(today) { "${today.year}-${today.monthNumber.toString().padStart(2, '0')}" }

    // Load current month notes from reconciliation meta
    LaunchedEffect(currentPeriod) {
        val meta = sharedApp.reconciliation.observePeriodSnapshot(currentPeriod, null).firstOrNull()?.getOrNull()?.meta
        if (meta != null && meta.notes.isNotBlank()) {
            monthlyNotes = meta.notes
        }
    }

    val activeAccounts = remember(state.accounts, selectedRole) {
        state.accounts.filter {
            it.deletedAt == null && it.includeInTotalAssets && (selectedRole == null || it.ownerRole == selectedRole)
        }
    }

    // Role-filtered summary calculation
    val summary = remember(activeAccounts) {
        var assetsMinor = 0L
        var liabMinor = 0L
        activeAccounts.forEach { a ->
            val debt = a.debt.minor
            if (debt > 0) {
                liabMinor += debt
            }
            if (a.balance.minor > 0 && a.type != AccountType.CREDIT_CARD) {
                assetsMinor += a.balance.minor
            }
        }
        AccountSummary(Money(assetsMinor), Money(liabMinor))
    }

    var totalCreditLimitMinor = 0L
    var totalDebtMinor = 0L
    activeAccounts.forEach { a ->
        if (a.type == AccountType.CREDIT_CARD) {
            totalCreditLimitMinor += a.creditLimit.minor
            totalDebtMinor += a.debt.minor
        }
    }
    val totalCreditLimit = Money(totalCreditLimitMinor)
    val totalDebt = Money(totalDebtMinor)

    // Aggregate assets by account type
    val assetAggregates: List<Pair<AccountType, Money>> = remember(activeAccounts) {
        AccountType.entries.mapNotNull { type ->
            val matching = activeAccounts.filter { it.type == type && it.balance.minor > 0 && it.type != AccountType.CREDIT_CARD }
            if (matching.isEmpty()) null
            else {
                val totalMinor = matching.fold(0L) { acc, a -> acc + a.balance.minor }
                type to Money(totalMinor)
            }
        }
    }

    val incColor = incomeColor()
    val expColor = expenseColor()

    val segmentItems: List<OmniBarSegment> = remember(assetAggregates, totalDebt, summary, incColor, expColor) {
        val totalVol = summary.assets.minor + totalDebt.minor
        if (totalVol <= 0L) emptyList()
        else {
            val list = mutableListOf<OmniBarSegment>()
            assetAggregates.forEach { (type, amount) ->
                list.add(
                    OmniBarSegment(
                        key = type.name,
                        weight = amount.minor.toFloat(),
                        color = incColor,
                        label = type.label,
                    ),
                )
            }
            if (totalDebt.minor > 0L) {
                list.add(
                    OmniBarSegment(
                        key = "LIABILITY",
                        weight = totalDebt.minor.toFloat(),
                        color = expColor,
                        label = "欠款负债",
                    ),
                )
            }
            list
        }
    }

    // Categorize accounts for clean mobile UX
    val creditCards = activeAccounts.filter { it.type == AccountType.CREDIT_CARD }
    val savingsAndWallets = activeAccounts.filter { it.type == AccountType.DEBIT_CARD || it.type == AccountType.E_WALLET || it.type == AccountType.CASH }
    val investments = activeAccounts.filter { it.type == AccountType.INVESTMENT }

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
                        selected = selectedRole == null,
                        onClick = { selectedRole = null },
                        label = { Text("家庭合并") },
                    )
                    AccountRole.entries.forEach { role ->
                        FilterChip(
                            selected = selectedRole == role,
                            onClick = { selectedRole = role },
                            label = { Text(role.label) },
                        )
                    }
                }
            }

            if (activeAccounts.isEmpty()) {
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
                        summary = summary,
                        totalCreditLimit = totalCreditLimit,
                        totalDebt = totalDebt,
                        selectedRole = selectedRole,
                        segmentItems = segmentItems,
                        selectedKey = selectedSegmentKey,
                        onSegmentClick = { segment ->
                            selectedSegmentKey = if (selectedSegmentKey == segment.key) null else segment.key
                        },
                    )
                }

                // Monthly Notes Card
                item(key = "monthly_notes") {
                    MonthlyNotesCard(
                        notes = monthlyNotes,
                        onEditNotes = { showEditNotesDialog = true },
                    )
                }

                // Section 1: Credit Cards & Lines
                if (creditCards.isNotEmpty()) {
                    item(key = "credit_header") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
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

                    items(creditCards, key = { it.id }) { card ->
                        AssetAccountRowCard(
                            account = card,
                            onQuickEdit = { editingAccount = it },
                        )
                    }
                }

                // Section 2: Savings & Cash
                if (savingsAndWallets.isNotEmpty()) {
                    item(key = "savings_header") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
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

                    items(savingsAndWallets, key = { it.id }) { acc ->
                        AssetAccountRowCard(
                            account = acc,
                            onQuickEdit = { editingAccount = it },
                        )
                    }
                }

                // Section 3: Investments
                if (investments.isNotEmpty()) {
                    item(key = "invest_header") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
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

                    items(investments, key = { it.id }) { inv ->
                        AssetAccountRowCard(
                            account = inv,
                            onQuickEdit = { editingAccount = it },
                        )
                    }
                }
            }
        }
    }

    // Quick Edit Bottom Sheet
    editingAccount?.let { account ->
        QuickEditAccountSheet(
            account = account,
            onDismiss = { editingAccount = null },
            onConfirm = { balance, availableLimit, profitLoss ->
                coroutineScope.launch {
                    sharedApp.reconciliation.recordAccountUpdate(
                        accountId = account.id,
                        period = currentPeriod,
                        balance = balance,
                        availableLimit = availableLimit,
                        profitLoss = profitLoss,
                    )
                }
                editingAccount = null
            },
        )
    }

    // Monthly Notes Dialog
    if (showEditNotesDialog) {
        var tempNotes by remember { mutableStateOf(monthlyNotes) }
        AlertDialog(
            onDismissRequest = { showEditNotesDialog = false },
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
                        monthlyNotes = tempNotes
                        coroutineScope.launch {
                            sharedApp.reconciliation.updateMonthlyMeta(
                                period = currentPeriod,
                                husbandIncome = Money.Zero,
                                wifeIncome = Money.Zero,
                                notes = tempNotes,
                            )
                        }
                        showEditNotesDialog = false
                    },
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNotesDialog = false }) {
                    Text("取消")
                }
            },
        )
    }
}
