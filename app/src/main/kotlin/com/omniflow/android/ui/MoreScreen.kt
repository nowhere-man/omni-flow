package com.omniflow.android.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omniflow.android.ui.feature.account.AccountListScreen
import com.omniflow.android.ui.feature.account.AccountViewModel
import com.omniflow.android.ui.feature.account.AccountViewModelFactory
import com.omniflow.android.ui.feature.ledger.LedgerListScreen
import com.omniflow.android.ui.feature.ledger.LedgerViewModel
import com.omniflow.android.ui.feature.ledger.LedgerViewModelFactory
import com.omniflow.android.ui.feature.more.DataManagementPage
import com.omniflow.android.ui.feature.more.ExportPage
import com.omniflow.android.ui.feature.more.ImportPage
import com.omniflow.android.ui.feature.more.MoreHomeScreen
import com.omniflow.android.ui.feature.more.SettingsPage

enum class MorePage(val label: String, val icon: ImageVector) {
    HOME("更多", Icons.Default.Settings),
    SETTINGS("设置", Icons.Default.Settings),
    DATA("数据与同步", Icons.Default.CloudSync),
    AI("AI 助手", Icons.Default.AutoAwesome),
    IMPORT("导入账单", Icons.Default.FileUpload),
    EXPORT("导出数据", Icons.Default.FileDownload),
    RULES("规则", Icons.AutoMirrored.Filled.Rule),
    REMINDERS("提醒", Icons.Default.Notifications),
    BUDGETS("预算", Icons.Default.Savings),
    LEDGERS("账本", Icons.Default.AccountBalance),
    ACCOUNTS("账户", Icons.Default.Wallet),
    ASSETS("资产", Icons.Default.Savings),
    CATEGORIES("分类", Icons.Default.Category),
    TAGS("标签", Icons.AutoMirrored.Filled.Label),
}

@Composable
internal fun MoreScreen(
    state: MoreUiState,
    viewModel: OmniFlowViewModel,
    page: MorePage,
    onPage: (MorePage) -> Unit,
    onBack: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenLedger: (String) -> Unit,
    onOpenAccount: (String) -> Unit,
    onOpenCategory: (String) -> Unit,
    dynamicColorEnabled: Boolean,
    onDynamicColorChanged: (Boolean) -> Unit,
    accountViewModel: AccountViewModel = viewModel(factory = AccountViewModelFactory(viewModel.sharedApp)),
    ledgerViewModel: LedgerViewModel = viewModel(factory = LedgerViewModelFactory(viewModel.sharedApp)),
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        if (page != MorePage.HOME) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = mutedContent())
                }
                Text(page.label, style = OmniText.titleRow)
            }
        }
        when (page) {
            MorePage.HOME -> MoreHomeScreen(state, onPage = onPage)
            MorePage.SETTINGS -> SettingsPage(
                state,
                viewModel,
                dynamicColorEnabled = dynamicColorEnabled,
                onDynamicColorChanged = onDynamicColorChanged,
            )
            MorePage.DATA -> DataManagementPage(state, viewModel)
            MorePage.AI -> AiSettingsPage()
            MorePage.BUDGETS -> BudgetPage(state, viewModel)
            MorePage.IMPORT -> ImportPage(state, viewModel)
            MorePage.EXPORT -> ExportPage(state, viewModel)
            MorePage.LEDGERS -> LedgerListScreen(ledgerViewModel, onOpenLedger = onOpenLedger)
            MorePage.ACCOUNTS -> AccountListScreen(accountViewModel, onOpenAccount = onOpenAccount)
            MorePage.RULES,
            MorePage.REMINDERS,
            MorePage.ASSETS,
            MorePage.CATEGORIES,
            MorePage.TAGS -> ManagementPage(
                page,
                state,
                viewModel,
                onPage = onPage,
                onRequestNotificationPermission = onRequestNotificationPermission,
                onOpenAccount = onOpenAccount,
                onOpenCategory = onOpenCategory,
            )
        }
    }
}

@Composable
internal fun <T> ValueMenu(label: String, values: List<T>, valueLabel: (T) -> String, onSelected: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(label) }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            values.forEach { value ->
                DropdownMenuItem(text = { Text(valueLabel(value)) }, onClick = { expanded = false; onSelected(value) })
            }
        }
    }
}

@Composable
internal fun <T> NullableValueMenu(
    label: String,
    allLabel: String,
    values: List<T>,
    valueLabel: (T) -> String,
    onAll: () -> Unit,
    onSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(label) }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(allLabel) }, onClick = { expanded = false; onAll() })
            values.forEach { value ->
                DropdownMenuItem(text = { Text(valueLabel(value)) }, onClick = { expanded = false; onSelected(value) })
            }
        }
    }
}
