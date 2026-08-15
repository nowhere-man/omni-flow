package com.omniflow.core.domain.usecase

import com.omniflow.core.domain.repository.InitialDataRepository

class InitializeAppUseCase(
    private val initialData: InitialDataRepository,
) {
    suspend operator fun invoke(): Result<Unit> = runCatching {
        initialData.seedIfNeeded()
    }
}
