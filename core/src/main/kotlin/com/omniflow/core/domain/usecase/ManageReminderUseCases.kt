package com.omniflow.core.domain.usecase

import com.omniflow.core.domain.model.Reminder
import com.omniflow.core.domain.model.ReminderId
import com.omniflow.core.domain.repository.ReminderRepository

class CreateReminderUseCase(private val reminders: ReminderRepository) {
    suspend operator fun invoke(reminder: Reminder): Result<Unit> = runCatching { reminders.create(reminder) }
}

class UpdateReminderUseCase(private val reminders: ReminderRepository) {
    suspend operator fun invoke(reminder: Reminder): Result<Unit> = runCatching { reminders.update(reminder) }
}

class SetReminderPausedUseCase(private val reminders: ReminderRepository) {
    suspend operator fun invoke(reminder: Reminder, paused: Boolean): Result<Unit> = runCatching {
        reminders.update(reminder.copy(paused = paused))
    }
}

class DeleteReminderUseCase(private val reminders: ReminderRepository) {
    suspend operator fun invoke(reminderId: ReminderId): Result<Unit> = runCatching { reminders.archive(reminderId) }
}
