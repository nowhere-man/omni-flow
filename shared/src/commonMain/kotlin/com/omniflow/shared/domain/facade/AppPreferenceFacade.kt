package com.omniflow.shared.domain.facade

import com.omniflow.shared.domain.model.AppPreferences
import kotlinx.coroutines.flow.Flow

interface AppPreferenceFacade {
    fun observe(): Flow<Result<AppPreferences>>
    suspend fun save(preferences: AppPreferences): Result<Unit>
}
