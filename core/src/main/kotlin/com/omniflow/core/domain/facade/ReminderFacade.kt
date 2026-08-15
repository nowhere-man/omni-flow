package com.omniflow.core.domain.facade

import com.omniflow.core.domain.model.Reminder
import kotlinx.coroutines.flow.Flow

interface ReminderFacade {
    fun observe(): Flow<Result<List<Reminder>>>
}
