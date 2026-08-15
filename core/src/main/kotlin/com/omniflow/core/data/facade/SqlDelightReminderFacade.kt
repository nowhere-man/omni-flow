package com.omniflow.core.data.facade

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.omniflow.core.data.repository.reminder
import com.omniflow.core.db.OmniFlowDatabase
import com.omniflow.core.domain.facade.ReminderFacade
import com.omniflow.core.domain.model.Reminder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SqlDelightReminderFacade(
    private val database: OmniFlowDatabase,
) : ReminderFacade {
    override fun observe(): Flow<Result<List<Reminder>>> = database.reminderQueries.activeReminders()
        .asFlow()
        .mapToList(Dispatchers.Default)
        .map { rows -> runCatching {
            rows.map { row ->
                reminder(
                    row.id,
                    row.type,
                    row.name,
                    row.amount_minor,
                    row.schedule_kind,
                    row.schedule_value,
                    row.paused,
                    row.deleted_at,
                )
            }
        } }
}
