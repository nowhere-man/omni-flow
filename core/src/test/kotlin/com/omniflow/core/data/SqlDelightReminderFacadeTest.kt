package com.omniflow.core.data

import com.omniflow.core.data.facade.SqlDelightReminderFacade
import com.omniflow.core.data.local.createJvmDatabase
import com.omniflow.core.data.repository.SqlDelightReminderRepository
import com.omniflow.core.domain.model.Reminder
import com.omniflow.core.domain.model.ReminderSchedule
import com.omniflow.core.domain.model.ReminderScheduleKind
import com.omniflow.core.domain.model.ReminderType
import com.omniflow.core.domain.usecase.CreateReminderUseCase
import com.omniflow.core.domain.usecase.SetReminderPausedUseCase
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
