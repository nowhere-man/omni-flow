package com.omniflow.shared.domain.usecase

import com.omniflow.shared.domain.repository.InitialDataRepository

class InitializeAppUseCase(
    private val initialData: InitialDataRepository,
) {
    suspend operator fun invoke(): Result<Unit> = runCatching {
        initialData.seedIfNeeded()
    }
}
