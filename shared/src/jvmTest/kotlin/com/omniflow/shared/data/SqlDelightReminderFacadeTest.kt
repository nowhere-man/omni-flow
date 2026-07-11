package com.omniflow.shared.data

import com.omniflow.shared.data.facade.SqlDelightReminderFacade
import com.omniflow.shared.data.local.createJvmDatabase
import com.omniflow.shared.data.repository.SqlDelightReminderRepository
import com.omniflow.shared.domain.model.Reminder
import com.omniflow.shared.domain.model.ReminderSchedule
import com.omniflow.shared.domain.model.ReminderScheduleKind
import com.omniflow.shared.domain.model.ReminderType
import com.omniflow.shared.domain.usecase.CreateReminderUseCase
import com.omniflow.shared.domain.usecase.SetReminderPausedUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

class SqlDelightReminderFacadeTest {
    @Test
    fun createsAndPausesIndependentReminder() = runBlocking {
        val database = createJvmDatabase()
        val repository = SqlDelightReminderRepository(database)
        val reminder = Reminder(
            id = "reminder",
            type = ReminderType.SUBSCRIPTION,
            name = "会员",
            amount = null,
            schedule = ReminderSchedule(ReminderScheduleKind.MONTHLY, dayOfMonth = 10),
        )
        CreateReminderUseCase(repository)(reminder).getOrThrow()
        SetReminderPausedUseCase(repository)(reminder, true).getOrThrow()

        assertTrue(SqlDelightReminderFacade(database).observe().first().getOrThrow().single().paused)
    }
}
