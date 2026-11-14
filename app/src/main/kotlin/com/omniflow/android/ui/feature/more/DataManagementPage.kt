package com.omniflow.android.ui.feature.more

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.omniflow.android.WebDavCredentials
import com.omniflow.android.WebDavSyncAdapter
import com.omniflow.android.ui.MoreUiState
import com.omniflow.android.ui.OmniFlowViewModel
import com.omniflow.android.ui.OmniText
import com.omniflow.android.ui.designsystem.components.OmniEmptyState
import com.omniflow.android.ui.designsystem.components.OmniSubPageScaffold
import com.omniflow.android.ui.mutedContent
import com.omniflow.core.domain.model.RemoteBackupMeta
import com.omniflow.core.domain.model.SyncTarget
import kotlinx.coroutines.launch

/**
 * 数据与同步页：缺陷 8.1 根治版本。
 * 遵循 Group 1 SYSTEM 语义色锁定规范，严禁出现 DATA_SYNC 蓝。
 * 包含 SyncStatusHubCard 状态中枢、渐进折叠 WebDAV 配置与云端快照时间轴。
 */
@Composable
fun DataManagementPage(
    state: MoreUiState,
    viewModel: OmniFlowViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val secure = remember { context.getSharedPreferences("webdav", Context.MODE_PRIVATE) }
    var endpoint by remember { mutableStateOf(secure.getString("endpoint", "").orEmpty()) }
    var username by remember { mutableStateOf(secure.getString("username", "").orEmpty()) }
    var password by remember { mutableStateOf(WebDavCredentials.password(context)) }
    var retention by remember(state.preferences.backupRetention) { mutableStateOf(state.preferences.backupRetention) }
    var pendingRestore by remember { mutableStateOf<RemoteBackupMeta?>(null) }
    var testStatus by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    var configExpanded by remember { mutableStateOf(endpoint.isBlank()) }

    OmniSubPageScaffold(
        title = "数据与同步",
        onBack = onBack,
        modifier = modifier,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1. SyncStatusHubCard 状态中枢
            item {
                SyncStatusHubCard(
                    state = state,
                    canBackup = endpoint.isNotBlank(),
                    onSyncNow = viewModel::syncNow,
                    onLoadBackups = viewModel::loadBackups,
                )
            }

            // 2. WebDAV 渐进折叠配置卡
            item {
                WebDavConfigCard(
                    endpoint = endpoint,
                    username = username,
                    password = password,
                    retention = retention,
                    expanded = configExpanded,
                    isTesting = isTesting,
                    testStatus = testStatus,
                    onToggleExpand = { configExpanded = !configExpanded },
                    onEndpointChange = { endpoint = it; testStatus = null },
                    onUsernameChange = { username = it; testStatus = null },
                    onPasswordChange = { password = it; testStatus = null },
                    onRetentionChange = { retention = it },
                    onSave = {
                        WebDavCredentials.save(context, endpoint, username, password)
                        viewModel.configureSync(SyncTarget.WEBDAV, retention)
                        testStatus = "配置已保存"
                        configExpanded = false
                    },
                    onTest = {
                        scope.launch {
                            isTesting = true
                            testStatus = "正在测试连接…"
                            val res = WebDavSyncAdapter.testConnection(endpoint, username, password)
                            testStatus = if (res.isSuccess) "连接成功！服务正常" else "连接失败: ${res.exceptionOrNull()?.message ?: "未知错误"}"
                            isTesting = false
                        }
                    },
                )
            }

            // 3. 云端历史快照时间轴
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "云端历史快照",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = viewModel::loadBackups,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "刷新快照",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            if (state.backups.isEmpty()) {
                item {
                    OmniEmptyState(
                        icon = Icons.Default.CloudQueue,
                        message = "暂无云端快照",
                        subtitle = if (state.syncState.lastBackupAt == null) {
                            "配置 WebDAV 后点击「立即备份」创建第一个云端快照"
                        } else {
                            "点击右上角刷新拉取云端快照"
                        },
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
            } else {
                items(state.backups, key = { "${it.deviceId}-${it.backupId}" }) { backup ->
                    CloudSnapshotRow(
                        backup = backup,
                        onRestore = { pendingRestore = backup },
                    )
                }
            }

            state.error?.let {
                item {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = OmniText.caption,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    pendingRestore?.let { backup ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("完整恢复备份？") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("恢复会完整替换本机全部数据，不与当前数据合并。")
                    Text("建议在恢复前先备份当前数据，避免数据意外丢失。", style = OmniText.caption, color = mutedContent())
                    OutlinedButton(
                        onClick = { viewModel.syncNow() },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("先备份当前数据")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.restoreBackup(backup)
                        pendingRestore = null
                    },
                ) {
                    Text("确认恢复", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestore = null }) {
                    Text("取消")
                }
            },
        )
    }
}
