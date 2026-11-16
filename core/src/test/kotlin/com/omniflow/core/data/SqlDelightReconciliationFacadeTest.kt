package com.omniflow.core.data

import com.omniflow.core.data.facade.SqlDelightReconciliationFacade
import com.omniflow.core.data.local.createJvmDatabase
import com.omniflow.core.data.repository.SqlDelightAccountRepository
import com.omniflow.core.data.repository.SqlDelightInitialDataRepository
import com.omniflow.core.data.repository.SqlDelightReconciliationRepository
import com.omniflow.core.domain.model.AccountRole
import com.omniflow.core.domain.model.AccountType
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.MonthlyReconciliationMeta
import com.omniflow.core.domain.model.ReconciliationEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SqlDelightReconciliationFacadeTest {

    private fun setupTestEnvironment(): Triple<SqlDelightReconciliationFacade, SqlDelightReconciliationRepository, SqlDelightAccountRepository> {
        val database = createJvmDatabase()
        val accountRepo = SqlDelightAccountRepository(database) { Instant.fromEpochMilliseconds(1_000) }
        val reconciliationRepo = SqlDelightReconciliationRepository(database) { Instant.fromEpochMilliseconds(1_000) }
        val facade = SqlDelightReconciliationFacade(reconciliationRepo, accountRepo)
        return Triple(facade, reconciliationRepo, accountRepo)
    }

    private suspend fun createTestAccount(
        repo: SqlDelightAccountRepository,
        name: String,
        type: AccountType,
        balance: Money = Money.Zero,
        creditLimit: Money = Money.Zero,
        availableLimit: Money? = null,
        ownerRole: AccountRole = AccountRole.HUSBAND,
        id: String = "acc-" + name.hashCode(),
    ): com.omniflow.core.domain.model.Account {
        val acc = com.omniflow.core.domain.model.Account(
            id = id,
            name = name,
            type = type,
            iconKey = "card",
            cardNumber = null,
            note = null,
            balance = balance,
            creditLimit = creditLimit,
            availableLimit = availableLimit,
            profitLoss = Money.Zero,
            ownerRole = ownerRole,
            includeInTotalAssets = true,
        )
        repo.create(acc)
        return acc
    }

    @Test
    fun testAnnualReportComputationPreciseNumerics() = runBlocking {
        val (facade, reconciliationRepo, accountRepo) = setupTestEnvironment()

        val hCard = createTestAccount(
            repo = accountRepo,
            name = "丈夫卡",
            type = AccountType.CREDIT_CARD,
            creditLimit = Money(100_000_00),
            availableLimit = Money(80_000_00),
            ownerRole = AccountRole.HUSBAND,
        )

        for (month in 1..12) {
            val period = "2025-%02d".format(month)
            reconciliationRepo.upsertMeta(
                MonthlyReconciliationMeta(
                    period = period,
                    husbandIncome = Money(20_000_00),
                    wifeIncome = Money(15_000_00),
                    notes = "测试备忘",
                    updatedAt = Instant.fromEpochMilliseconds(1_000),
                ),
            )
            reconciliationRepo.recordEntry(
                ReconciliationEntry(
                    id = "entry-$month",
                    accountId = hCard.id,
                    period = period,
                    date = Instant.fromEpochMilliseconds(month * 1000L),
                    creditLimit = Money(100_000_00),
                    availableLimit = Money(80_000_00),
                    debt = Money(20_000_00),
                    deposit = Money(50_000_00),
                    investment = Money.Zero,
                    profitLoss = Money.Zero,
                    ownerRole = AccountRole.HUSBAND,
                    createdAt = Instant.fromEpochMilliseconds(month * 1000L),
                ),
            )
        }

        val reportResult = facade.observeAnnualReport(2025, null).first()
        assertTrue(reportResult.isSuccess)

        val report = reportResult.getOrThrow()
        assertEquals(2025, report.year)
        assertEquals(12, report.monthlySnapshots.size)

        // Exact numerical assertions: (20k + 15k) * 12 = 420,000.00
        assertEquals(Money(420_000_00), report.totalAnnualIncome)
        // Total credit limit = 100,000.00
        assertEquals(Money(100_000_00), report.totalCreditLimit)
        // Year end net assets = 50,000 - 20,000 = 30,000.00
        assertEquals(Money(30_000_00), report.yearEndNetAssets)

        // Role filtering: Husband annual income = 20k * 12 = 240k
        val husbandReport = facade.observeAnnualReport(2025, AccountRole.HUSBAND).first().getOrThrow()
        assertEquals(Money(240_000_00), husbandReport.totalAnnualIncome)

        // Wife annual income = 15k * 12 = 180k
        val wifeReport = facade.observeAnnualReport(2025, AccountRole.WIFE).first().getOrThrow()
        assertEquals(Money(180_000_00), wifeReport.totalAnnualIncome)
    }

    @Test
    fun testReconciliationDeduplicationSameAccountSameMonth() = runBlocking {
        // A1: Deduplication test: multiple updates for the same account in the same month
        val (facade, reconciliationRepo, accountRepo) = setupTestEnvironment()

        val acc = createTestAccount(
            repo = accountRepo,
            name = "储蓄账户",
            type = AccountType.DEBIT_CARD,
            balance = Money(10_000_00),
            ownerRole = AccountRole.HUSBAND,
        )

        // Record update 1: deposit = 10k at t=1000
        reconciliationRepo.recordEntry(
            ReconciliationEntry(
                id = "up-1",
                accountId = acc.id,
                period = "2026-03",
                date = Instant.fromEpochMilliseconds(1_000),
                creditLimit = Money.Zero,
                availableLimit = null,
                debt = Money.Zero,
                deposit = Money(10_000_00),
                investment = Money.Zero,
                profitLoss = Money.Zero,
                ownerRole = AccountRole.HUSBAND,
                createdAt = Instant.fromEpochMilliseconds(1_000),
            ),
        )

        // Record update 2: deposit = 20k at t=2000
        reconciliationRepo.recordEntry(
            ReconciliationEntry(
                id = "up-2",
                accountId = acc.id,
                period = "2026-03",
                date = Instant.fromEpochMilliseconds(2_000),
                creditLimit = Money.Zero,
                availableLimit = null,
                debt = Money.Zero,
                deposit = Money(20_000_00),
                investment = Money.Zero,
                profitLoss = Money.Zero,
                ownerRole = AccountRole.HUSBAND,
                createdAt = Instant.fromEpochMilliseconds(2_000),
            ),
        )

        // Record update 3: deposit = 30k at t=3000 (the latest)
        reconciliationRepo.recordEntry(
            ReconciliationEntry(
                id = "up-3",
                accountId = acc.id,
                period = "2026-03",
                date = Instant.fromEpochMilliseconds(3_000),
                creditLimit = Money.Zero,
                availableLimit = null,
                debt = Money.Zero,
                deposit = Money(30_000_00),
                investment = Money.Zero,
                profitLoss = Money.Zero,
                ownerRole = AccountRole.HUSBAND,
                createdAt = Instant.fromEpochMilliseconds(3_000),
            ),
        )

        // In database, there are 3 raw entries for 2026-03
        assertEquals(3, reconciliationRepo.entriesForPeriod("2026-03").size)

        // In period snapshot: should ONLY count the latest entry (30,000.00), NOT 10k + 20k + 30k = 60k
        val snapshot = facade.observePeriodSnapshot("2026-03", null).first().getOrThrow()
        assertNotNull(snapshot)
        assertEquals(Money(30_000_00), snapshot.totalAssets)
        assertEquals(Money(30_000_00), snapshot.netAssets)
        assertEquals(1, snapshot.accountEntries.size)
        assertEquals("up-3", snapshot.accountEntries[0].id)

        // In annual report: should also only count the latest 30,000.00
        val annual = facade.observeAnnualReport(2026, null).first().getOrThrow()
        val marchSnapshot = annual.monthlySnapshots[2] // March is index 2
        assertEquals(Money(30_000_00), marchSnapshot.totalAssets)
        assertEquals(Money(30_000_00), marchSnapshot.netAssets)
    }

    @Test
    fun testObservePeriodSnapshotDerivedMetrics() = runBlocking {
        // A2: Real derived savings, derived expense, and savings rate percent
        val (facade, reconciliationRepo, accountRepo) = setupTestEnvironment()

        val acc = createTestAccount(
            repo = accountRepo,
            name = "储蓄卡",
            type = AccountType.DEBIT_CARD,
            balance = Money(50_000_00),
            ownerRole = AccountRole.HUSBAND,
        )

        // Period 1: 2026-01 -> Net Assets = 100,000.00; Income = 20,000.00
        reconciliationRepo.upsertMeta(
            MonthlyReconciliationMeta(
                period = "2026-01",
                husbandIncome = Money(20_000_00),
                wifeIncome = Money.Zero,
                notes = "",
                updatedAt = Instant.fromEpochMilliseconds(1_000),
            ),
        )
        reconciliationRepo.recordEntry(
            ReconciliationEntry(
                id = "e-jan",
                accountId = acc.id,
                period = "2026-01",
                date = Instant.fromEpochMilliseconds(1_000),
                creditLimit = Money.Zero,
                availableLimit = null,
                debt = Money.Zero,
                deposit = Money(100_000_00),
                investment = Money.Zero,
                profitLoss = Money.Zero,
                ownerRole = AccountRole.HUSBAND,
                createdAt = Instant.fromEpochMilliseconds(1_000),
            ),
        )

        // Period 2: 2026-02 -> Net Assets = 112,000.00; Income = 20,000.00
        // netSavings = 112,000 - 100,000 = 12,000.00
        // derivedExpense = 20,000 - 12,000 = 8,000.00
        // savingsRatePercent = (12,000 / 20,000) * 100 = 60.0%
        reconciliationRepo.upsertMeta(
            MonthlyReconciliationMeta(
                period = "2026-02",
                husbandIncome = Money(20_000_00),
                wifeIncome = Money.Zero,
                notes = "",
                updatedAt = Instant.fromEpochMilliseconds(2_000),
            ),
        )
        reconciliationRepo.recordEntry(
            ReconciliationEntry(
                id = "e-feb",
                accountId = acc.id,
                period = "2026-02",
                date = Instant.fromEpochMilliseconds(2_000),
                creditLimit = Money.Zero,
                availableLimit = null,
                debt = Money.Zero,
                deposit = Money(112_000_00),
                investment = Money.Zero,
                profitLoss = Money.Zero,
                ownerRole = AccountRole.HUSBAND,
                createdAt = Instant.fromEpochMilliseconds(2_000),
            ),
        )

        val febSnapshot = facade.observePeriodSnapshot("2026-02", null).first().getOrThrow()
        assertNotNull(febSnapshot)
        assertEquals(Money(112_000_00), febSnapshot.netAssets)
        assertEquals(Money(12_000_00), febSnapshot.netSavings)
        assertEquals(Money(8_000_00), febSnapshot.derivedExpense)
        assertEquals(60.0, febSnapshot.savingsRatePercent, 0.001)
    }

    @Test
    fun testUpdateMonthlyNotesDoesNotOverwriteIncome() = runBlocking {
        // A3: updateMonthlyNotes updates notes without resetting incomes to Money.Zero
        val (facade, reconciliationRepo, _) = setupTestEnvironment()

        // Initial setup with established incomes
        facade.updateMonthlyMeta(
            period = "2026-04",
            husbandIncome = Money(30_000_00),
            wifeIncome = Money(25_000_00),
            notes = "初始备忘",
        )

        // Now call atomic updateMonthlyNotes
        val result = facade.updateMonthlyNotes("2026-04", "修改后的备忘：年终奖结清")
        assertTrue(result.isSuccess)

        val loadedMeta = reconciliationRepo.metaForPeriod("2026-04")
        assertNotNull(loadedMeta)
        assertEquals(Money(30_000_00), loadedMeta.husbandIncome)
        assertEquals(Money(25_000_00), loadedMeta.wifeIncome)
        assertEquals("修改后的备忘：年终奖结清", loadedMeta.notes)
    }

    @Test
    fun testFamilyRoleExplicitHandling() = runBlocking {
        // A4: AccountRole.FAMILY is handled explicitly and not mixed into HUSBAND
        val (facade, reconciliationRepo, accountRepo) = setupTestEnvironment()

        val hAcc = createTestAccount(
            repo = accountRepo,
            name = "丈夫个人账户",
            type = AccountType.DEBIT_CARD,
            ownerRole = AccountRole.HUSBAND,
        )
        val wAcc = createTestAccount(
            repo = accountRepo,
            name = "妻子个人账户",
            type = AccountType.DEBIT_CARD,
            ownerRole = AccountRole.WIFE,
        )
        val fAcc = createTestAccount(
            repo = accountRepo,
            name = "家庭公用账户",
            type = AccountType.DEBIT_CARD,
            ownerRole = AccountRole.FAMILY,
        )

        reconciliationRepo.recordEntry(
            ReconciliationEntry(
                id = "h-entry",
                accountId = hAcc.id,
                period = "2026-06",
                date = Instant.fromEpochMilliseconds(1_000),
                creditLimit = Money.Zero,
                availableLimit = null,
                debt = Money(5_000_00),
                deposit = Money(50_000_00),
                investment = Money.Zero,
                profitLoss = Money.Zero,
                ownerRole = AccountRole.HUSBAND,
                createdAt = Instant.fromEpochMilliseconds(1_000),
            ),
        )
        reconciliationRepo.recordEntry(
            ReconciliationEntry(
                id = "w-entry",
                accountId = wAcc.id,
                period = "2026-06",
                date = Instant.fromEpochMilliseconds(1_000),
                creditLimit = Money.Zero,
                availableLimit = null,
                debt = Money(2_000_00),
                deposit = Money(80_000_00),
                investment = Money.Zero,
                profitLoss = Money.Zero,
                ownerRole = AccountRole.WIFE,
                createdAt = Instant.fromEpochMilliseconds(1_000),
            ),
        )
        reconciliationRepo.recordEntry(
            ReconciliationEntry(
                id = "f-entry",
                accountId = fAcc.id,
                period = "2026-06",
                date = Instant.fromEpochMilliseconds(1_000),
                creditLimit = Money.Zero,
                availableLimit = null,
                debt = Money.Zero,
                deposit = Money(200_000_00),
                investment = Money.Zero,
                profitLoss = Money.Zero,
                ownerRole = AccountRole.FAMILY,
                createdAt = Instant.fromEpochMilliseconds(1_000),
            ),
        )

        val fullSnapshot = facade.observePeriodSnapshot("2026-06", null).first().getOrThrow()
        assertNotNull(fullSnapshot)
        // Husband: assets = 50k, liab = 5k, net = 45k
        assertEquals(Money(50_000_00), fullSnapshot.husbandSummary.totalAssets)
        assertEquals(Money(5_000_00), fullSnapshot.husbandSummary.totalLiabilities)
        assertEquals(Money(45_000_00), fullSnapshot.husbandSummary.netAssets)

        // Wife: assets = 80k, liab = 2k, net = 78k
        assertEquals(Money(80_000_00), fullSnapshot.wifeSummary.totalAssets)
        assertEquals(Money(2_000_00), fullSnapshot.wifeSummary.totalLiabilities)
        assertEquals(Money(78_000_00), fullSnapshot.wifeSummary.netAssets)

        // Family: assets = 200k, liab = 0, net = 200k
        assertEquals(Money(200_000_00), fullSnapshot.familySummary.totalAssets)
        assertEquals(Money.Zero, fullSnapshot.familySummary.totalLiabilities)
        assertEquals(Money(200_000_00), fullSnapshot.familySummary.netAssets)

        // Combined: assets = 330k, liab = 7k, net = 323k
        assertEquals(Money(330_000_00), fullSnapshot.totalAssets)
        assertEquals(Money(7_000_00), fullSnapshot.totalLiabilities)
        assertEquals(Money(323_000_00), fullSnapshot.netAssets)

        // Filtered Husband view: only 50k / 5k, FAMILY is NOT mixed into HUSBAND
        val husbandSnapshot = facade.observePeriodSnapshot("2026-06", AccountRole.HUSBAND).first().getOrThrow()
        assertNotNull(husbandSnapshot)
        assertEquals(Money(50_000_00), husbandSnapshot.totalAssets)
        assertEquals(Money(5_000_00), husbandSnapshot.totalLiabilities)
        assertEquals(Money(45_000_00), husbandSnapshot.netAssets)
        assertEquals(1, husbandSnapshot.accountEntries.size)
    }

    @Test
    fun testNegativeNetAssetsYearEndSelection() = runBlocking {
        // A5: When all recorded months have negative net assets, latestRecordedSnapshot is still valid
        val (facade, reconciliationRepo, accountRepo) = setupTestEnvironment()

        val acc = createTestAccount(
            repo = accountRepo,
            name = "负债账户",
            type = AccountType.CREDIT_CARD,
            creditLimit = Money(100_000_00),
            availableLimit = Money(10_000_00),
            ownerRole = AccountRole.HUSBAND,
        )

        // Record Jan: assets = 10k, debt = 90k => netAssets = -80k
        reconciliationRepo.recordEntry(
            ReconciliationEntry(
                id = "jan-neg",
                accountId = acc.id,
                period = "2026-01",
                date = Instant.fromEpochMilliseconds(1_000),
                creditLimit = Money(100_000_00),
                availableLimit = Money(10_000_00),
                debt = Money(90_000_00),
                deposit = Money(10_000_00),
                investment = Money.Zero,
                profitLoss = Money.Zero,
                ownerRole = AccountRole.HUSBAND,
                createdAt = Instant.fromEpochMilliseconds(1_000),
            ),
        )

        // Record Feb: assets = 10k, debt = 60k => netAssets = -50k
        reconciliationRepo.recordEntry(
            ReconciliationEntry(
                id = "feb-neg",
                accountId = acc.id,
                period = "2026-02",
                date = Instant.fromEpochMilliseconds(2_000),
                creditLimit = Money(100_000_00),
                availableLimit = Money(40_000_00),
                debt = Money(60_000_00),
                deposit = Money(10_000_00),
                investment = Money.Zero,
                profitLoss = Money.Zero,
                ownerRole = AccountRole.HUSBAND,
                createdAt = Instant.fromEpochMilliseconds(2_000),
            ),
        )

        val report = facade.observeAnnualReport(2026, null).first().getOrThrow()
        // Year end net assets should be from February (-50,000.00), NOT fallback to 0
        assertEquals(Money(-50_000_00), report.yearEndNetAssets)
    }

    @Test
    fun testObserveAllPeriods() = runBlocking {
        val (facade, reconciliationRepo, accountRepo) = setupTestEnvironment()
        val acc = accountRepo.activeAccounts().firstOrNull() ?: createTestAccount(
            repo = accountRepo,
            name = "测试卡",
            type = AccountType.DEBIT_CARD,
        )

        reconciliationRepo.recordEntry(
            ReconciliationEntry(
                id = "p1",
                accountId = acc.id,
                period = "2025-10",
                date = Instant.fromEpochMilliseconds(1_000),
                creditLimit = Money.Zero,
                availableLimit = null,
                debt = Money.Zero,
                deposit = Money.Zero,
                investment = Money.Zero,
                profitLoss = Money.Zero,
                ownerRole = AccountRole.HUSBAND,
                createdAt = Instant.fromEpochMilliseconds(1_000),
            ),
        )
        reconciliationRepo.recordEntry(
            ReconciliationEntry(
                id = "p2",
                accountId = acc.id,
                period = "2025-12",
                date = Instant.fromEpochMilliseconds(2_000),
                creditLimit = Money.Zero,
                availableLimit = null,
                debt = Money.Zero,
                deposit = Money.Zero,
                investment = Money.Zero,
                profitLoss = Money.Zero,
                ownerRole = AccountRole.HUSBAND,
                createdAt = Instant.fromEpochMilliseconds(2_000),
            ),
        )

        val periods = facade.observeAllPeriods().first()
        assertEquals(listOf("2025-12", "2025-10"), periods)
    }

    @Test
    fun testQuickUpdateFinanceRecordsAuditEntry() = runBlocking {
        val (facade, reconciliationRepo, accountRepo) = setupTestEnvironment()

        val acc = createTestAccount(
            repo = accountRepo,
            name = "活期测试",
            type = AccountType.DEBIT_CARD,
            balance = Money(1_000_00),
        )

        val result = facade.recordAccountUpdate(
            accountId = acc.id,
            period = "2026-11",
            balance = Money(500000),
            availableLimit = null,
            profitLoss = Money.Zero,
        )
        assertTrue(result.isSuccess)

        val entries = reconciliationRepo.entriesForPeriod("2026-11")
        assertEquals(1, entries.size)
        assertEquals(acc.id, entries[0].accountId)
        assertEquals(Money(500000), entries[0].deposit)
        assertEquals(Money.Zero, entries[0].debt)
    }
}
