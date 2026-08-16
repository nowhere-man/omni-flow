package com.omniflow.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omniflow.core.domain.model.Account
import com.omniflow.core.domain.model.AccountType
import com.omniflow.core.domain.model.Category
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.Reminder
import com.omniflow.core.domain.model.ReminderSchedule
import com.omniflow.core.domain.model.ReminderScheduleKind
import com.omniflow.core.domain.model.ReminderType
import com.omniflow.core.domain.model.Rule
import com.omniflow.core.domain.model.RuleActionType
import com.omniflow.core.domain.model.RuleConditionType
import com.omniflow.core.domain.model.Tag
import com.omniflow.core.domain.model.TransactionType

@Composable
internal fun ManagementPage(
    page: MorePage,
    state: MoreUiState,
    viewModel: OmniFlowViewModel,
    onPage: (MorePage) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenLedger: (String) -> Unit,
    onOpenAccount: (String) -> Unit,
    onOpenCategory: (String) -> Unit,
) {
    when (page) {
        MorePage.LEDGERS -> LedgerManagement(state, viewModel, onOpenLedger)
        MorePage.ACCOUNTS -> AccountManagement(state, viewModel, onOpenAccount)
        MorePage.ASSETS -> AssetManagement(state, onOpenAccount)
        MorePage.CATEGORIES -> CategoryManagement(state, viewModel, onOpenCategory)
        MorePage.TAGS -> TagManagement(state, viewModel)
        MorePage.RULES -> RuleManagement(state, viewModel)
        MorePage.REMINDERS -> ReminderManagement(state, viewModel, onRequestNotificationPermission)
        else -> Unit
    }
}

@Composable
private fun LedgerManagement(state: MoreUiState, viewModel: OmniFlowViewModel, onOpen: (String) -> Unit) {
    var editing by remember { mutableStateOf<com.omniflow.core.domain.model.Ledger?>(null) }
    var showNew by remember { mutableStateOf(false) }
    var menuFor by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf<com.omniflow.core.domain.model.Ledger?>(null) }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { Button(onClick = { showNew = true }, modifier = Modifier.fillMaxWidth()) { Text("新建账本") } }
        state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        items(state.ledgers, key = { it.id }) { ledger ->
            Surface(
                onClick = { onOpen(ledger.id) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LedgerCoverBox(ledger.coverKey, Modifier.size(52.dp), iconSize = 26)
                    Spacer(Modifier.size(14.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                ledger.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (state.defaultLedgerId == ledger.id) {
                                Spacer(Modifier.size(6.dp))
                                Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primary) {
                                    Text(
                                        "默认",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                }
                            }
                        }
                        Text(
                            ledgerCover(ledger.coverKey).label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Box {
                        TextButton(onClick = { menuFor = ledger.id }) { Text("⋯") }
                        DropdownMenu(expanded = menuFor == ledger.id, onDismissRequest = { menuFor = null }) {
                            DropdownMenuItem(text = { Text("编辑") }, onClick = { menuFor = null; editing = ledger })
                            if (state.defaultLedgerId != ledger.id) {
                                DropdownMenuItem(
                                    text = { Text("设为默认账本") },
                                    onClick = { menuFor = null; viewModel.setDefaultLedger(ledger.id) },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                                onClick = { menuFor = null; confirmDelete = ledger },
                            )
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(88.dp)) }
    }
    if (showNew || editing != null) {
        LedgerSheet(editing, onDismiss = { showNew = false; editing = null }) { name, cover ->
            viewModel.saveLedger(editing?.id, name, cover)
            showNew = false
            editing = null
        }
    }
    confirmDelete?.let { ledger ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("删除账本“${ledger.name}”？") },
            text = { Text("该账本下的交易、分类、标签和规则都会一并删除，且无法撤销。") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = null; viewModel.deleteLedger(ledger.id) }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun LedgerSheet(
    ledger: com.omniflow.core.domain.model.Ledger?,
    onDismiss: () -> Unit,
    onSave: (String, String?) -> Unit,
) {
    var name by remember(ledger) { mutableStateOf(ledger?.name.orEmpty()) }
    var cover by remember(ledger) { mutableStateOf(ledger?.coverKey ?: LedgerCovers.first().key) }
    ManagementSheet(
        title = if (ledger == null) "新建账本" else "编辑账本",
        onDismiss = onDismiss,
        onSave = { onSave(name.trim(), cover) },
        saveEnabled = name.isNotBlank(),
    ) {
        OutlinedTextField(name, { name = it }, label = { Text("账本名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Text("封面", style = MaterialTheme.typography.labelLarge)
        LedgerCoverPicker(cover) { cover = it }
    }
}

@Composable
private fun AccountManagement(state: MoreUiState, viewModel: OmniFlowViewModel, onOpen: (String) -> Unit) {
    var editing by remember { mutableStateOf<Account?>(null) }
    var showNew by remember { mutableStateOf(false) }
    val grouped = AccountType.entries.mapNotNull { type ->
        state.accounts.filter { it.type == type }.takeIf(List<Account>::isNotEmpty)?.let { type to it }
    }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { Button(onClick = { showNew = true }, modifier = Modifier.fillMaxWidth()) { Text("新建账户") } }
        state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        grouped.forEach { (type, accounts) ->
            val subtotal = accounts.fold(Money.Zero) { total, account -> total + account.balance }
            item(key = "header-${type.name}") {
                ManagementGroupHeader("${type.label} · ${accounts.size}", subtotal.asRmb())
            }
            itemsIndexed(accounts, key = { _, account -> account.id }) { index, account ->
                AccountRow(
                    account = account,
                    shape = groupedOptionShape(index, accounts.size),
                    onClick = { onOpen(account.id) },
                    onEdit = { editing = account },
                )
            }
        }
        item { Spacer(Modifier.height(88.dp)) }
    }
    if (showNew || editing != null) {
        AccountSheet(editing, onDismiss = { showNew = false; editing = null }, onDelete = { id ->
            viewModel.deleteAccount(id)
            editing = null
        }) { name, type, icon, card, note, balance, included ->
            viewModel.saveAccount(editing?.id, name, type, icon, card, note, balance, included)
            showNew = false
            editing = null
        }
    }
}

@Composable
private fun AccountRow(
    account: Account,
    shape: androidx.compose.ui.graphics.Shape,
    onClick: () -> Unit,
    onEdit: () -> Unit,
) {
    // 信用卡这类负债余额为负，用 error 语义色，和列表、统计口径一致
    val isLiability = account.balance.minor < 0
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = shape, color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIcon(account.iconKey)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(account.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val details = listOfNotNull(
                    account.cardNumber?.takeLast(4)?.takeIf(String::isNotBlank)?.let { "尾号 $it" },
                    if (!account.includeInTotalAssets) "不计入总资产" else null,
                ).joinToString(" · ")
                if (details.isNotBlank()) {
                    Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
            Text(
                account.balance.asRmb(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isLiability) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            TextButton(onClick = onEdit) { Text("编辑") }
        }
    }
}

@Composable
private fun AccountSheet(
    account: Account?,
    onDismiss: () -> Unit,
    onDelete: (String) -> Unit,
    onSave: (String, AccountType, String, String?, String?, Money, Boolean) -> Unit,
) {
    var name by remember(account) { mutableStateOf(account?.name.orEmpty()) }
    var type by remember(account) { mutableStateOf(account?.type ?: AccountType.CASH) }
    var icon by remember(account) { mutableStateOf(account?.iconKey ?: "wallet-cards") }
    var card by remember(account) { mutableStateOf(account?.cardNumber.orEmpty()) }
    var note by remember(account) { mutableStateOf(account?.note.orEmpty()) }
    var balance by remember(account) { mutableStateOf(account?.balance?.toDecimal().orEmpty()) }
    var included by remember(account) { mutableStateOf(account?.includeInTotalAssets ?: true) }
    var balanceError by remember(account) { mutableStateOf<String?>(null) }
    var confirmDelete by remember(account) { mutableStateOf(false) }
    ManagementSheet(
        title = if (account == null) "新建账户" else "编辑账户",
        onDismiss = onDismiss,
        onSave = {
            val parsed = balance.toMoneyOrNull()
            if (parsed == null) {
                balanceError = "请输入有效余额，最多两位小数"
            } else {
                balanceError = null
                onSave(name.trim(), type, icon, card.ifBlank { null }, note.ifBlank { null }, parsed, included)
            }
        },
        saveEnabled = name.isNotBlank(),
    ) {
        OutlinedTextField(name, { name = it }, label = { Text("账户名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Text("类型", style = MaterialTheme.typography.labelLarge)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            AccountType.entries.forEach { entry ->
                FilterChip(selected = type == entry, onClick = { type = entry }, label = { Text(entry.label) })
            }
        }
        Text("图标", style = MaterialTheme.typography.labelLarge)
        IconPickerGrid(AccountIconOptions, icon) { icon = it }
        OutlinedTextField(
            balance,
            { balance = it; balanceError = null },
            label = { Text("当前余额") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            isError = balanceError != null,
            supportingText = { balanceError?.let { Text(it) } },
        )
        OutlinedTextField(card, { card = it }, label = { Text("卡号（可选）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(note, { note = it }, label = { Text("备注（可选）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("计入总资产")
                Text("关闭后该账户不参与净资产统计", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(included, { included = it })
        }
        if (account != null) {
            TextButton(onClick = { confirmDelete = true }) {
                Text("删除账户", color = MaterialTheme.colorScheme.error)
            }
        }
    }
    if (confirmDelete && account != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除账户“${account.name}”？") },
            text = { Text("此操作无法撤销。") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete(account.id) }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun AssetManagement(state: MoreUiState, onOpenAccount: (String) -> Unit) {
    val counted = state.accounts.filter { it.includeInTotalAssets }
    val assets = counted.filter { it.balance.minor >= 0 }
    val liabilities = counted.filter { it.balance.minor < 0 }
    val excluded = state.accounts.filterNot { it.includeInTotalAssets }
    val assetTotal = assets.fold(Money.Zero) { total, account -> total + account.balance }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("净资产", style = MaterialTheme.typography.labelLarge)
                    Text(
                        state.accountSummary.netAssets.asRmb(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text("资产", style = MaterialTheme.typography.labelMedium)
                            Text(state.accountSummary.assets.asRmb(), fontWeight = FontWeight.Medium)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("负债", style = MaterialTheme.typography.labelMedium)
                            Text(state.accountSummary.liabilities.asRmb(), fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
        if (assets.isNotEmpty()) {
            item { ManagementGroupHeader("资产构成", assetTotal.asRmb()) }
            itemsIndexed(assets, key = { _, account -> "asset-${account.id}" }) { index, account ->
                AssetRow(
                    account = account,
                    share = if (assetTotal.minor > 0) account.balance.minor.toFloat() / assetTotal.minor else 0f,
                    shape = groupedOptionShape(index, assets.size),
                    onClick = { onOpenAccount(account.id) },
                )
            }
        }
        if (liabilities.isNotEmpty()) {
            item { ManagementGroupHeader("负债", state.accountSummary.liabilities.asRmb()) }
            itemsIndexed(liabilities, key = { _, account -> "liability-${account.id}" }) { index, account ->
                AssetRow(account, 0f, groupedOptionShape(index, liabilities.size)) { onOpenAccount(account.id) }
            }
        }
        if (excluded.isNotEmpty()) {
            item { ManagementGroupHeader("不计入总资产", "${excluded.size} 个") }
            itemsIndexed(excluded, key = { _, account -> "excluded-${account.id}" }) { index, account ->
                AssetRow(account, 0f, groupedOptionShape(index, excluded.size)) { onOpenAccount(account.id) }
            }
        }
        if (state.accounts.isEmpty()) {
            item {
                Text(
                    "还没有账户，先去「账户」里创建一个",
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item { Spacer(Modifier.height(88.dp)) }
    }
}

@Composable
private fun AssetRow(
    account: Account,
    share: Float,
    shape: androidx.compose.ui.graphics.Shape,
    onClick: () -> Unit,
) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = shape, color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundIcon(account.iconKey, size = 34)
                Spacer(Modifier.size(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(account.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(account.type.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    account.balance.asRmb(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (account.balance.minor < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
            }
            if (share > 0f) {
                LinearProgressIndicator(
                    progress = { share },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                )
            }
        }
    }
}

@Composable
private fun CategoryManagement(state: MoreUiState, viewModel: OmniFlowViewModel, onOpen: (String) -> Unit) {
    var editing by remember { mutableStateOf<Category?>(null) }
    var newParentId by remember { mutableStateOf<String?>(null) }
    var showNew by remember { mutableStateOf(false) }
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var expanded by remember { mutableStateOf<Set<String>>(emptySet()) }
    val primaries = state.categories.filter { it.parentId == null && it.type == type }
    Column(Modifier.fillMaxSize()) {
        LedgerPickerBar(state.ledgers, state.selectedLedgerId) { viewModel.selectMoreLedger(it) }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TransactionType.entries.forEach { entry ->
                FilterChip(
                    selected = type == entry,
                    onClick = { type = entry },
                    modifier = Modifier.weight(1f),
                    label = { Text(entry.label, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Button(onClick = { newParentId = null; showNew = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("新建一级分类")
                }
            }
            state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            if (primaries.isEmpty()) {
                item {
                    Text(
                        "该账本还没有${type.label}分类",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(primaries, key = { it.id }) { primary ->
                val children = state.categories.filter { it.parentId == primary.id }
                CategoryCardRow(
                    primary = primary,
                    children = children,
                    expanded = primary.id in expanded,
                    onToggle = {
                        expanded = if (primary.id in expanded) expanded - primary.id else expanded + primary.id
                    },
                    onOpen = { onOpen(primary.id) },
                    onEdit = { editing = primary },
                    onEditChild = { editing = it },
                    onAddChild = { newParentId = primary.id; showNew = true },
                )
            }
            item { Spacer(Modifier.height(88.dp)) }
        }
    }
    val ledgerId = state.selectedLedgerId
    if ((showNew || editing != null) && ledgerId != null) {
        CategorySheet(
            category = editing,
            presetParentId = newParentId,
            presetType = type,
            ledgerId = ledgerId,
            categories = state.categories,
            onDismiss = { showNew = false; editing = null; newParentId = null },
            onDelete = { id -> viewModel.deleteCategory(id); editing = null },
        ) { parent, name, icon, categoryType ->
            viewModel.saveCategory(editing?.id, ledgerId, parent, name, icon, categoryType)
            showNew = false
            editing = null
            newParentId = null
        }
    }
}

@Composable
private fun CategoryCardRow(
    primary: Category,
    children: List<Category>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onEditChild: (Category) -> Unit,
    onAddChild: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundIcon(primary.iconKey)
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(primary.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (children.isEmpty()) "暂无二级分类" else "${children.size} 个二级分类",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onEdit) { Text("编辑") }
                TextButton(onClick = onToggle) { Text(if (expanded) "收起" else "展开") }
            }
            if (expanded) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    children.forEach { child ->
                        FilterChip(selected = false, onClick = { onEditChild(child) }, label = { Text(child.name, maxLines = 1) })
                    }
                    TextButton(onClick = onAddChild) { Text("+ 新建") }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun CategorySheet(
    category: Category?,
    presetParentId: String?,
    presetType: TransactionType,
    ledgerId: String,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onDelete: (String) -> Unit,
    onSave: (String?, String, String?, TransactionType) -> Unit,
) {
    var type by remember(category) { mutableStateOf(category?.type ?: presetType) }
    var parentId by remember(category) { mutableStateOf(category?.parentId ?: presetParentId) }
    var name by remember(category) { mutableStateOf(category?.name.orEmpty()) }
    var icon by remember(category) { mutableStateOf(category?.iconKey ?: CategoryIconOptions.first().key) }
    var confirmDelete by remember(category) { mutableStateOf(false) }
    val parents = categories.filter { it.ledgerId == ledgerId && it.parentId == null && it.type == type && it.id != category?.id }
    ManagementSheet(
        title = if (category == null) "新建分类" else "编辑分类",
        onDismiss = onDismiss,
        onSave = { onSave(parentId, name.trim(), if (parentId == null) icon else null, type) },
        saveEnabled = name.isNotBlank(),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TransactionType.entries.forEach { value ->
                FilterChip(selected = type == value, onClick = { type = value; parentId = null }, label = { Text(value.label) })
            }
        }
        NullableValueMenu(
            label = parents.firstOrNull { it.id == parentId }?.name ?: "作为一级分类",
            allLabel = "作为一级分类",
            values = parents,
            valueLabel = Category::name,
            onAll = { parentId = null },
            onSelected = { parentId = it.id },
        )
        OutlinedTextField(name, { name = it }, label = { Text("分类名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        if (parentId == null) {
            Text("图标", style = MaterialTheme.typography.labelLarge)
            IconPickerGrid(CategoryIconOptions, icon) { icon = it }
        }
        if (category != null) {
            TextButton(onClick = { confirmDelete = true }) {
                Text("删除分类", color = MaterialTheme.colorScheme.error)
            }
        }
    }
    if (confirmDelete && category != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除分类“${category.name}”？") },
            text = { Text("已经记到这个分类下的交易不会被删除，但会失去分类。此操作无法撤销。") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete(category.id) }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun TagManagement(state: MoreUiState, viewModel: OmniFlowViewModel) {
    var editing by remember { mutableStateOf<Tag?>(null) }
    var showNew by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        LedgerPickerBar(state.ledgers, state.selectedLedgerId) { viewModel.selectMoreLedger(it) }
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { Button(onClick = { showNew = true }, modifier = Modifier.fillMaxWidth()) { Text("新建标签") } }
            state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            if (state.tags.isEmpty()) {
                item {
                    Text(
                        "该账本还没有标签",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            itemsIndexed(state.tags, key = { _, tag -> tag.id }) { index, tag ->
                Surface(
                    onClick = { editing = tag },
                    modifier = Modifier.fillMaxWidth(),
                    shape = groupedOptionShape(index, state.tags.size),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("#", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.size(10.dp))
                        Text(tag.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("编辑", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            item { Spacer(Modifier.height(88.dp)) }
        }
    }
    val ledgerId = state.selectedLedgerId
    if ((showNew || editing != null) && ledgerId != null) {
        TagSheet(
            tag = editing,
            onDismiss = { showNew = false; editing = null },
            onDelete = { id -> viewModel.deleteTag(id); editing = null },
        ) { name ->
            viewModel.saveTag(editing?.id, ledgerId, name)
            showNew = false
            editing = null
        }
    }
}

@Composable
private fun TagSheet(tag: Tag?, onDismiss: () -> Unit, onDelete: (String) -> Unit, onSave: (String) -> Unit) {
    var name by remember(tag) { mutableStateOf(tag?.name.orEmpty()) }
    ManagementSheet(
        title = if (tag == null) "新建标签" else "编辑标签",
        onDismiss = onDismiss,
        onSave = { onSave(name.trim()) },
        saveEnabled = name.isNotBlank(),
    ) {
        OutlinedTextField(name, { name = it }, label = { Text("标签名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        if (tag != null) {
            TextButton(onClick = { onDelete(tag.id) }) {
                Text("删除标签", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun RuleManagement(state: MoreUiState, viewModel: OmniFlowViewModel) {
    var editing by remember { mutableStateOf<Rule?>(null) }
    var showNew by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<Rule?>(null) }
    val orderedRules = state.rules.sortedBy(Rule::priority)
    Column(Modifier.fillMaxSize()) {
        LedgerPickerBar(state.ledgers, state.selectedLedgerId) { viewModel.selectMoreLedger(it) }
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { Button(onClick = { showNew = true }, modifier = Modifier.fillMaxWidth()) { Text("新建规则") } }
            state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            if (orderedRules.isEmpty()) {
                item {
                    Text(
                        "还没有规则。规则会在导入账单时按顺序自动套用。",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        textAlign = TextAlign.Center,
                        color = mutedContent(),
                    )
                }
            } else {
                item {
                    Text("长按可拖动调整优先级，靠前的先生效", style = OmniText.caption, color = mutedContent())
                }
                item {
                    ReorderableColumn(
                        items = orderedRules,
                        key = Rule::id,
                        rowHeight = 76.dp,
                        resetSignal = state.error,
                        onReordered = { reordered -> viewModel.reorderRules(reordered.map(Rule::id)) },
                    ) { rule, dragging ->
                        RuleRow(
                            rule = rule,
                            dragging = dragging,
                            onEdit = { editing = rule },
                            onDelete = { confirmDelete = rule },
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(88.dp)) }
        }
    }
    val ledgerId = state.selectedLedgerId
    if ((showNew || editing != null) && ledgerId != null) {
        RuleDialog(editing, state, onDismiss = { showNew = false; editing = null }) { name, condition, conditionValue, action, actionValue, priority ->
            viewModel.saveRule(editing?.id, ledgerId, name, condition, conditionValue, action, actionValue, priority)
            showNew = false
            editing = null
        }
    }
    confirmDelete?.let { rule ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("删除规则“${rule.name}”？") },
            text = { Text("此操作无法撤销。") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = null; viewModel.deleteRule(rule.id) }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun RuleRow(rule: Rule, dragging: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(OmniRadius.medium),
        color = if (dragging) MaterialTheme.colorScheme.surfaceBright else surfaceInset(),
        tonalElevation = if (dragging) 6.dp else 0.dp,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${rule.priority + 1}",
                modifier = Modifier.padding(end = 12.dp),
                style = OmniText.titleRow,
                color = mutedContent(),
            )
            Column(Modifier.weight(1f)) {
                Text(rule.name, style = OmniText.titleRow, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${rule.conditionType.label}: ${rule.conditionValue} → ${rule.actionType.label}",
                    style = OmniText.caption,
                    color = mutedContent(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(onClick = onEdit) { Text("编辑") }
            TextButton(onClick = onDelete) { Text("删除", color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun RuleDialog(
    rule: Rule?,
    state: MoreUiState,
    onDismiss: () -> Unit,
    onSave: (String, RuleConditionType, String, RuleActionType, String, Int) -> Unit,
) {
    var name by remember(rule) { mutableStateOf(rule?.name.orEmpty()) }
    var condition by remember(rule) { mutableStateOf(rule?.conditionType ?: RuleConditionType.NOTE_CONTAINS) }
    var conditionValue by remember(rule) { mutableStateOf(rule?.conditionValue.orEmpty()) }
    var action by remember(rule) { mutableStateOf(rule?.actionType ?: RuleActionType.SET_CATEGORY) }
    var actionValue by remember(rule) { mutableStateOf(rule?.actionValue.orEmpty()) }
    var priority by remember(rule) { mutableStateOf((rule?.priority ?: state.rules.size).toString()) }
    var validationError by remember(rule) { mutableStateOf<String?>(null) }
    FormDialog(if (rule == null) "新建规则" else "编辑规则", onDismiss, {
        validationError = when {
            name.isBlank() -> "请输入规则名称"
            conditionValue.isBlank() -> "请输入匹配值"
            action == RuleActionType.SET_CATEGORY && state.categories.none { it.id == actionValue } -> "请选择有效分类"
            else -> null
        }
        if (validationError == null) {
            onSave(name, condition, conditionValue, action, actionValue, priority.toIntOrNull() ?: 0)
        }
    }) {
        OutlinedTextField(name, { name = it }, label = { Text("名称") }, modifier = Modifier.fillMaxWidth())
        ValueMenu(condition.label, RuleConditionType.entries, { it.label }) { selected ->
            condition = selected
            conditionValue = if (selected == RuleConditionType.TRANSACTION_TYPE) TransactionType.EXPENSE.name else ""
            validationError = null
        }
        if (condition == RuleConditionType.TRANSACTION_TYPE) {
            ValueMenu(
                TransactionType.entries.firstOrNull { it.name == conditionValue }?.label ?: "选择收支类型",
                TransactionType.entries,
                { it.label },
            ) { conditionValue = it.name }
        } else {
            OutlinedTextField(conditionValue, { conditionValue = it }, label = { Text("匹配值") }, modifier = Modifier.fillMaxWidth())
        }
        ValueMenu(action.label, RuleActionType.entries, { it.label }) { selected ->
            action = selected
            actionValue = ""
            validationError = null
        }
        if (action == RuleActionType.SET_CATEGORY) {
            ValueMenu(
                state.categories.firstOrNull { it.id == actionValue }?.name ?: "选择分类",
                state.categories,
                Category::name,
            ) { actionValue = it.id }
        }
        OutlinedTextField(priority, { priority = it.filter(Char::isDigit) }, label = { Text("优先级") }, modifier = Modifier.fillMaxWidth())
        validationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun ReminderManagement(
    state: MoreUiState,
    viewModel: OmniFlowViewModel,
    onRequestNotificationPermission: () -> Unit,
) {
    var editing by remember { mutableStateOf<Reminder?>(null) }
    var showNew by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<Reminder?>(null) }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { Button(onClick = { showNew = true }, modifier = Modifier.fillMaxWidth()) { Text("新建提醒") } }
        state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        if (state.reminders.isEmpty()) {
            item {
                Text(
                    "还没有提醒",
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    textAlign = TextAlign.Center,
                    color = mutedContent(),
                )
            }
        }
        itemsIndexed(state.reminders, key = { _, reminder -> reminder.id }) { index, reminder ->
            Surface(
                onClick = { editing = reminder },
                modifier = Modifier.fillMaxWidth(),
                shape = groupedOptionShape(index, state.reminders.size),
                color = surfaceInset(),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(reminder.name, style = OmniText.titleRow, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${reminder.type.label} · ${reminder.schedule.kind.label}",
                            style = OmniText.caption,
                            color = mutedContent(),
                            maxLines = 1,
                        )
                    }
                    Switch(!reminder.paused, { enabled ->
                        if (enabled) onRequestNotificationPermission()
                        viewModel.setReminderPaused(reminder, !enabled)
                    })
                    TextButton(onClick = { confirmDelete = reminder }) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(88.dp)) }
    }
    confirmDelete?.let { reminder ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("删除提醒“${reminder.name}”？") },
            text = { Text("此操作无法撤销。") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = null; viewModel.deleteReminder(reminder.id) }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("取消") } },
        )
    }
    if (showNew || editing != null) {
        ReminderDialog(editing, onDismiss = { showNew = false; editing = null }) { type, name, amount, schedule, paused ->
            if (!paused) onRequestNotificationPermission()
            viewModel.saveReminder(editing?.id, type, name, amount, schedule, paused)
            showNew = false
            editing = null
        }
    }
}

@Composable
private fun ReminderDialog(
    reminder: Reminder?,
    onDismiss: () -> Unit,
    onSave: (ReminderType, String, Money?, ReminderSchedule, Boolean) -> Unit,
) {
    var type by remember(reminder) { mutableStateOf(reminder?.type ?: ReminderType.REPAYMENT) }
    var name by remember(reminder) { mutableStateOf(reminder?.name.orEmpty()) }
    var amount by remember(reminder) { mutableStateOf(reminder?.amount?.toDecimal().orEmpty()) }
    var kind by remember(reminder) { mutableStateOf(reminder?.schedule?.kind ?: ReminderScheduleKind.FIXED_REPAYMENT_DAY) }
    var day by remember(reminder) { mutableStateOf((reminder?.schedule?.dayOfMonth ?: 1).toString()) }
    var daysAfter by remember(reminder) { mutableStateOf((reminder?.schedule?.daysAfter ?: 0).toString()) }
    var weekday by remember(reminder) { mutableStateOf((reminder?.schedule?.dayOfWeek ?: 1).toString()) }
    var month by remember(reminder) { mutableStateOf((reminder?.schedule?.month ?: 1).toString()) }
    var paused by remember(reminder) { mutableStateOf(reminder?.paused ?: false) }
    val kinds = if (type == ReminderType.REPAYMENT) {
        listOf(ReminderScheduleKind.FIXED_REPAYMENT_DAY, ReminderScheduleKind.DAYS_AFTER_STATEMENT)
    } else {
        listOf(ReminderScheduleKind.DAILY, ReminderScheduleKind.WEEKLY, ReminderScheduleKind.MONTHLY, ReminderScheduleKind.YEARLY)
    }
    FormDialog(if (reminder == null) "新建提醒" else "编辑提醒", onDismiss, {
        onSave(
            type,
            name,
            amount.takeIf(String::isNotBlank)?.toMoneyOrNull(),
            ReminderSchedule(
                kind = kind,
                dayOfMonth = day.toIntOrNull(),
                daysAfter = daysAfter.toIntOrNull(),
                dayOfWeek = weekday.toIntOrNull(),
                month = month.toIntOrNull(),
            ),
            paused,
        )
    }) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReminderType.entries.forEach { value ->
                FilterChip(
                    selected = type == value,
                    onClick = { type = value; kind = if (value == ReminderType.REPAYMENT) ReminderScheduleKind.FIXED_REPAYMENT_DAY else ReminderScheduleKind.MONTHLY },
                    label = { Text(value.label) },
                )
            }
        }
        OutlinedTextField(name, { name = it }, label = { Text("名称") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(amount, { amount = it }, label = { Text("金额（可选）") }, modifier = Modifier.fillMaxWidth())
        ValueMenu(kind.label, kinds, { it.label }) { kind = it }
        when (kind) {
            ReminderScheduleKind.FIXED_REPAYMENT_DAY,
            ReminderScheduleKind.MONTHLY -> NumberField("每月几号", day) { day = it }
            ReminderScheduleKind.DAYS_AFTER_STATEMENT -> {
                NumberField("账单日", day) { day = it }
                NumberField("账单日后天数", daysAfter) { daysAfter = it }
            }
            ReminderScheduleKind.WEEKLY -> NumberField("星期（1-7）", weekday) { weekday = it }
            ReminderScheduleKind.YEARLY -> {
                NumberField("月份", month) { month = it }
                NumberField("日期", day) { day = it }
            }
            ReminderScheduleKind.DAILY -> Unit
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("暂停", modifier = Modifier.weight(1f))
            Switch(paused, { paused = it })
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, onValue: (String) -> Unit) {
    OutlinedTextField(value, { onValue(it.filter(Char::isDigit)) }, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun FormDialog(
    title: String,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp), content = content) },
        confirmButton = { TextButton(onClick = onSave) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

private val AccountType.label: String
    get() = when (this) {
        AccountType.CASH -> "现金"
        AccountType.DEBIT_CARD -> "储蓄卡"
        AccountType.CREDIT_CARD -> "信用卡"
        AccountType.E_WALLET -> "电子钱包"
        AccountType.INVESTMENT -> "投资账户"
    }

private val TransactionType.label: String get() = if (this == TransactionType.EXPENSE) "支出" else "收入"
private val ReminderType.label: String get() = if (this == ReminderType.REPAYMENT) "还款提醒" else "订阅提醒"
private val ReminderScheduleKind.label: String
    get() = when (this) {
        ReminderScheduleKind.FIXED_REPAYMENT_DAY -> "固定还款日"
        ReminderScheduleKind.DAYS_AFTER_STATEMENT -> "账单日后 N 天"
        ReminderScheduleKind.DAILY -> "每天"
        ReminderScheduleKind.WEEKLY -> "每周"
        ReminderScheduleKind.MONTHLY -> "每月"
        ReminderScheduleKind.YEARLY -> "每年"
    }

private val RuleConditionType.label: String
    get() = when (this) {
        RuleConditionType.NOTE_CONTAINS -> "备注包含"
        RuleConditionType.TRANSACTION_TYPE -> "收支类型"
        RuleConditionType.TRANSACTION_SOURCE -> "来源平台"
    }

private val RuleActionType.label: String
    get() = when (this) {
        RuleActionType.SET_CATEGORY -> "设置分类"
        RuleActionType.SET_EXCLUDED -> "设为不计入收支"
        RuleActionType.EXCLUDE -> "排除不入账"
    }

private fun Money.toDecimal(): String = java.math.BigDecimal.valueOf(minor, 2).toPlainString()

private fun String.toMoneyOrNull(): Money? {
    val value = trim().toBigDecimalOrNull() ?: return null
    if (value.scale() > 2) return null
    return runCatching { Money(value.movePointRight(2).longValueExact()) }.getOrNull()
}

private val BundledIconKeys = listOf(
    "banknote", "wallet-cards", "wallet", "landmark", "shopping-bag", "utensils",
    "bus", "wrench", "film", "heart-pulse", "plane", "car", "house", "smartphone",
    "shirt", "chart-line", "briefcase-business", "trophy", "gift", "play", "category",
)

/** 账户图标网格用的选项，key 沿用 [BundledIconKeys]。 */
internal val AccountIconOptions = BundledIconKeys.map { key ->
    CategoryIconOption(key, CategoryIconOptions.firstOrNull { it.key == key }?.label ?: key)
}
