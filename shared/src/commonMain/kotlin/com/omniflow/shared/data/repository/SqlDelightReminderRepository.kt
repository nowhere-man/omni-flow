package com.omniflow.shared.data.repository

import com.omniflow.shared.db.OmniFlowDatabase
import com.omniflow.shared.domain.model.Money
import com.omniflow.shared.domain.model.Reminder
import com.omniflow.shared.domain.model.ReminderId
import com.omniflow.shared.domain.model.ReminderSchedule
import com.omniflow.shared.domain.model.ReminderScheduleKind
import com.omniflow.shared.domain.model.ReminderType
import com.omniflow.shared.domain.repository.ReminderRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class SqlDelightReminderRepository(
    private val database: OmniFlowDatabase,
    private val now: () -> Instant = { Clock.System.now() },
) : ReminderRepository {
    override suspend fun activeReminders(): List<Reminder> = database.reminderQueries.activeReminders()
        .executeAsList()
        .map { row -> reminder(row.id, row.type, row.name, row.amount_minor, row.schedule_kind, row.schedule_value, row.paused, row.deleted_at) }

    override suspend fun create(reminder: Reminder) {
        val timestamp = now().toEpochMilliseconds()
        database.reminderQueries.insertReminder(
            id = reminder.id,
            type = reminder.type.name,
            name = reminder.name.trim(),
            amount_minor = reminder.amount?.minor,
            schedule_kind = reminder.schedule.kind.name,
            schedule_value = reminder.schedule.encode(),
            paused = if (reminder.paused) 1L else 0L,
            created_at = timestamp,
            updated_at = timestamp,
        )
    }

    override suspend fun update(reminder: Reminder) {
        require(database.reminderQueries.activeReminderId(reminder.id).executeAsOneOrNull() != null) {
            "提醒不存在或已删除"
        }
        database.reminderQueries.updateReminder(
            type = reminder.type.name,
            name = reminder.name.trim(),
            amount_minor = reminder.amount?.minor,
            schedule_kind = reminder.schedule.kind.name,
            schedule_value = reminder.schedule.encode(),
            paused = if (reminder.paused) 1L else 0L,
            updated_at = now().toEpochMilliseconds(),
            id = reminder.id,
        )
    }

    override suspend fun archive(reminderId: ReminderId) {
        require(database.reminderQueries.activeReminderId(reminderId).executeAsOneOrNull() != null) {
            "提醒不存在或已删除"
        }
        database.reminderQueries.archiveReminder(now().toEpochMilliseconds(), reminderId)
    }
}

internal fun reminder(
    id: String,
    type: String,
    name: String,
    amountMinor: Long?,
    scheduleKind: String,
    scheduleValue: String,
    paused: Long,
    deletedAt: Long?,
) = Reminder(
    id = id,
    type = ReminderType.valueOf(type),
    name = name,
    amount = amountMinor?.let(::Money),
    schedule = decodeSchedule(ReminderScheduleKind.valueOf(scheduleKind), scheduleValue),
    paused = paused != 0L,
    deletedAt = deletedAt?.let(Instant::fromEpochMilliseconds),
)

private fun ReminderSchedule.encode(): String = listOf(dayOfMonth, daysAfter, dayOfWeek, month)
    .joinToString(",") { it?.toString().orEmpty() }

private fun decodeSchedule(kind: ReminderScheduleKind, value: String): ReminderSchedule {
    val parts = value.split(',')
    return ReminderSchedule(
        kind = kind,
        dayOfMonth = parts.getOrNull(0)?.toIntOrNull(),
        daysAfter = parts.getOrNull(1)?.toIntOrNull(),
        dayOfWeek = parts.getOrNull(2)?.toIntOrNull(),
        month = parts.getOrNull(3)?.toIntOrNull(),
    )
}
