package com.omniflow.android.ui.feature.reconciliation

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
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
import com.omniflow.android.ui.OmniTextField
import com.omniflow.android.ui.asRmb
import com.omniflow.android.ui.designsystem.components.OmniSubPageScaffold
import com.omniflow.android.ui.expenseColor
import com.omniflow.android.ui.feature.asset.MonthlyNotesCard
import com.omniflow.android.ui.incomeColor
import com.omniflow.android.ui.toDecimal
import com.omniflow.android.ui.toMoneyOrNull
import com.omniflow.core.domain.model.AccountRole
import com.omniflow.core.domain.model.Money

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReconciliationScreen(
    viewModel: ReconciliationViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    var showIncomeDialog by remember { mutableStateOf(false) }
    var showNotesDialog by remember { mutableStateOf(false) }

    OmniSubPageScaffold(
        title = "家庭对账中心",
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
            // View Mode Tab Row: [ 月度深度分析 | 年度财富台账 ]
            item(key = "view_mode_tabs") {
                PrimaryTabRow(
                    selectedTabIndex = state.viewMode.ordinal,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ReconciliationViewMode.entries.forEach { mode ->
                        Tab(
                            selected = state.viewMode == mode,
                            onClick = { viewModel.setViewMode(mode) },
                            text = {
                                Text(
                                    text = mode.label,
                                    fontWeight = if (state.viewMode == mode) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                        )
                    }
                }
            }

            // Year Pills Selector
            item(key = "year_selector") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.availableYears.forEach { yr ->
                        FilterChip(
                            selected = state.selectedYear == yr,
                            onClick = { viewModel.selectYear(yr) },
                            label = { Text("${yr}年") },
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    // Role filter pills
                    FilterChip(
                        selected = state.selectedRole == null,
                        onClick = { viewModel.selectRole(null) },
                        label = { Text("家庭合并") },
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

            // Content branch based on View Mode
            if (state.viewMode == ReconciliationViewMode.MONTHLY) {
                // Month selector pills (1月 ~ 12月)
                item(key = "month_pills") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        (1..12).forEach { m ->
                            val mStr = m.toString().padStart(2, '0')
                            val period = "${state.selectedYear}-$mStr"
                            val isSelected = state.selectedPeriod == period
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectPeriod(period) },
                                label = { Text("${m}月") },
                            )
                        }
                    }
                }

                state.periodSnapshot?.let { snapshot ->
                    // 1. Monthly Scorecard Hero
                    item(key = "monthly_hero") {
                        MonthlyScorecardHero(
                            snapshot = snapshot,
                            onEditIncome = { showIncomeDialog = true },
                        )
                    }

                    // 2. Notes Card for this month
                    item(key = "monthly_notes") {
                        MonthlyNotesCard(
                            notes = snapshot.meta.notes,
                            onEditNotes = { showNotesDialog = true },
                        )
                    }

                    // 3. Husband vs Wife Breakdown Card
                    item(key = "role_breakdown") {
                        RoleBreakdownCard(snapshot = snapshot)
                    }
                }
            } else {
                // Annual View Mode
                state.annualReport?.let { report ->
                    // 1. Wealth Ladder Chart (Smooth Bezier with Gradient)
                    item(key = "ladder_chart") {
                        NetWorthLadderChart(points = report.netWorthPoints)
                    }

                    // 2. Annual Summary Hero Card
                    item(key = "annual_totals") {
                        AnnualTotalsCard(report = report, selectedRole = state.selectedRole)
                    }

                    // 3. Expandable Monthly Ledger Cards (Replaces long tables on mobile)
                    item(key = "ledger_cards_header") {
                        Text(
                            text = "${state.selectedYear}年 各月财富对账台账",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }

                    items(report.monthlySnapshots, key = { it.period }) { snapshot ->
                        ExpandableMonthLedgerCard(
                            snapshot = snapshot,
                            onSelect = {
                                viewModel.selectPeriod(snapshot.period)
                                viewModel.setViewMode(ReconciliationViewMode.MONTHLY)
                            },
                        )
                    }
                }
            }
        }
    }

    // Income Entry Dialog
    if (showIncomeDialog) {
        val snap = state.periodSnapshot
        var husbandIncomeStr by remember { mutableStateOf(snap?.meta?.husbandIncome?.toDecimal().orEmpty()) }
        var wifeIncomeStr by remember { mutableStateOf(snap?.meta?.wifeIncome?.toDecimal().orEmpty()) }

        AlertDialog(
            onDismissRequest = { showIncomeDialog = false },
            title = { Text("录入 ${state.selectedPeriod} 角色收入") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "录入后系统将自动计算当月家庭总收入、储蓄率与倒推支出。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OmniTextField(
                        value = husbandIncomeStr,
                        onValueChange = { husbandIncomeStr = it },
                        label = "${AccountRole.HUSBAND.label} 本月收入（元）",
                        placeholder = "如 30000.00",
                    )
                    OmniTextField(
                        value = wifeIncomeStr,
                        onValueChange = { wifeIncomeStr = it },
                        label = "${AccountRole.WIFE.label} 本月收入（元）",
                        placeholder = "如 15000.00",
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val h = husbandIncomeStr.toMoneyOrNull() ?: Money.Zero
                        val w = wifeIncomeStr.toMoneyOrNull() ?: Money.Zero
                        viewModel.updateMonthlyIncome(h, w)
                        showIncomeDialog = false
                    },
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showIncomeDialog = false }) {
                    Text("取消")
                }
            },
        )
    }

    // Notes Dialog
    if (showNotesDialog) {
        val snap = state.periodSnapshot
        var notesStr by remember { mutableStateOf(snap?.meta?.notes.orEmpty()) }

        AlertDialog(
            onDismissRequest = { showNotesDialog = false },
            title = { Text("编辑 ${state.selectedPeriod} 财务备忘") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "记录当月大额转账、奖金、税收、大宗消费等核心事件。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OmniTextField(
                        value = notesStr,
                        onValueChange = { notesStr = it },
                        label = "备忘详情",
                        placeholder = "如：家庭内部转账；公积金到账；数码支出...",
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateMonthlyNotes(notesStr)
                        showNotesDialog = false
                    },
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotesDialog = false }) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
private fun AnnualTotalsCard(
    report: com.omniflow.core.domain.model.ReconciliationAnnualReport,
    selectedRole: AccountRole?,
) {
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${report.year}年 ${selectedRole?.label ?: "家庭合并"}年度收支汇总",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (report.averageSavingsRate > 0) {
                    Text(
                        text = "年均储蓄率 ${String.format("%.1f%%", report.averageSavingsRate)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = incomeColor(),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "年度总收入",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = report.totalAnnualIncome.asRmb(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = incomeColor(),
                    )
                }

                Column {
                    Text(
                        text = "年度总支出",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = report.totalAnnualExpense.asRmb(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = expenseColor(),
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "年末总净资产",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = report.yearEndNetAssets.asRmb(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
