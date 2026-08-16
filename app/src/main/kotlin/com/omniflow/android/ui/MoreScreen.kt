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
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.omniflow.core.domain.model.AppearanceMode
import com.omniflow.core.domain.model.DateRange
import com.omniflow.core.domain.model.ImportPreviewPhase
import com.omniflow.core.domain.model.Ledger
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.SyncPhase
import com.omniflow.core.domain.model.SyncTarget
import com.omniflow.core.domain.model.ThemeColor
import com.omniflow.core.domain.model.TransactionType
import com.omniflow.core.parser.ImportFormat
import com.omniflow.android.WebDavCredentials
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal enum class MorePage(val label: String, val icon: ImageVector) {
    HOME("更多", Icons.Default.Settings),
    SETTINGS("通用设置", Icons.Default.Tune),
    DATA("数据同步", Icons.Default.CloudSync),
    IMPORT("导入账单", Icons.Default.FileUpload),
    EXPORT("导出数据", Icons.Default.FileDownload),
    RULES("规则", Icons.AutoMirrored.Filled.Rule),
    REMINDERS("提醒", Icons.Default.Notifications),
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
                Text(page.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
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
    LazyColumn(
        Modifier.readableContentWidth().fillMaxHeight().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.small,
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("净资产", style = OmniText.caption)
                    Text(state.accountSummary.netAssets.asRmb(), style = OmniText.amountHero)
                    Text("资产 ${state.accountSummary.assets.asRmb()} · 负债 ${state.accountSummary.liabilities.asRmb()}")
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(syncLabel(state), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item { MoreSection("账本与账户", listOf(MorePage.LEDGERS, MorePage.ACCOUNTS, MorePage.ASSETS), state, onPage) }
        item { MoreSection("分类与标签", listOf(MorePage.CATEGORIES, MorePage.TAGS), state, onPage) }
        item { MoreSection("自动化", listOf(MorePage.RULES, MorePage.REMINDERS), state, onPage) }
        item { MoreSection("数据", listOf(MorePage.IMPORT, MorePage.EXPORT, MorePage.DATA), state, onPage) }
        item { MoreSection("通用", listOf(MorePage.SETTINGS), state, onPage) }
        state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun MoreSection(
    title: String,
    pages: List<MorePage>,
    state: MoreUiState,
    onPage: (MorePage) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            modifier = Modifier.padding(start = 8.dp),
            color = mutedContent(),
            style = OmniText.caption,
            fontWeight = FontWeight.SemiBold,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            pages.forEachIndexed { index, page ->
                GroupedOptionRow(
                    title = page.label,
                    subtitle = page.description(state),
                    icon = page.icon,
                    shape = groupedOptionShape(index, pages.size),
                    onClick = { onPage(page) },
                    trailing = { Icon(Icons.Default.ChevronRight, contentDescription = page.label) },
                )
            }
        }
    }
}

@Composable
private fun GroupedOptionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    shape: Shape,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable (() -> Unit)? = null,
) {
    val modifier = if (onClick == null) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)
    }
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surfaceVariant, shape = shape) {
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
                Text(subtitle, style = OmniText.caption, color = mutedContent())
                content?.invoke()
            }
            trailing?.invoke()
        }
    }
}

internal fun groupedOptionShape(index: Int, size: Int): Shape = when {
    size == 1 -> RoundedCornerShape(24.dp)
    index == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
    index == size - 1 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
    else -> RoundedCornerShape(4.dp)
}

private fun MorePage.description(state: MoreUiState): String = when (this) {
    MorePage.HOME -> ""
    MorePage.DATA -> syncLabel(state)
    MorePage.IMPORT -> "从支付宝、微信等账单文件批量导入"
    MorePage.EXPORT -> "导出青子记账兼容数据"
    MorePage.SETTINGS -> "外观、主题色与应用锁"
    MorePage.LEDGERS -> "账本封面、默认账本与收支概览"
    MorePage.ACCOUNTS -> "账户余额、卡片信息与流水"
    MorePage.ASSETS -> "净资产构成与负债"
    MorePage.CATEGORIES -> "收支分类、图标与排序"
    MorePage.TAGS -> "账本标签"
    MorePage.RULES -> "自动分类和排除规则"
    MorePage.REMINDERS -> "还款与订阅提醒"
}

@Composable
private fun SettingsPage(
    state: MoreUiState,
    viewModel: OmniFlowViewModel,
    dynamicColorEnabled: Boolean,
    onDynamicColorChanged: (Boolean) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                GroupedOptionRow(
                    title = "应用锁",
                    subtitle = "启动或回到前台时验证设备凭据",
                    icon = Icons.Default.Lock,
                    shape = groupedOptionShape(0, 4),
                    trailing = { Switch(state.preferences.appLockEnabled, viewModel::setAppLockEnabled) },
                )
                GroupedOptionRow(
                    title = "界面外观",
                    subtitle = "跟随系统、浅色或深色",
                    icon = Icons.Default.DarkMode,
                    shape = groupedOptionShape(1, 4),
                    content = {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AppearanceMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = state.preferences.appearanceMode == mode,
                                    onClick = { viewModel.setAppearanceMode(mode) },
                                    modifier = Modifier.weight(1f),
                                    label = { Text(when (mode) { AppearanceMode.SYSTEM -> "系统"; AppearanceMode.LIGHT -> "浅色"; AppearanceMode.DARK -> "深色" }) },
                                )
                            }
                        }
                    },
                )
                GroupedOptionRow(
                    title = "主题色",
                    subtitle = "选择按钮和导航的全局强调色",
                    icon = Icons.Default.Palette,
                    shape = groupedOptionShape(2, 4),
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
                    subtitle = "使用 Android 系统壁纸生成 Material 3 配色（Android 12+）",
                    icon = Icons.Default.Palette,
                    shape = groupedOptionShape(3, 4),
                    trailing = { Switch(dynamicColorEnabled, onDynamicColorChanged) },
                )
            }
        }
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
    val secure = remember { context.getSharedPreferences("webdav", Context.MODE_PRIVATE) }
    var endpoint by remember { mutableStateOf(secure.getString("endpoint", "").orEmpty()) }
    var username by remember { mutableStateOf(secure.getString("username", "").orEmpty()) }
    var password by remember { mutableStateOf(WebDavCredentials.password(context)) }
    var retention by remember(state.preferences.backupRetention) { mutableStateOf(state.preferences.backupRetention.toString()) }
    var pendingRestore by remember { mutableStateOf<com.omniflow.core.domain.model.RemoteBackupMeta?>(null) }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("WebDAV 全量备份", fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(endpoint, { endpoint = it }, label = { Text("服务器目录 URL") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(username, { username = it }, label = { Text("用户名") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(password, { password = it }, label = { Text("密码") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(retention, { retention = it.filter(Char::isDigit) }, label = { Text("最大备份数") })
                    Button(onClick = {
                        WebDavCredentials.save(context, endpoint, username, password)
                        viewModel.configureSync(SyncTarget.WEBDAV, retention.toIntOrNull() ?: 10)
                    }) { Text("保存配置") }
                    LinearProgressIndicator(
                        progress = { state.syncState.progress ?: 0f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(syncLabel(state), style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = viewModel::syncNow, enabled = state.syncState.phase != SyncPhase.RUNNING) { Text("立即备份") }
                        OutlinedButton(onClick = viewModel::loadBackups) { Text("刷新备份") }
                    }
                }
            }
        }
        items(state.backups, key = { "${it.deviceId}-${it.backupId}" }) { backup ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(backup.createdAt.backupTimeText(), fontWeight = FontWeight.Medium)
                        Text(backup.backupId, style = MaterialTheme.typography.bodySmall, maxLines = 1)
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
                if (bytes != null) viewModel.importFile(ledgerId!!, context.fileName(uri), bytes, selectedFormat)
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
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ValueMenu(
                label = state.ledgers.firstOrNull { it.id == ledgerId }?.name ?: "先选择账本",
                values = state.ledgers,
                valueLabel = Ledger::name,
            ) { ledgerId = it.id; viewModel.selectMoreLedger(it.id) }
            NullableValueMenu(
                label = selectedFormat?.label ?: "自动识别来源",
                allLabel = "自动识别来源",
                values = ImportFormat.entries,
                valueLabel = { it.label },
                onAll = { selectedFormat = null },
                onSelected = { selectedFormat = it },
            )
            if (state.accounts.isEmpty()) {
                Text("请先在「账户」里创建至少一个账户，导入的交易需要落到账户上。", color = MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = { fileLauncher.launch("*/*") },
                enabled = ledgerId != null && state.accounts.isNotEmpty() && !state.isImporting,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("选择账单文件") }
            state.importPreview?.let { preview ->
                Text(
                    when (preview.phase) {
                        ImportPreviewPhase.DETECTING -> "正在识别格式"
                        ImportPreviewPhase.PARSING -> "正在解析账单"
                        ImportPreviewPhase.ENRICHING -> "正在应用规则、记忆和去重"
                        ImportPreviewPhase.READY -> "准备就绪"
                    },
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (state.isImporting) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            state.importMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun ExportPage(state: MoreUiState, viewModel: OmniFlowViewModel) {
    val context = LocalContext.current
    var payload by remember { mutableStateOf<String?>(null) }
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    var incremental by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf(today) }
    var endDate by remember { mutableStateOf(today) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) context.contentResolver.openOutputStream(uri)?.use { it.write(payload.orEmpty().encodeToByteArray()) }
        payload = null
        viewModel.consumeExportPayload()
    }
    LaunchedEffect(state.exportPayload) {
        state.exportPayload?.let {
            payload = it
            launcher.launch("OmniFlow-Qingzi.json")
        }
    }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("青子记账兼容 JSON", fontWeight = FontWeight.SemiBold)
                Text("默认导出全部有效交易，也可按日期范围增量导出。")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("仅导出日期范围", modifier = Modifier.weight(1f))
                    Switch(incremental, { incremental = it })
                }
                if (incremental) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExportDateButton("开始", startDate, Modifier.weight(1f)) { startDate = it }
                        ExportDateButton("结束", endDate, Modifier.weight(1f)) { endDate = it }
                    }
                }
                Button(
                    onClick = { viewModel.prepareQingziExport(if (incremental) exportRange(startDate, endDate) else null) },
                    enabled = !state.isExporting,
                ) {
                    Text(if (state.isExporting) "生成中…" else "生成并保存")
                }
            }
        }
        state.exportWarnings.forEach { Text(it, color = MaterialTheme.colorScheme.tertiary) }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun ExportDateButton(label: String, date: LocalDate, modifier: Modifier, onDate: (LocalDate) -> Unit) {
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

private fun exportRange(first: LocalDate, second: LocalDate): DateRange {
    val start = minOf(first, second)
    val end = maxOf(first, second)
    val exclusive = java.time.LocalDate.of(end.year, end.monthNumber, end.dayOfMonth).plusDays(1)
    val zone = TimeZone.currentSystemDefault()
    return DateRange(
        start.atStartOfDayIn(zone),
        LocalDate(exclusive.year, exclusive.monthValue, exclusive.dayOfMonth).atStartOfDayIn(zone),
    )
}

private fun syncLabel(state: MoreUiState): String = when (state.syncState.phase) {
    SyncPhase.IDLE -> if (state.preferences.syncTarget == null) "未配置同步" else "等待备份"
    SyncPhase.RUNNING -> "备份进行中 ${((state.syncState.progress ?: 0f) * 100).toInt()}%"
    SyncPhase.SUCCESS -> "最近备份 ${state.syncState.lastBackupAt ?: "刚刚"}"
    SyncPhase.ERROR -> state.syncState.errorMessage ?: "备份失败"
}

private val ImportFormat.label: String
    get() = when (this) {
        ImportFormat.ALIPAY -> "支付宝"
        ImportFormat.WECHAT -> "微信"
        ImportFormat.JD -> "京东"
        ImportFormat.MEITUAN -> "美团"
        ImportFormat.CCB -> "建设银行"
        ImportFormat.QINGZI -> "青子记账"
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
