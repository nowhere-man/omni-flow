package com.omniflow.android.ui.feature.more

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.MoreUiState
import com.omniflow.android.ui.OmniRadius
import com.omniflow.android.ui.OmniText
import com.omniflow.android.ui.OmniTextField
import com.omniflow.android.ui.backupTimeText
import com.omniflow.android.ui.designsystem.components.OmniBadgeIcon
import com.omniflow.android.ui.designsystem.tokens.OmniColorTokens
import com.omniflow.android.ui.designsystem.tokens.SemanticBadgeType
import com.omniflow.android.ui.mutedContent
import com.omniflow.core.domain.model.RemoteBackupMeta
import com.omniflow.core.domain.model.SyncPhase

@Composable
internal fun SyncStatusHubCard(
    state: MoreUiState,
    canBackup: Boolean,
    onSyncNow: () -> Unit,
    onLoadBackups: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OmniBadgeIcon(
                    icon = Icons.Default.CloudSync,
                    badgeType = SemanticBadgeType.SYSTEM,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        when (state.syncState.phase) {
                            SyncPhase.IDLE -> if (state.preferences.syncTarget == null) "未配置同步" else "云端备份就绪"
                            SyncPhase.RUNNING -> "正在进行云端备份…"
                            SyncPhase.SUCCESS -> "备份成功"
                            SyncPhase.ERROR -> "备份出错"
                        },
                        style = OmniText.titleRow,
                    )
                    Text(
                        "上次备份：${state.syncState.lastBackupAt?.backupTimeText() ?: "暂无备份"}",
                        style = OmniText.caption,
                        color = mutedContent(),
                    )
                }
                if (state.syncState.phase == SyncPhase.RUNNING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = OmniColorTokens.BadgeSystemContentLight,
                    )
                }
            }

            state.syncState.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = OmniText.caption)
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onSyncNow,
                    enabled = state.syncState.phase != SyncPhase.RUNNING && canBackup,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f).height(46.dp),
                ) {
                    Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("立即备份")
                }
                OutlinedButton(
                    onClick = onLoadBackups,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f).height(46.dp),
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("测试并拉取")
                }
            }
        }
    }
}

@Composable
internal fun WebDavConfigCard(
    endpoint: String,
    username: String,
    password: String,
    retention: Int,
    expanded: Boolean,
    isTesting: Boolean,
    testStatus: String?,
    onToggleExpand: () -> Unit,
    onEndpointChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRetentionChange: (Int) -> Unit,
    onSave: () -> Unit,
    onTest: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OmniBadgeIcon(
                    icon = Icons.Default.Storage,
                    badgeType = SemanticBadgeType.SYSTEM,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("WebDAV 服务配置", style = OmniText.titleRow)
                    Text(
                        if (endpoint.isNotBlank()) "已配置 · 保留 $retention 份快照" else "私有云存储全量加密快照",
                        style = OmniText.caption,
                        color = mutedContent(),
                    )
                }
                IconButton(onClick = onToggleExpand) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "折叠" else "展开",
                        tint = mutedContent(),
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OmniTextField(
                        value = endpoint,
                        onValueChange = onEndpointChange,
                        label = "服务器地址",
                        placeholder = "https://dav.jianguoyun.com/dav/",
                        supportingText = "支持坚果云、Nextcloud、Alist 等 WebDAV 服务",
                        leadingIcon = Icons.Default.CloudSync,
                    )
                    OmniTextField(
                        value = username,
                        onValueChange = onUsernameChange,
                        label = "账户用户名",
                        placeholder = "your-account@example.com",
                        leadingIcon = Icons.Default.Tune,
                    )
                    OmniTextField(
                        value = password,
                        onValueChange = onPasswordChange,
                        label = "应用授权密码",
                        placeholder = "WebDAV 专用密码或 Token",
                        isPassword = true,
                        leadingIcon = Icons.Default.Lock,
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("备份保留份数", style = OmniText.caption, color = mutedContent())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(5, 10, 20, 30).forEach { count ->
                                FilterChip(
                                    selected = retention == count,
                                    onClick = { onRetentionChange(count) },
                                    label = { Text("${count}份") },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = OmniColorTokens.BadgeSystemContainerLight,
                                        selectedLabelColor = OmniColorTokens.BadgeSystemContentLight,
                                    ),
                                )
                            }
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onSave,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f).height(46.dp),
                        ) {
                            Text("保存配置")
                        }
                        OutlinedButton(
                            onClick = onTest,
                            enabled = !isTesting && endpoint.isNotBlank(),
                            shape = RoundedCornerShape(20.dp),
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
    }
}

@Composable
internal fun CloudSnapshotRow(
    backup: RemoteBackupMeta,
    onRestore: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(OmniColorTokens.BadgeSystemContainerLight, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.CloudDone,
                    contentDescription = null,
                    tint = OmniColorTokens.BadgeSystemContentLight,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(backup.createdAt.backupTimeText(), style = OmniText.titleRow)
                Text(
                    "设备：${backup.deviceId}",
                    style = OmniText.caption,
                    color = mutedContent(),
                    maxLines = 1,
                )
            }
            OutlinedButton(
                onClick = onRestore,
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(34.dp),
            ) {
                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("恢复", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
