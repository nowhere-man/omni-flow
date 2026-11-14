package com.omniflow.android.ui.feature.more

import android.content.Context
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.omniflow.android.WebDavCredentials
import com.omniflow.android.WebDavSyncAdapter
import com.omniflow.android.ui.MoreUiState
import com.omniflow.android.ui.OmniFlowViewModel
import com.omniflow.android.ui.OmniRadius
import com.omniflow.android.ui.OmniText
import com.omniflow.android.ui.OmniTextField
import com.omniflow.android.ui.backupTimeText
import com.omniflow.android.ui.designsystem.components.OmniBadgeIcon
import com.omniflow.android.ui.designsystem.tokens.SemanticBadgeType
import com.omniflow.android.ui.mutedContent
import com.omniflow.android.ui.surfaceInset
import com.omniflow.core.domain.model.RemoteBackupMeta
import com.omniflow.core.domain.model.SyncPhase
import com.omniflow.core.domain.model.SyncTarget
import kotlinx.coroutines.launch

@Composable
fun DataManagementPage(
    state: MoreUiState,
    viewModel: OmniFlowViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val secure = remember { context.getSharedPreferences("webdav", Context.MODE_PRIVATE) }
    var endpoint by remember { mutableStateOf(secure.getString("endpoint", "").orEmpty()) }
    var username by remember { mutableStateOf(secure.getString("username", "").orEmpty()) }
    var password by remember { mutableStateOf(WebDavCredentials.password(context)) }
    var retention by remember(state.preferences.backupRetention) { mutableStateOf(state.preferences.backupRetention.toString()) }
    var pendingRestore by remember { mutableStateOf<RemoteBackupMeta?>(null) }
    var testStatus by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 卡片 1：WebDAV 服务配置
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.5.dp),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OmniBadgeIcon(
                            icon = Icons.Default.CloudSync,
                            badgeType = SemanticBadgeType.DATA_SYNC,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("WebDAV 备份配置", style = OmniText.titleRow)
                            Text("私有云存储全量加密快照", style = OmniText.caption, color = mutedContent())
                        }
                    }

                    OmniTextField(
                        value = endpoint,
                        onValueChange = { endpoint = it; testStatus = null },
                        label = "服务器地址",
                        placeholder = "https://dav.jianguoyun.com/dav/",
                        supportingText = "支持坚果云、Nextcloud、Alist 等 WebDAV 服务",
                        leadingIcon = Icons.Default.CloudSync,
                    )
                    OmniTextField(
                        value = username,
                        onValueChange = { username = it; testStatus = null },
                        label = "账户用户名",
                        placeholder = "your-account@example.com",
                        leadingIcon = Icons.Default.Tune,
                    )
                    OmniTextField(
                        value = password,
                        onValueChange = { password = it; testStatus = null },
                        label = "应用授权密码",
                        placeholder = "WebDAV 专用密码或 Token",
                        isPassword = true,
                        leadingIcon = Icons.Default.Lock,
                    )
                    OmniTextField(
                        value = retention,
                        onValueChange = { retention = it.filter(Char::isDigit) },
                        label = "最大备份保留份数",
                        placeholder = "10",
                        supportingText = "超出数量将自动轮转清理最早的历史备份",
                        leadingIcon = Icons.Default.Storage,
                    )

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                WebDavCredentials.save(context, endpoint, username, password)
                                viewModel.configureSync(SyncTarget.WEBDAV, retention.toIntOrNull() ?: 10)
                                testStatus = "配置已保存"
                            },
                            shape = RoundedCornerShape(OmniRadius.medium),
                            modifier = Modifier.weight(1f).height(46.dp),
                        ) { Text("保存配置") }
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    isTesting = true
                                    testStatus = "正在测试连接…"
                                    val res = WebDavSyncAdapter.testConnection(endpoint, username, password)
                                    testStatus = if (res.isSuccess) "连接成功！服务正常" else "连接失败: ${res.exceptionOrNull()?.message ?: "未知错误"}"
                                    isTesting = false
                                }
                            },
                            enabled = !isTesting && endpoint.isNotBlank(),
                            shape = RoundedCornerShape(OmniRadius.medium),
                            modifier = Modifier.weight(1f).height(46.dp),
                        ) {
                            Text(if (isTesting) "测试中…" else "测试连接")
                        }
                    }

                    testStatus?.let { status ->
                        Surface(
                            shape = RoundedCornerShape(OmniRadius.small),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                status,
                                style = OmniText.caption,
                                modifier = Modifier.padding(10.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        // 卡片 2：备份与同步控制
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.5.dp),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OmniBadgeIcon(
                            icon = Icons.Default.Storage,
                            badgeType = SemanticBadgeType.DATA_SYNC,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("备份控制", style = OmniText.titleRow)
                            Text(
                                when (state.syncState.phase) {
                                    SyncPhase.IDLE -> if (state.preferences.syncTarget == null) "未配置同步" else "就绪"
                                    SyncPhase.RUNNING -> "正在进行云端备份…"
                                    SyncPhase.SUCCESS -> "上次备份成功"
                                    SyncPhase.ERROR -> "备份出错"
                                },
                                style = OmniText.caption,
                                color = mutedContent(),
                            )
                        }
                    }
                    state.syncState.errorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = OmniText.caption)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = viewModel::syncNow,
                            enabled = state.syncState.phase != SyncPhase.RUNNING && endpoint.isNotBlank(),
                            shape = RoundedCornerShape(OmniRadius.medium),
                            modifier = Modifier.weight(1f).height(46.dp),
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("立即备份")
                        }
                        OutlinedButton(
                            onClick = viewModel::loadBackups,
                            shape = RoundedCornerShape(OmniRadius.medium),
                            modifier = Modifier.weight(1f).height(46.dp),
                        ) {
                            Text("刷新列表")
                        }
                    }
                }
            }
        }

        item {
            Text("云端历史快照", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
        }

        if (state.backups.isEmpty()) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.5.dp),
                ) {
                    Text(
                        if (state.syncState.lastBackupAt == null) "配置 WebDAV 后点击「立即备份」创建第一个云端快照" else "未拉取到云端备份，请点击「刷新列表」",
                        style = OmniText.caption,
                        color = mutedContent(),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 16.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        items(state.backups, key = { "${it.deviceId}-${it.backupId}" }) { backup ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.5.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OmniBadgeIcon(
                        icon = Icons.Default.Storage,
                        badgeType = SemanticBadgeType.DATA_SYNC,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(backup.createdAt.backupTimeText(), style = OmniText.titleRow)
                        Text(backup.deviceId, style = OmniText.caption, color = mutedContent(), maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { pendingRestore = backup },
                        shape = RoundedCornerShape(OmniRadius.small),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp),
                    ) { Text("恢复") }
                }
            }
        }

        state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        item { Spacer(Modifier.height(32.dp)) }
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
