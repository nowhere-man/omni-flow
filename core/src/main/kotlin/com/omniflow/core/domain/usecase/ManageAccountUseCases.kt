package com.omniflow.core.domain.usecase

import com.omniflow.core.domain.model.Account
import com.omniflow.core.domain.model.AccountId
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.repository.AccountRepository

class CreateAccountUseCase(
    private val accounts: AccountRepository,
) {
    suspend operator fun invoke(account: Account): Result<Unit> = runCatching {
        accounts.create(account)
    }
}

class UpdateAccountUseCase(
    private val accounts: AccountRepository,
) {
    suspend operator fun invoke(account: Account): Result<Unit> = runCatching {
        accounts.update(account)
    }
}

class CalibrateAccountUseCase(
    private val accounts: AccountRepository,
) {
    suspend operator fun invoke(accountId: AccountId, balance: Money): Result<Unit> = runCatching {
        accounts.calibrate(accountId, balance)
    }
}

class DeleteAccountUseCase(
    private val accounts: AccountRepository,
) {
    suspend operator fun invoke(accountId: AccountId): Result<Unit> = runCatching {
        accounts.archive(accountId)
    }
}
