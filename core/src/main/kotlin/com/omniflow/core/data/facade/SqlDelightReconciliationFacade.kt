package com.omniflow.core.data.facade

import com.omniflow.core.domain.facade.ReconciliationFacade
import com.omniflow.core.domain.model.AccountId
import com.omniflow.core.domain.model.AccountRole
import com.omniflow.core.domain.model.AccountType
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.MonthlyReconciliationMeta
import com.omniflow.core.domain.model.PeriodReconciliationSnapshot
import com.omniflow.core.domain.model.ReconciliationAnnualReport
import com.omniflow.core.domain.model.ReconciliationEntry
import com.omniflow.core.domain.repository.AccountRepository
import com.omniflow.core.domain.repository.ReconciliationRepository
import com.omniflow.core.domain.util.UuidGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class SqlDelightReconciliationFacade(
    private val reconciliationRepository: ReconciliationRepository,
    private val accountRepository: AccountRepository,
    private val ids: UuidGenerator = UuidGenerator(),
    private val now: () -> Instant = { Clock.System.now() },
) : ReconciliationFacade {

    override fun observeAnnualReport(year: Int, role: AccountRole?): Flow<Result<ReconciliationAnnualReport>> = flow {
        val result = runCatching {
            val allEntries = reconciliationRepository.allEntries()
            val allMetas = reconciliationRepository.allMetas().associateBy { it.period }
            val snapshots = ReconciliationCalculator.computeChronologicalSnapshots(
                allEntries = allEntries,
                allMetas = allMetas,
                role = role,
                now = now,
            )
            ReconciliationCalculator.buildAnnualReport(
                year = year,
                role = role,
                periodSnapshots = snapshots,
                now = now,
            )
        }
        emit(result)
    }

    override fun observePeriodSnapshot(period: String, role: AccountRole?): Flow<Result<PeriodReconciliationSnapshot?>> = flow {
        val result = runCatching {
            val allEntries = reconciliationRepository.allEntries()
            val allMetas = reconciliationRepository.allMetas().associateBy { it.period }
            val snapshots = ReconciliationCalculator.computeChronologicalSnapshots(
                allEntries = allEntries,
                allMetas = allMetas,
                role = role,
                now = now,
            )
            snapshots[period] ?: ReconciliationCalculator.emptySnapshot(period, now)
        }
        emit(result)
    }

    override fun observeAllPeriods(): Flow<List<String>> = flow {
        val periods = reconciliationRepository.allDistinctPeriods()
        emit(periods)
    }

    override suspend fun recordAccountUpdate(
        accountId: AccountId,
        period: String,
        balance: Money,
        availableLimit: Money?,
        profitLoss: Money,
    ): Result<Unit> = runCatching {
        val account = accountRepository.activeAccounts().find { it.id == accountId }
            ?: error("账户不存在")

        // 1. Update live account
        accountRepository.quickUpdateFinance(
            accountId = accountId,
            balance = balance,
            availableLimit = availableLimit,
            profitLoss = profitLoss,
        )

        val updatedAccount = account.copy(
            balance = balance,
            availableLimit = availableLimit,
            profitLoss = profitLoss,
        )

        // 2. A6: Unified debt via updatedAccount.debt
        val debt = updatedAccount.debt

        val deposit = if (account.type != AccountType.CREDIT_CARD && account.type != AccountType.INVESTMENT && balance.minor > 0) {
            balance
        } else Money.Zero

        val investment = if (account.type == AccountType.INVESTMENT && balance.minor > 0) {
            balance
        } else Money.Zero

        val entry = ReconciliationEntry(
            id = ids.next(),
            accountId = accountId,
            period = period,
            date = now(),
            creditLimit = account.creditLimit,
            availableLimit = availableLimit,
            debt = debt,
            deposit = deposit,
            investment = investment,
            profitLoss = profitLoss,
            ownerRole = account.ownerRole,
            createdAt = now(),
        )

        reconciliationRepository.recordEntry(entry)
    }

    override suspend fun updateMonthlyMeta(
        period: String,
        husbandIncome: Money,
        wifeIncome: Money,
        notes: String,
    ): Result<Unit> = runCatching {
        val meta = MonthlyReconciliationMeta(
            period = period,
            husbandIncome = husbandIncome,
            wifeIncome = wifeIncome,
            notes = notes,
            updatedAt = now(),
        )
        reconciliationRepository.upsertMeta(meta)
    }

    // A3: Atomic update of notes without overwriting husband/wife income
    override suspend fun updateMonthlyNotes(
        period: String,
        notes: String,
    ): Result<Unit> = runCatching {
        val existing = reconciliationRepository.metaForPeriod(period)
        val meta = if (existing != null) {
            existing.copy(notes = notes, updatedAt = now())
        } else {
            MonthlyReconciliationMeta(
                period = period,
                husbandIncome = Money.Zero,
                wifeIncome = Money.Zero,
                notes = notes,
                updatedAt = now(),
            )
        }
        reconciliationRepository.upsertMeta(meta)
    }
}
