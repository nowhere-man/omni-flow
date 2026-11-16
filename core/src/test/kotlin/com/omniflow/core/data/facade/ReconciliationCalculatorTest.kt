package com.omniflow.core.data.facade

import com.omniflow.core.data.local.createJvmDatabase
import com.omniflow.core.data.repository.SqlDelightAccountRepository
import com.omniflow.core.data.repository.SqlDelightReconciliationRepository
import com.omniflow.core.domain.model.Account
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReconciliationCalculatorTest {

    private val now = { Instant.fromEpochMilliseconds(1_000) }

    @Test
    fun isRecordedSnapshot_strictCriteria_notesOnlyDoesNotCount() {
        val empty = ReconciliationCalculator.emptySnapshot("2026-12", now)
        assertFalse(ReconciliationCalculator.isRecordedSnapshot(empty))

        // Month with only notes, 0 income and no account entries
        val notesOnly = empty.copy(
            meta = MonthlyReconciliationMeta(
                period = "2026-12",
                husbandIncome = Money.Zero,
                wifeIncome = Money.Zero,
                notes = "12月备忘：准备过年开销",
                updatedAt = now(),
            ),
        )
        assertFalse(ReconciliationCalculator.isRecordedSnapshot(notesOnly))

        // With husband income > 0
        val husbandIncomeOnly = empty.copy(
            meta = MonthlyReconciliationMeta(
                period = "2026-12",
                husbandIncome = Money(10_000_00),
                wifeIncome = Money.Zero,
                notes = "",
                updatedAt = now(),
            ),
        )
        assertTrue(ReconciliationCalculator.isRecordedSnapshot(husbandIncomeOnly))

        // With wife income > 0
        val wifeIncomeOnly = empty.copy(
            meta = MonthlyReconciliationMeta(
                period = "2026-12",
                husbandIncome = Money.Zero,
                wifeIncome = Money(8_000_00),
                notes = "",
                updatedAt = now(),
            ),
        )
        assertTrue(ReconciliationCalculator.isRecordedSnapshot(wifeIncomeOnly))

        // With account entries
        val withEntry = empty.copy(
            accountEntries = listOf(
                ReconciliationEntry(
                    id = "e1",
                    period = "2026-12",
                    accountId = "acc-1",
                    ownerRole = AccountRole.HUSBAND,
                    date = now(),
                    deposit = Money(50_000_00),
                    debt = Money.Zero,
                    investment = Money.Zero,
                    profitLoss = Money.Zero,
                    creditLimit = Money.Zero,
                    availableLimit = null,
                    createdAt = now(),
                ),
            ),
        )
        assertTrue(ReconciliationCalculator.isRecordedSnapshot(withEntry))
    }

    @Test
    fun yearEndNetAssets_whenDecemberHasOnlyNotes_takesPreviousMonthRealSnapshot() = runBlocking {
        val database = createJvmDatabase()
        val accountRepo = SqlDelightAccountRepository(database = database, now = now)
        val reconciliationRepo = SqlDelightReconciliationRepository(database, now)
        val facade = SqlDelightReconciliationFacade(reconciliationRepo, accountRepo)

        val acc = Account(
            id = "acc-test-1",
            name = "招行储蓄卡",
            type = AccountType.DEBIT_CARD,
            iconKey = "card",
            cardNumber = null,
            note = null,
            balance = Money(100_000_00),
            creditLimit = Money.Zero,
            availableLimit = null,
            profitLoss = Money.Zero,
            ownerRole = AccountRole.HUSBAND,
            includeInTotalAssets = true,
        )
        accountRepo.create(acc)

        // 2026-11: Real reconciliation snapshot with 100,000.00 net assets
        reconciliationRepo.recordEntry(
            ReconciliationEntry(
                id = "entry-nov",
                period = "2026-11",
                accountId = acc.id,
                ownerRole = acc.ownerRole,
                date = now(),
                deposit = Money(100_000_00),
                debt = Money.Zero,
                investment = Money.Zero,
                profitLoss = Money.Zero,
                creditLimit = Money.Zero,
                availableLimit = null,
                createdAt = now(),
            ),
        )

        // 2026-12: Only notes added, no account entries and no income
        facade.updateMonthlyNotes("2026-12", "12月备忘：准备过年开销")

        val report = facade.observeAnnualReport(2026, null).first().getOrThrow()

        // 11月有真实对账，其净资产为 100,000.00
        assertEquals(Money(100_000_00), report.monthlySnapshots[10].netAssets)

        // 12月无条目无收入仅备忘，其月度快照净资产为 0
        assertEquals(Money.Zero, report.monthlySnapshots[11].netAssets)
        assertEquals("12月备忘：准备过年开销", report.monthlySnapshots[11].meta.notes)

        // 年末净资产不应取到 12 月的 0，而必须取到 11 月真实快照的 100,000.00
        assertEquals(Money(100_000_00), report.yearEndNetAssets)
    }
}
