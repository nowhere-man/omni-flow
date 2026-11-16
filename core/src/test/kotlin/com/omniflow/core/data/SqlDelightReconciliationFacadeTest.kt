package com.omniflow.core.data

import com.omniflow.core.data.facade.SqlDelightReconciliationFacade
import com.omniflow.core.data.local.createJvmDatabase
import com.omniflow.core.data.repository.SqlDelightAccountRepository
import com.omniflow.core.data.repository.SqlDelightInitialDataRepository
import com.omniflow.core.data.repository.SqlDelightReconciliationRepository
import com.omniflow.core.domain.model.AccountRole
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.MonthlyReconciliationMeta
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SqlDelightReconciliationFacadeTest {

    @Test
    fun testAnnualReportComputationFromSeededData() = runBlocking {
        val database = createJvmDatabase()
        val initialRepo = SqlDelightInitialDataRepository(database) { Instant.fromEpochMilliseconds(1_000) }
        initialRepo.seedIfNeeded()

        val accountRepo = SqlDelightAccountRepository(database) { Instant.fromEpochMilliseconds(1_000) }
        val reconciliationRepo = SqlDelightReconciliationRepository(database) { Instant.fromEpochMilliseconds(1_000) }
        val facade = SqlDelightReconciliationFacade(reconciliationRepo, accountRepo)

        // Seed fictional test data for 2025
        for (month in 1..12) {
            val period = "2025-%02d".format(month)
            reconciliationRepo.upsertMeta(
                MonthlyReconciliationMeta(
                    period = period,
                    husbandIncome = Money(20_000_00),
                    wifeIncome = Money(20_000_00),
                    notes = "示例备忘",
                    updatedAt = Instant.fromEpochMilliseconds(1_000),
                )
            )
        }

        val reportResult = facade.observeAnnualReport(2025, null).first()
        assertTrue(reportResult.isSuccess)

        val report = reportResult.getOrThrow()
        assertEquals(2025, report.year)
        assertEquals(12, report.monthlySnapshots.size)

        // Verify total credit limits across accounts
        val totalCredits = accountRepo.totalCreditLimit(null)
        assertTrue(totalCredits.minor > 0)

        // Verify role filtering
        val husbandReport = facade.observeAnnualReport(2025, AccountRole.HUSBAND).first().getOrThrow()
        val wifeReport = facade.observeAnnualReport(2025, AccountRole.WIFE).first().getOrThrow()

        assertNotNull(husbandReport)
        assertNotNull(wifeReport)
        assertTrue(husbandReport.totalAnnualIncome.minor > 0)
        assertTrue(wifeReport.totalAnnualIncome.minor > 0)
    }

    @Test
    fun testQuickUpdateFinanceRecordsAuditEntry() = runBlocking {
        val database = createJvmDatabase()
        val initialRepo = SqlDelightInitialDataRepository(database) { Instant.fromEpochMilliseconds(1_000) }
        initialRepo.seedIfNeeded()

        val accountRepo = SqlDelightAccountRepository(database) { Instant.fromEpochMilliseconds(1_000) }
        val reconciliationRepo = SqlDelightReconciliationRepository(database) { Instant.fromEpochMilliseconds(1_000) }
        val facade = SqlDelightReconciliationFacade(reconciliationRepo, accountRepo)

        val firstAccount = accountRepo.activeAccounts().first()
        val result = facade.recordAccountUpdate(
            accountId = firstAccount.id,
            period = "2026-11",
            balance = com.omniflow.core.domain.model.Money(500000), // 5000 yuan
            availableLimit = com.omniflow.core.domain.model.Money(800000),
            profitLoss = com.omniflow.core.domain.model.Money.Zero,
        )
        assertTrue(result.isSuccess)

        val entries = reconciliationRepo.entriesForPeriod("2026-11")
        assertEquals(1, entries.size)
        assertEquals(firstAccount.id, entries[0].accountId)
    }
}
