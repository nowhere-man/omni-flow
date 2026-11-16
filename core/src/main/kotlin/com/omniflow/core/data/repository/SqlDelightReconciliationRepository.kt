package com.omniflow.core.data.repository

import com.omniflow.core.db.OmniFlowDatabase
import com.omniflow.core.domain.model.AccountRole
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.MonthlyReconciliationMeta
import com.omniflow.core.domain.model.ReconciliationEntry
import com.omniflow.core.domain.repository.ReconciliationRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class SqlDelightReconciliationRepository(
    private val database: OmniFlowDatabase,
    private val now: () -> Instant = { Clock.System.now() },
) : ReconciliationRepository {
    override suspend fun recordEntry(entry: ReconciliationEntry) {
        database.reconciliationQueries.insertEntry(
            id = entry.id,
            account_id = entry.accountId,
            period = entry.period,
            date = entry.date.toEpochMilliseconds(),
            credit_limit_minor = entry.creditLimit.minor,
            available_limit_minor = entry.availableLimit?.minor,
            debt_minor = entry.debt.minor,
            deposit_minor = entry.deposit.minor,
            investment_minor = entry.investment.minor,
            profit_loss_minor = entry.profitLoss.minor,
            owner_role = entry.ownerRole.name,
            created_at = entry.createdAt.toEpochMilliseconds(),
        )
    }

    override suspend fun entriesForPeriod(period: String): List<ReconciliationEntry> =
        database.reconciliationQueries.entriesForPeriod(period).executeAsList().map {
            ReconciliationEntry(
                id = it.id,
                accountId = it.account_id,
                period = it.period,
                date = Instant.fromEpochMilliseconds(it.date),
                creditLimit = Money(it.credit_limit_minor),
                availableLimit = it.available_limit_minor?.let(::Money),
                debt = Money(it.debt_minor),
                deposit = Money(it.deposit_minor),
                investment = Money(it.investment_minor),
                profitLoss = Money(it.profit_loss_minor),
                ownerRole = AccountRole.fromString(it.owner_role),
                createdAt = Instant.fromEpochMilliseconds(it.created_at),
            )
        }

    override suspend fun allDistinctPeriods(): List<String> =
        database.reconciliationQueries.allDistinctPeriods().executeAsList()

    override suspend fun allEntries(): List<ReconciliationEntry> =
        database.reconciliationQueries.allEntries().executeAsList().map {
            ReconciliationEntry(
                id = it.id,
                accountId = it.account_id,
                period = it.period,
                date = Instant.fromEpochMilliseconds(it.date),
                creditLimit = Money(it.credit_limit_minor),
                availableLimit = it.available_limit_minor?.let(::Money),
                debt = Money(it.debt_minor),
                deposit = Money(it.deposit_minor),
                investment = Money(it.investment_minor),
                profitLoss = Money(it.profit_loss_minor),
                ownerRole = AccountRole.fromString(it.owner_role),
                createdAt = Instant.fromEpochMilliseconds(it.created_at),
            )
        }

    override suspend fun upsertMeta(meta: MonthlyReconciliationMeta) {
        val timestamp = now().toEpochMilliseconds()
        database.reconciliationQueries.upsertMeta(
            period = meta.period,
            husband_income_minor = meta.husbandIncome.minor,
            wife_income_minor = meta.wifeIncome.minor,
            notes = meta.notes,
            created_at = timestamp,
            updated_at = timestamp,
        )
    }

    override suspend fun metaForPeriod(period: String): MonthlyReconciliationMeta? =
        database.reconciliationQueries.metaForPeriod(period).executeAsOneOrNull()?.let {
            MonthlyReconciliationMeta(
                period = it.period,
                husbandIncome = Money(it.husband_income_minor),
                wifeIncome = Money(it.wife_income_minor),
                notes = it.notes,
                updatedAt = Instant.fromEpochMilliseconds(it.updated_at),
            )
        }

    override suspend fun allMetas(): List<MonthlyReconciliationMeta> =
        database.reconciliationQueries.allMetas().executeAsList().map {
            MonthlyReconciliationMeta(
                period = it.period,
                husbandIncome = Money(it.husband_income_minor),
                wifeIncome = Money(it.wife_income_minor),
                notes = it.notes,
                updatedAt = Instant.fromEpochMilliseconds(it.updated_at),
            )
        }
}
