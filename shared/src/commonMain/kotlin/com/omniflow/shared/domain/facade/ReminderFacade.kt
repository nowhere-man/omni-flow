package com.omniflow.shared.domain.facade

import com.omniflow.shared.domain.model.Reminder
import kotlinx.coroutines.flow.Flow

interface ReminderFacade {
    fun observe(): Flow<Result<List<Reminder>>>
}
