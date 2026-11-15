package com.omniflow.android.ui.feature.reminder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.ValueMenu
import com.omniflow.android.ui.designsystem.components.OmniSheetScaffold
import com.omniflow.android.ui.designsystem.tokens.OmniShapeTokens
import com.omniflow.android.ui.label
import com.omniflow.android.ui.toDecimal
import com.omniflow.android.ui.toMoneyOrNull
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.Reminder
import com.omniflow.core.domain.model.ReminderSchedule
import com.omniflow.core.domain.model.ReminderScheduleKind
import com.omniflow.core.domain.model.ReminderType

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun ReminderEditSheet(
    reminder: Reminder?,
    onDismiss: () -> Unit,
    onDelete: ((String) -> Unit)? = null,
    onSave: (ReminderType, String, Money?, ReminderSchedule, Boolean) -> Unit,
) {
    var type by remember(reminder) { mutableStateOf(reminder?.type ?: ReminderType.REPAYMENT) }
    var name by remember(reminder) { mutableStateOf(reminder?.name.orEmpty()) }
    var amountText by remember(reminder) { mutableStateOf(reminder?.amount?.toDecimal().orEmpty()) }
    var kind by remember(reminder) {
        mutableStateOf(reminder?.schedule?.kind ?: ReminderScheduleKind.FIXED_REPAYMENT_DAY)
    }
    var day by remember(reminder) { mutableStateOf((reminder?.schedule?.dayOfMonth ?: 1).toString()) }
    var daysAfter by remember(reminder) { mutableStateOf((reminder?.schedule?.daysAfter ?: 0).toString()) }
    var weekday by remember(reminder) { mutableStateOf((reminder?.schedule?.dayOfWeek ?: 1).toString()) }
    var month by remember(reminder) { mutableStateOf((reminder?.schedule?.month ?: 1).toString()) }
    var paused by remember(reminder) { mutableStateOf(reminder?.paused ?: false) }

    val kinds = if (type == ReminderType.REPAYMENT) {
        listOf(ReminderScheduleKind.FIXED_REPAYMENT_DAY, ReminderScheduleKind.DAYS_AFTER_STATEMENT)
    } else {
        listOf(
            ReminderScheduleKind.DAILY,
            ReminderScheduleKind.WEEKLY,
            ReminderScheduleKind.MONTHLY,
            ReminderScheduleKind.YEARLY,
        )
    }

    OmniSheetScaffold(
        title = if (reminder == null) "新建提醒" else "编辑提醒",
        onDismiss = onDismiss,
        saveText = "保存",
        saveEnabled = name.isNotBlank(),
        onSave = {
            val schedule = when (kind) {
                ReminderScheduleKind.FIXED_REPAYMENT_DAY,
                ReminderScheduleKind.MONTHLY -> ReminderSchedule(
                    kind = kind,
                    dayOfMonth = day.toIntOrNull()?.coerceIn(1, 31) ?: 1,
                )
                ReminderScheduleKind.DAYS_AFTER_STATEMENT -> ReminderSchedule(
                    kind = kind,
                    dayOfMonth = day.toIntOrNull()?.coerceIn(1, 31) ?: 1,
                    daysAfter = daysAfter.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                )
                ReminderScheduleKind.WEEKLY -> ReminderSchedule(
                    kind = kind,
                    dayOfWeek = weekday.toIntOrNull()?.coerceIn(1, 7) ?: 1,
                )
                ReminderScheduleKind.YEARLY -> ReminderSchedule(
                    kind = kind,
                    month = month.toIntOrNull()?.coerceIn(1, 12) ?: 1,
                    dayOfMonth = day.toIntOrNull()?.coerceIn(1, 31) ?: 1,
                )
                ReminderScheduleKind.DAILY -> ReminderSchedule(kind = kind)
            }
            onSave(type, name.trim(), amountText.toMoneyOrNull(), schedule, paused)
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Type switch
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = type == ReminderType.REPAYMENT,
                    onClick = {
                        type = ReminderType.REPAYMENT
                        kind = ReminderScheduleKind.FIXED_REPAYMENT_DAY
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    label = { Text("还款提醒") },
                )
                SegmentedButton(
                    selected = type == ReminderType.SUBSCRIPTION,
                    onClick = {
                        type = ReminderType.SUBSCRIPTION
                        kind = ReminderScheduleKind.MONTHLY
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    label = { Text("订阅提醒") },
                )
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("提醒名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = OmniShapeTokens.mediumSmall,
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("预估金额（可选）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = OmniShapeTokens.mediumSmall,
            )

            ValueMenu(
                label = "频率：${kind.label}",
                values = kinds,
                valueLabel = ReminderScheduleKind::label,
                onSelected = { kind = it },
            )

            when (kind) {
                ReminderScheduleKind.FIXED_REPAYMENT_DAY,
                ReminderScheduleKind.MONTHLY -> {
                    OutlinedTextField(
                        value = day,
                        onValueChange = { day = it },
                        label = { Text("每月几号（1-31）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = OmniShapeTokens.mediumSmall,
                    )
                }
                ReminderScheduleKind.DAYS_AFTER_STATEMENT -> {
                    OutlinedTextField(
                        value = day,
                        onValueChange = { day = it },
                        label = { Text("账单日（1-31）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = OmniShapeTokens.mediumSmall,
                    )
                    OutlinedTextField(
                        value = daysAfter,
                        onValueChange = { daysAfter = it },
                        label = { Text("账单日后几天") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = OmniShapeTokens.mediumSmall,
                    )
                }
                ReminderScheduleKind.WEEKLY -> {
                    OutlinedTextField(
                        value = weekday,
                        onValueChange = { weekday = it },
                        label = { Text("周几（1=周一 … 7=周日）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = OmniShapeTokens.mediumSmall,
                    )
                }
                ReminderScheduleKind.YEARLY -> {
                    OutlinedTextField(
                        value = month,
                        onValueChange = { month = it },
                        label = { Text("几月（1-12）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = OmniShapeTokens.mediumSmall,
                    )
                    OutlinedTextField(
                        value = day,
                        onValueChange = { day = it },
                        label = { Text("几号（1-31）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = OmniShapeTokens.mediumSmall,
                    )
                }
                ReminderScheduleKind.DAILY -> Unit
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("启用此提醒", style = MaterialTheme.typography.bodyLarge)
                OmniExpressiveSwitch(checked = !paused, onCheckedChange = { paused = !it })
            }

            if (reminder != null && onDelete != null) {
                TextButton(
                    onClick = { onDelete(reminder.id) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("删除提醒", color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

