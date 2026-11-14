package com.omniflow.android.ui.feature.reminder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.omniflow.android.ui.designsystem.components.OmniExpressiveSwitch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.omniflow.android.ui.MoreUiState
import com.omniflow.android.ui.OmniFlowViewModel
import com.omniflow.android.ui.asRmb
import com.omniflow.android.ui.designsystem.components.OmniEmptyState
import com.omniflow.android.ui.designsystem.components.OmniSubPageScaffold
import com.omniflow.android.ui.designsystem.tokens.OmniColorTokens
import com.omniflow.android.ui.designsystem.tokens.categoryBadgeColors
import com.omniflow.android.ui.designsystem.tokens.getCategoryIcon
import com.omniflow.android.ui.expenseColor
import com.omniflow.android.ui.label
import com.omniflow.core.domain.model.AppTimeZone
import com.omniflow.core.domain.model.Reminder
import com.omniflow.core.domain.model.ReminderScheduleKind
import kotlinx.datetime.Clock
import kotlinx.datetime.toLocalDateTime

@Composable
internal fun ReminderScreen(
    state: MoreUiState,
    viewModel: OmniFlowViewModel,
    onRequestNotificationPermission: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf<Reminder?>(null) }
    var showNew by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<Reminder?>(null) }
    var pausedExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val hasNotificationPermission = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    val activeReminders = remember(state.reminders) {
        state.reminders.filter { it.deletedAt == null && !it.paused }
    }
    val pausedReminders = remember(state.reminders) {
        state.reminders.filter { it.deletedAt == null && it.paused }
    }

    val today = remember { Clock.System.now().toLocalDateTime(AppTimeZone).date }

    // Find nearest reminder for the hero countdown
    val nearestReminder = remember(activeReminders, today) {
        activeReminders.minByOrNull { reminder ->
            val day = reminder.schedule.dayOfMonth ?: 1
            if (day >= today.dayOfMonth) day - today.dayOfMonth else (day - today.dayOfMonth + 30)
        }
    }

    OmniSubPageScaffold(
        title = "提醒管理",
        onBack = onBack,
        modifier = modifier,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Android 13+ permission banner
            if (!hasNotificationPermission) {
                item(key = "permission_banner") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = OmniColorTokens.BadgePlanningContainerLight.copy(alpha = 0.25f),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "未开启通知权限，可能无法按时收到提醒",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = onRequestNotificationPermission) {
                                Text("开启权限")
                            }
                        }
                    }
                }
            }

            state.error?.let { err ->
                item {
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            if (state.reminders.isEmpty()) {
                item {
                    OmniEmptyState(
                        status = "还没有提醒",
                        value = "设置还款或订阅提醒，告别逾期与遗漏",
                        actionLabel = "新建提醒",
                        onAction = { showNew = true },
                        modifier = Modifier.padding(top = 32.dp),
                    )
                }
            } else {
                // Countdown Hero
                if (nearestReminder != null) {
                    item(key = "reminder_hero") {
                        val day = nearestReminder.schedule.dayOfMonth ?: 1
                        val daysLeft = if (day >= today.dayOfMonth) day - today.dayOfMonth else (day - today.dayOfMonth + 30)
                        ReminderCountdownHero(
                            reminder = nearestReminder,
                            daysLeft = daysLeft,
                            onClick = { editing = nearestReminder },
                        )
                    }
                }

                // Vertical timeline
                if (activeReminders.isNotEmpty()) {
                    item(key = "timeline_header") {
                        Text(
                            text = "提醒时间线",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }

                    items(activeReminders, key = { it.id }) { reminder ->
                        val day = reminder.schedule.dayOfMonth ?: 1
                        val isOverdue = reminder.schedule.kind == ReminderScheduleKind.FIXED_REPAYMENT_DAY && day < today.dayOfMonth

                        TimelineReminderItem(
                            reminder = reminder,
                            isOverdue = isOverdue,
                            onClick = { editing = reminder },
                            onToggle = { enabled ->
                                if (enabled) onRequestNotificationPermission()
                                viewModel.setReminderPaused(reminder, !enabled)
                            },
                            onDelete = { confirmDelete = reminder },
                        )
                    }
                }

                // Collapsed paused/completed zone
                if (pausedReminders.isNotEmpty()) {
                    item(key = "paused_zone") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { pausedExpanded = !pausedExpanded }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "已停用提醒 (${pausedReminders.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Icon(
                                    imageVector = if (pausedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            AnimatedVisibility(visible = pausedExpanded) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    pausedReminders.forEach { reminder ->
                                        TimelineReminderItem(
                                            reminder = reminder,
                                            isOverdue = false,
                                            isMuted = true,
                                            onClick = { editing = reminder },
                                            onToggle = { enabled ->
                                                if (enabled) onRequestNotificationPermission()
                                                viewModel.setReminderPaused(reminder, !enabled)
                                            },
                                            onDelete = { confirmDelete = reminder },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item(key = "dashed_add_reminder") {
                    DashedNewReminderCard(onClick = { showNew = true })
                }
            }
        }
    }

    if (showNew || editing != null) {
        ReminderEditSheet(
            reminder = editing,
            onDismiss = {
                showNew = false
                editing = null
            },
            onDelete = { id ->
                viewModel.deleteReminder(id)
                editing = null
            },
            onSave = { type, name, amount, schedule, paused ->
                if (!paused) onRequestNotificationPermission()
                viewModel.saveReminder(editing?.id, type, name, amount, schedule, paused)
                showNew = false
                editing = null
            },
        )
    }

    confirmDelete?.let { reminder ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("删除提醒「${reminder.name}」？") },
            text = { Text("此操作无法撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = null
                    viewModel.deleteReminder(reminder.id)
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("取消") } },
        )
    }
}
