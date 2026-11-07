package com.omniflow.android.ui

import android.content.Context
import android.app.DatePickerDialog
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.omniflow.core.domain.model.AppearanceMode
import com.omniflow.core.domain.model.DateRange
import com.omniflow.core.domain.model.ImportPreviewPhase
import com.omniflow.core.domain.model.Ledger
import com.omniflow.core.domain.model.LedgerScope
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.SyncPhase
import com.omniflow.core.domain.model.SyncTarget
import com.omniflow.core.domain.model.ThemeColor
import com.omniflow.core.domain.model.TransactionType
import com.omniflow.core.parser.ImportFormat
import com.omniflow.android.WebDavCredentials
import com.omniflow.android.WebDavSyncAdapter
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

internal enum class MorePage(val label: String, val icon: ImageVector) {
    HOME("更多", Icons.Default.Settings),
    SETTINGS("通用设置", Icons.Default.Tune),
    DATA("数据同步", Icons.Default.CloudSync),
    AI("AI 分类", Icons.Default.AutoAwesome),
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
            MorePage.HOME -> MoreHome(state, onPage = onPage)
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
            MorePage.RULES,
            MorePage.REMINDERS,
            MorePage.LEDGERS,
            MorePage.ACCOUNTS,
            MorePage.ASSETS,
            MorePage.CATEGORIES,
            MorePage.TAGS -> ManagementPage(
                page,
                state,
                viewModel,
                onPage = onPage,
                onRequestNotificationPermission = onRequestNotificationPermission,
                onOpenLedger = onOpenLedger,
                onOpenAccount = onOpenAccount,
                onOpenCategory = onOpenCategory,
            )
        }
    }
}

@Composable
private fun MoreHome(state: MoreUiState, onPage: (MorePage) -> Unit) {
    var hideAmount by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        Modifier.readableContentWidth().fillMaxHeight().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(OmniRadius.medium),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("净资产", style = OmniText.caption)
                        IconButton(
                            onClick = { hideAmount = !hideAmount },
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                if (hideAmount) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (hideAmount) "显示金额" else "隐藏金额",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                            )
                        }
                    }
                    Text(
                        if (hideAmount) "••••••" else state.accountSummary.netAssets.asRmb(),
                        style = OmniText.amountHero,
                        maxLines = 1,
                    )
                    Text(
                        if (hideAmount) "资产 •••••• · 负债 ••••••" else "资产 ${state.accountSummary.assets.asRmb()} · 负债 ${state.accountSummary.liabilities.asRmb()}",
                        style = OmniText.caption,
                    )
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(syncLabel(state), style = OmniText.caption)
                    }
                }
            }
        }
        item { MoreSection(listOf(MorePage.SETTINGS), state, onPage) }
        item { MoreSection(listOf(MorePage.LEDGERS, MorePage.ACCOUNTS, MorePage.ASSETS), state, onPage) }
        item { MoreSection(listOf(MorePage.IMPORT, MorePage.EXPORT, MorePage.DATA, MorePage.AI), state, onPage) }
        item { MoreSection(listOf(MorePage.CATEGORIES, MorePage.TAGS), state, onPage) }
        item { MoreSection(listOf(MorePage.BUDGETS, MorePage.RULES, MorePage.REMINDERS), state, onPage) }
        state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error, style = OmniText.caption) } }
        item {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("OmniFlow", style = OmniText.titleRow, color = mutedContent())
                Text("本地优先 · 开源透明 · 隐私至上", style = OmniText.caption, color = mutedContent())
            }
        }
        item { Spacer(Modifier.height(88.dp)) }
    }
}

@Composable
private fun MoreSection(pages: List<MorePage>, state: MoreUiState, onPage: (MorePage) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        pages.forEachIndexed { index, page ->
            GroupedOptionRow(
                title = page.label,
                icon = page.icon,
                shape = groupedOptionShape(index, pages.size),
                subtitle = page.description(state),
                onClick = { onPage(page) },
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        page.badge(state)?.let { badgeText ->
                            Surface(
                                shape = RoundedCornerShape(OmniRadius.small),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                            ) {
                                Text(
                                    badgeText,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = OmniText.caption,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = mutedContent(),
                        )
                    }
                },
            )
        }
    }
}

private fun MorePage.badge(state: MoreUiState): String? = when (this) {
    MorePage.LEDGERS -> "${state.ledgers.size}"
    MorePage.ACCOUNTS -> "${state.accounts.size}"
    MorePage.CATEGORIES -> "${state.categories.count { it.parentId == null }}"
    MorePage.TAGS -> if (state.tags.isNotEmpty()) "${state.tags.size}" else null
    MorePage.RULES -> if (state.rules.isNotEmpty()) "${state.rules.size}" else null
    MorePage.REMINDERS -> if (state.reminders.isNotEmpty()) "${state.reminders.size}" else null
    MorePage.DATA -> if (state.syncState.phase == SyncPhase.RUNNING) "同步中" else null
    else -> null
}

private fun MorePage.description(state: MoreUiState): String = when (this) {
    MorePage.HOME -> ""
    MorePage.DATA -> syncLabel(state)
    MorePage.AI -> "导入时让大模型建议一级分类"
    MorePage.IMPORT -> "从支付宝、微信、银行流水批量导入"
    MorePage.EXPORT -> "导出 CSV 表格或 JSON 备份"
    MorePage.SETTINGS -> "外观、主题色、应用锁与缓存"
    MorePage.LEDGERS -> "账本封面、默认账本与收支概览"
    MorePage.ACCOUNTS -> "账户余额、卡片信息与流水"
    MorePage.ASSETS -> "净资产构成与负债"
    MorePage.CATEGORIES -> "收支分类、图标与排序"
    MorePage.TAGS -> "账本标签"
    MorePage.BUDGETS -> budgetLabel(state)
    MorePage.RULES -> "自动分类和排除规则"
    MorePage.REMINDERS -> "还款与周期记账提醒"
}

@Composable
private fun GroupedOptionRow(
    title: String,
    icon: ImageVector,
    shape: Shape,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable (() -> Unit)? = null,
) {
    val modifier = if (onClick == null) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)
    }
    Surface(modifier = modifier, color = surfaceCard(), shape = shape) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 图标改中性：主题色留给选中态和图表，列表图标跟着变会喧宾夺主
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = mutedContent(),
            )
            Column(
                Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(title, style = OmniText.titleRow)
                subtitle?.let { Text(it, style = OmniText.caption, color = mutedContent()) }
                content?.invoke()
            }
            trailing?.invoke()
        }
    }
}

internal fun groupedOptionShape(index: Int, size: Int): Shape = when {
    size == 1 -> RoundedCornerShape(OmniRadius.medium)
    index == 0 -> RoundedCornerShape(topStart = OmniRadius.medium, topEnd = OmniRadius.medium, bottomStart = 4.dp, bottomEnd = 4.dp)
    index == size - 1 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = OmniRadius.medium, bottomEnd = OmniRadius.medium)
    else -> RoundedCornerShape(4.dp)
}

@Composable
private fun SettingsPage(
    state: MoreUiState,
    viewModel: OmniFlowViewModel,
    dynamicColorEnabled: Boolean,
    onDynamicColorChanged: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    var cacheSubtitle by remember { mutableStateOf("清理导入临时文件与网络缓存") }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                GroupedOptionRow(
                    title = "应用锁",
                    icon = Icons.Default.Lock,
                    subtitle = "启动或回到前台时验证设备凭据",
                    shape = groupedOptionShape(0, 5),
                    trailing = { Switch(state.preferences.appLockEnabled, viewModel::setAppLockEnabled) },
                )
                GroupedOptionRow(
                    title = "界面外观",
                    icon = Icons.Default.DarkMode,
                    subtitle = "跟随系统、浅色或深色",
                    shape = groupedOptionShape(1, 5),
                    content = {
                        OmniSegmented(
                            options = AppearanceMode.entries,
                            selected = state.preferences.appearanceMode,
                            label = { mode ->
                                when (mode) {
                                    AppearanceMode.SYSTEM -> "系统"
                                    AppearanceMode.LIGHT -> "浅色"
                                    AppearanceMode.DARK -> "深色"
                                }
                            },
                            onSelected = { viewModel.setAppearanceMode(it) },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        )
                    },
                )
                GroupedOptionRow(
                    title = "主题色",
                    icon = Icons.Default.Palette,
                    subtitle = "选择按钮和导航的全局强调色",
                    shape = groupedOptionShape(2, 5),
                    content = {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(ThemeColor.entries, key = { it.name }) { color ->
                                val selected = state.preferences.themeColor == color
                                Column(
                                    modifier = Modifier
                                        .width(64.dp)
                                        .selectable(
                                            selected = selected,
                                            onClick = { viewModel.setThemeColor(color) },
                                            role = Role.RadioButton,
                                        ),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .border(
                                                width = if (selected) 3.dp else 0.dp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                shape = CircleShape,
                                            ),
                                        shape = CircleShape,
                                        color = themePrimaryColor(color, false),
                                    ) {}
                                    Text(themeColorLabel(color), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                }
                            }
                        }
                    },
                )
                GroupedOptionRow(
                    title = "动态取色",
                    icon = Icons.Default.Palette,
                    subtitle = "使用 Android 系统壁纸生成 Material 3 配色（Android 12+）",
                    shape = groupedOptionShape(3, 5),
                    trailing = { Switch(dynamicColorEnabled, onDynamicColorChanged) },
                )
                GroupedOptionRow(
                    title = "清理临时缓存",
                    icon = Icons.Default.Storage,
                    subtitle = cacheSubtitle,
                    shape = groupedOptionShape(4, 5),
                    trailing = {
                        OutlinedButton(
                            onClick = {
                                val deleted = context.cacheDir.deleteRecursively()
                                cacheSubtitle = if (deleted) "临时缓存已成功清理" else "清理完成"
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp),
                        ) {
                            Text("清理", style = MaterialTheme.typography.labelMedium)
                        }
                    },
                )
            }
        }
    }
}

private fun budgetLabel(state: MoreUiState): String {
    val overall = state.budgets.firstOrNull { it.budget.isOverall } ?: return "设置月度预算，盯住整体开销"
    return if (overall.isOverspent) {
        "本月已超支 ${(-overall.remaining.minor / 100).grouped()} 元"
    } else {
        "本月还可用 ${(overall.remaining.minor / 100).grouped()} 元"
    }
}

private fun themeColorLabel(color: ThemeColor): String = when (color) {
    ThemeColor.MIST_BLUE -> "雾蓝"
    ThemeColor.SAGE -> "鼠尾草"
    ThemeColor.LAVENDER -> "薰衣草"
    ThemeColor.SOFT_CORAL -> "柔珊瑚"
    ThemeColor.WARM_AMBER -> "暖琥珀"
    ThemeColor.GRAPHITE -> "石墨灰"
}

@Composable
private fun DataManagementPage(
    state: MoreUiState,
    viewModel: OmniFlowViewModel,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val secure = remember { context.getSharedPreferences("webdav", Context.MODE_PRIVATE) }
    var endpoint by remember { mutableStateOf(secure.getString("endpoint", "").orEmpty()) }
    var username by remember { mutableStateOf(secure.getString("username", "").orEmpty()) }
    var password by remember { mutableStateOf(WebDavCredentials.password(context)) }
    var retention by remember(state.preferences.backupRetention) { mutableStateOf(state.preferences.backupRetention.toString()) }
    var pendingRestore by remember { mutableStateOf<com.omniflow.core.domain.model.RemoteBackupMeta?>(null) }
    var showPassword by remember { mutableStateOf(false) }
    var testStatus by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(
                Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                colors = CardDefaults.cardColors(containerColor = surfaceCard()),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("WebDAV 全量备份", fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(endpoint, { endpoint = it; testStatus = null }, label = { Text("服务器目录 URL") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(username, { username = it; testStatus = null }, label = { Text("用户名") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        password,
                        { password = it; testStatus = null },
                        label = { Text("密码") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { showPassword = !showPassword }) {
                                Text(if (showPassword) "隐藏" else "显示")
                            }
                        },
                    )
                    OutlinedTextField(
                        retention,
                        { retention = it.filter(Char::isDigit) },
                        label = { Text("最大备份数") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                WebDavCredentials.save(context, endpoint, username, password)
                                viewModel.configureSync(SyncTarget.WEBDAV, retention.toIntOrNull() ?: 10)
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text("保存配置") }
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    isTesting = true
                                    testStatus = "正在测试连接…"
                                    val res = WebDavSyncAdapter.testConnection(endpoint, username, password)
                                    testStatus = if (res.isSuccess) "连接成功！" else "连接失败: ${res.exceptionOrNull()?.message ?: "未知错误"}"
                                    isTesting = false
                                }
                            },
                            enabled = !isTesting && endpoint.isNotBlank(),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(if (isTesting) "测试中…" else "测试连接")
                        }
                    }
                    testStatus?.let { status ->
                        Text(
                            status,
                            color = if (status.startsWith("连接成功")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            style = OmniText.caption,
                        )
                    }
                    if (state.syncState.phase == SyncPhase.RUNNING) {
                        LinearProgressIndicator(
                            progress = { state.syncState.progress ?: 0f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Text(syncLabel(state), style = MaterialTheme.typography.bodySmall)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = viewModel::syncNow,
                            enabled = state.syncState.phase != SyncPhase.RUNNING,
                            modifier = Modifier.weight(1f),
                        ) { Text("立即备份") }
                        OutlinedButton(onClick = viewModel::loadBackups, modifier = Modifier.weight(1f)) { Text("刷新备份") }
                    }
                }
            }
        }
        item {
            Text("云端备份", style = OmniText.caption, color = mutedContent(), modifier = Modifier.padding(top = 4.dp))
        }
        if (state.backups.isEmpty()) {
            item {
                Text(
                    if (state.syncState.lastBackupAt == null) "配置 WebDAV 后点「立即备份」创建第一个备份" else "还没有拉取到云端备份，试试「刷新备份」",
                    style = OmniText.caption,
                    color = mutedContent(),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
        items(state.backups, key = { "${it.deviceId}-${it.backupId}" }) { backup ->
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = surfaceCard()),
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(backup.createdAt.backupTimeText(), style = OmniText.titleRow)
                    }
                    TextButton(onClick = { pendingRestore = backup }) { Text("恢复") }
                }
            }
        }
        state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
    }
    pendingRestore?.let { backup ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("完整恢复备份？") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("恢复会完整替换本机全部可恢复数据，不与当前数据合并。")
                    OutlinedButton(onClick = viewModel::syncNow) { Text("先备份当前数据") }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.restoreBackup(backup); pendingRestore = null }) { Text("确认恢复") }
            },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun ImportPage(state: MoreUiState, viewModel: OmniFlowViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var ledgerId by remember(state.selectedLedgerId) { mutableStateOf(state.selectedLedgerId) }
    var selectedFormat by remember { mutableStateOf<ImportFormat?>(null) }
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && ledgerId != null) {
            scope.launch {
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }
                if (bytes != null) {
                    viewModel.importFile(
                        ledgerId = ledgerId!!,
                        fileName = context.fileName(uri),
                        bytes = bytes,
                        selectedFormat = selectedFormat,
                    )
                }
            }
        }
    }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (state.importPreview?.phase == ImportPreviewPhase.READY) {
            ImportPreviewSection(
                preview = state.importPreview,
                state = state,
                viewModel = viewModel,
                modifier = Modifier.weight(1f),
            )
            return@Column
        }
        LazyColumn(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                LedgerPickerBar(state.ledgers, ledgerId) { ledgerId = it; viewModel.selectMoreLedger(it) }
            }
            if (state.accounts.isEmpty()) {
                item {
                    Text(
                        "请先在「账户」里创建至少一个账户，导入的交易需要落到账户上。",
                        color = MaterialTheme.colorScheme.error,
                        style = OmniText.caption,
                    )
                }
            }
            item { Text("选择账单来源", style = OmniText.caption, color = mutedContent()) }
            val importOptions: List<ImportFormat?> = listOf(null) + ImportFormat.entries
            items(importOptions.chunked(3)) { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { format ->
                        if (format == null) {
                            ImportSourceTile(
                                label = "自动识别",
                                icon = {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(44.dp))
                                },
                                enabled = ledgerId != null && state.accounts.isNotEmpty() && !state.isImporting,
                                modifier = Modifier.weight(1f),
                            ) {
                                selectedFormat = null
                                fileLauncher.launch("*/*")
                            }
                        } else {
                            ImportSourceTile(
                                label = format.label,
                                icon = { ImportSourceIcon(format, Modifier.size(44.dp)) },
                                enabled = ledgerId != null && state.accounts.isNotEmpty() && !state.isImporting,
                                modifier = Modifier.weight(1f),
                            ) {
                                selectedFormat = format
                                fileLauncher.launch("*/*")
                            }
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            state.importPreview?.let { preview ->
                item {
                    Text(
                        when (preview.phase) {
                            ImportPreviewPhase.DETECTING -> "正在识别格式"
                            ImportPreviewPhase.PARSING -> "正在解析账单"
                            ImportPreviewPhase.ENRICHING -> "正在应用规则、记忆和去重"
                        ImportPreviewPhase.SUGGESTING -> "正在请求 AI 建议分类"
                            ImportPreviewPhase.READY -> "准备就绪"
                        },
                        style = OmniText.caption,
                        color = mutedContent(),
                    )
                }
            }
            if (state.isImporting) item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            state.importMessage?.let { item { Text(it, color = MaterialTheme.colorScheme.primary, style = OmniText.caption) } }
            state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error, style = OmniText.caption) } }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ImportSourceTile(
    label: String,
    icon: @Composable () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(OmniRadius.medium),
        color = surfaceCard(),
    ) {
        Column(
            Modifier.padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            icon()
            Text(label, style = OmniText.caption, maxLines = 1)
        }
    }
}

internal enum class ExportFormatChoice(val label: String, val extension: String, val mimeType: String) {
    CSV("CSV 表格", "csv", "text/csv"),
    JSON("JSON 备份", "json", "application/json"),
}

@Composable
private fun ExportPage(state: MoreUiState, viewModel: OmniFlowViewModel) {
    val context = LocalContext.current
    var payload by remember { mutableStateOf<String?>(null) }
    var formatChoice by remember { mutableStateOf(ExportFormatChoice.CSV) }
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    var ledgerScope by remember { mutableStateOf<LedgerScope>(LedgerScope.All) }
    var incremental by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf(today) }
    var endDate by remember { mutableStateOf(today) }
    var exportType by remember { mutableStateOf<TransactionType?>(null) }
    val exportDateRange = if (incremental) exportRange(startDate, endDate) else null
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(formatChoice.mimeType)) { uri ->
        if (uri != null && payload != null) {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                val content = if (formatChoice == ExportFormatChoice.CSV) {
                    exportJsonToCsv(payload!!)
                } else {
                    payload!!
                }
                stream.write(content.toByteArray(Charsets.UTF_8))
            }
        }
        payload = null
        viewModel.consumeExportPayload()
    }
    LaunchedEffect(state.exportPayload) {
        state.exportPayload?.let {
            payload = it
            launcher.launch("OmniFlow-Export-${today}.${formatChoice.extension}")
        }
    }
    LaunchedEffect(ledgerScope, incremental, startDate, endDate, exportType, state.ledgers) {
        viewModel.observeExportPreview(ledgerScope, exportDateRange, exportType)
    }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("导出格式", style = OmniText.caption, color = mutedContent())
            }
            item {
                OmniSegmented(
                    options = ExportFormatChoice.entries,
                    selected = formatChoice,
                    label = { it.label },
                    onSelected = { formatChoice = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Text("账本", style = OmniText.caption, color = mutedContent())
            }
            item {
                LedgerScopePill(
                    scope = ledgerScope,
                    ledgers = state.ledgers,
                    onScope = { ledgerScope = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Text("日期", style = OmniText.caption, color = mutedContent())
            }
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(OmniRadius.medium),
                    color = surfaceCard(),
                ) {
                    Column(
                        Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text("仅导出日期范围", style = OmniText.bodyRow)
                                Text(
                                    if (incremental) "只导出所选日期内的交易" else "默认导出全部日期",
                                    style = OmniText.caption,
                                    color = mutedContent(),
                                )
                            }
                            Switch(incremental, { incremental = it })
                        }
                        if (incremental) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ExportDateButton("开始", startDate, Modifier.weight(1f)) { startDate = it }
                                ExportDateButton("结束", endDate, Modifier.weight(1f)) { endDate = it }
                            }
                        }
                    }
                }
            }
            item {
                Text("类别", style = OmniText.caption, color = mutedContent())
            }
            item {
                OmniSegmented(
                    options = listOf<TransactionType?>(null, TransactionType.INCOME, TransactionType.EXPENSE),
                    selected = exportType,
                    label = { type ->
                        when (type) {
                            null -> "全部"
                            TransactionType.INCOME -> "收入"
                            TransactionType.EXPENSE -> "支出"
                        }
                    },
                    onSelected = { exportType = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(OmniRadius.medium),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("导出预览", style = OmniText.caption)
                            Text(
                                when {
                                    state.isLoadingExportPreview -> "正在计算…"
                                    state.exportPreviewCount != null -> "${state.exportPreviewCount} 条记录"
                                    else -> "—"
                                },
                                style = OmniText.amountPrimary,
                            )
                            Text("当前筛选条件下的交易", style = OmniText.caption)
                        }
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(32.dp))
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.exportWarnings.forEach { Text(it, color = mutedContent(), style = OmniText.caption) }
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = OmniText.bodyRow) }
                }
            }
        }
        Button(
            onClick = { viewModel.prepareExport(ledgerScope, exportDateRange, exportType) },
            enabled = !state.isExporting,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp).height(52.dp),
        ) {
            Icon(Icons.Default.FileDownload, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (state.isExporting) "导出中…" else "导出")
        }
    }
}

@Composable
internal fun ExportDateButton(label: String, date: LocalDate, modifier: Modifier, onDate: (LocalDate) -> Unit) {
    val context = LocalContext.current
    OutlinedButton(
        onClick = {
            DatePickerDialog(
                context,
                { _, year, month, day -> onDate(LocalDate(year, month + 1, day)) },
                date.year,
                date.monthNumber - 1,
                date.dayOfMonth,
            ).show()
        },
        modifier = modifier,
    ) { Text("$label ${date.monthNumber}/${date.dayOfMonth}") }
}

internal fun exportRange(first: LocalDate, second: LocalDate): DateRange {
    val start = minOf(first, second)
    val end = maxOf(first, second)
    val exclusive = java.time.LocalDate.of(end.year, end.monthNumber, end.dayOfMonth).plusDays(1)
    val zone = TimeZone.currentSystemDefault()
    return DateRange(
        start.atStartOfDayIn(zone),
        LocalDate(exclusive.year, exclusive.monthValue, exclusive.dayOfMonth).atStartOfDayIn(zone),
    )
}

internal fun exportJsonToCsv(payload: String): String {
    val root = JSONObject(payload)
    val books = mutableMapOf<String, String>()
    root.optJSONArray("bookJsonString")?.let { arr ->
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            books[obj.optString("id")] = obj.optString("name")
        }
    }
    val accounts = mutableMapOf<String, String>()
    root.optJSONArray("accountJsonString")?.let { arr ->
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            accounts[obj.optString("identifier")] = obj.optString("name")
        }
    }
    val categories = mutableMapOf<String, Pair<String, String>>()
    root.optJSONArray("categoryJsonString")?.let { arr ->
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val typeStr = if (obj.optInt("type") == 0) "收入" else "支出"
            categories[obj.optString("identifier")] = Pair(obj.optString("name"), typeStr)
        }
    }
    val marks = mutableMapOf<String, String>()
    root.optJSONArray("markJsonString")?.let { arr ->
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            marks[obj.optString("id")] = obj.optString("name")
        }
    }

    val sb = StringBuilder()
    sb.append("\uFEFF")
    sb.append("时间,账本,收支类型,分类,账户,金额,标签,备注\n")

    val entries = root.optJSONArray("entryJsonString") ?: JSONArray()
    for (i in 0 until entries.length()) {
        val entry = entries.getJSONObject(i)
        val epochSeconds = entry.optLong("createDate")
        val timeText = if (epochSeconds > 0) {
            Instant.fromEpochMilliseconds(epochSeconds * 1000).backupTimeText()
        } else ""
        val bookName = books[entry.optString("bookID")] ?: ""
        val accountName = accounts[entry.optString("accountID")] ?: ""
        val catInfo = categories[entry.optString("categoryID")]
        val catName = catInfo?.first ?: ""
        val typeName = catInfo?.second ?: "支出"
        val value = entry.optString("value")
        val markIds = entry.optString("markIDs").split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val tagsStr = markIds.mapNotNull { marks[it] }.joinToString(";")
        val note = entry.optString("content")

        sb.append(escapeCsv(timeText)).append(",")
        sb.append(escapeCsv(bookName)).append(",")
        sb.append(escapeCsv(typeName)).append(",")
        sb.append(escapeCsv(catName)).append(",")
        sb.append(escapeCsv(accountName)).append(",")
        sb.append(escapeCsv(value)).append(",")
        sb.append(escapeCsv(tagsStr)).append(",")
        sb.append(escapeCsv(note)).append("\n")
    }
    return sb.toString()
}

private fun escapeCsv(value: String): String {
    return if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
        "\"" + value.replace("\"", "\"\"") + "\""
    } else {
        value
    }
}

private fun syncLabel(state: MoreUiState): String = when (state.syncState.phase) {
    SyncPhase.IDLE -> if (state.preferences.syncTarget == null) "未配置同步" else "等待备份"
    SyncPhase.RUNNING -> "备份进行中 ${((state.syncState.progress ?: 0f) * 100).toInt()}%"
    SyncPhase.SUCCESS -> "最近备份 ${state.syncState.lastBackupAt?.backupTimeText() ?: "刚刚"}"
    SyncPhase.ERROR -> state.syncState.errorMessage ?: "备份失败"
}

private fun Context.fileName(uri: Uri): String {
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) return cursor.getString(0)
    }
    return uri.lastPathSegment ?: "import"
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
