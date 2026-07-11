package com.omniflow.shared.domain.usecase

import com.omniflow.shared.domain.model.Ledger
import com.omniflow.shared.domain.model.LedgerId
import com.omniflow.shared.domain.repository.LedgerRepository

class CreateLedgerUseCase(
    private val ledgers: LedgerRepository,
) {
    suspend operator fun invoke(ledger: Ledger): Result<Unit> = runCatching {
        ledgers.create(ledger)
    }
}

class UpdateLedgerUseCase(
    private val ledgers: LedgerRepository,
) {
    suspend operator fun invoke(ledger: Ledger): Result<Unit> = runCatching {
        ledgers.update(ledger)
    }
}

class DeleteLedgerUseCase(
    private val ledgers: LedgerRepository,
) {
    suspend operator fun invoke(ledgerId: LedgerId): Result<Unit> = runCatching {
        ledgers.archive(ledgerId)
    }
}

class SetDefaultLedgerUseCase(
    private val ledgers: LedgerRepository,
) {
    suspend operator fun invoke(ledgerId: LedgerId?): Result<Unit> = runCatching {
        ledgers.setDefaultLedgerId(ledgerId)
    }
}
