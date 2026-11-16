package com.omniflow.core.data.repository

import com.omniflow.core.data.local.createJvmDatabase
import com.omniflow.core.domain.model.AccountRole
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.MonthlyReconciliationMeta
import com.omniflow.core.domain.model.ReconciliationEntry
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SqlDelightReconciliationRepositoryTest {

    private fun createRepository(): Pair<SqlDelightReconciliationRepository, SqlDelightAccountRepository> {
        val db = createJvmDatabase()
        val accountRepo = SqlDelightAccountRepository(db) { Instant.fromEpochMilliseconds(1_000) }
        val reconciliationRepo = SqlDelightReconciliationRepository(db) { Instant.fromEpochMilliseconds(1_000) }
        return reconciliationRepo to accountRepo
    }

    private suspend fun createTestAccount(
        repo: SqlDelightAccountRepository,
        name: String,
        id: String = "acc-" + name.hashCode(),
    ): com.omniflow.core.domain.model.Account {
        val acc = com.omniflow.core.domain.model.Account(
            id = id,
            name = name,
            type = com.omniflow.core.domain.model.AccountType.DEBIT_CARD,
            iconKey = "card",
            cardNumber = null,
            note = null,
            balance = Money.Zero,
            creditLimit = Money.Zero,
            availableLimit = null,
            profitLoss = Money.Zero,
            ownerRole = AccountRole.HUSBAND,
            includeInTotalAssets = true,
        )
        repo.create(acc)
        return acc
    }

    @Test
    fun testRecordAndRetrieveEntries() = runBlocking {
        val (repo, accountRepo) = createRepository()
        val account = createTestAccount(accountRepo, "测试账户1")

        val entry1 = ReconciliationEntry(
            id = "entry-1",
            accountId = account.id,
            period = "2026-01",
            date = Instant.fromEpochMilliseconds(10_000),
            creditLimit = Money(50_000_00),
            availableLimit = Money(42_000_00),
            debt = Money(8_000_00),
            deposit = Money.Zero,
            investment = Money.Zero,
            profitLoss = Money.Zero,
            ownerRole = AccountRole.HUSBAND,
            createdAt = Instant.fromEpochMilliseconds(10_000),
        )

        repo.recordEntry(entry1)

        val entries = repo.entriesForPeriod("2026-01")
        assertEquals(1, entries.size)
        val loaded = entries.first()
        assertEquals("entry-1", loaded.id)
        assertEquals(account.id, loaded.accountId)
        assertEquals("2026-01", loaded.period)
        assertEquals(Money(50_000_00), loaded.creditLimit)
        assertEquals(Money(42_000_00), loaded.availableLimit)
        assertEquals(Money(8_000_00), loaded.debt)
        assertEquals(AccountRole.HUSBAND, loaded.ownerRole)

        // Empty period
        val emptyEntries = repo.entriesForPeriod("2026-02")
        assertEquals(0, emptyEntries.size)
    }

    @Test
    fun testAllDistinctPeriodsAndAllEntries() = runBlocking {
        val (repo, accountRepo) = createRepository()
        val acc1 = createTestAccount(accountRepo, "账户A")
        val acc2 = createTestAccount(accountRepo, "账户B")

        val e1 = ReconciliationEntry(
            id = "e1",
            accountId = acc1.id,
            period = "2025-11",
            date = Instant.fromEpochMilliseconds(1_000),
            creditLimit = Money(20_000_00),
            availableLimit = null,
            debt = Money.Zero,
            deposit = Money(15_000_00),
            investment = Money.Zero,
            profitLoss = Money.Zero,
            ownerRole = AccountRole.HUSBAND,
            createdAt = Instant.fromEpochMilliseconds(1_000),
        )
        val e2 = ReconciliationEntry(
            id = "e2",
            accountId = acc2.id,
            period = "2025-12",
            date = Instant.fromEpochMilliseconds(2_000),
            creditLimit = Money.Zero,
            availableLimit = null,
            debt = Money.Zero,
            deposit = Money(30_000_00),
            investment = Money.Zero,
            profitLoss = Money.Zero,
            ownerRole = AccountRole.WIFE,
            createdAt = Instant.fromEpochMilliseconds(2_000),
        )

        repo.recordEntry(e1)
        repo.recordEntry(e2)

        val periods = repo.allDistinctPeriods()
        assertEquals(listOf("2025-12", "2025-11"), periods)

        val all = repo.allEntries()
        assertEquals(2, all.size)
        assertEquals("e1", all[0].id)
        assertEquals("e2", all[1].id)
    }

    @Test
    fun testUpsertAndRetrieveMonthlyMeta() = runBlocking {
        val (repo, _) = createRepository()

        assertNull(repo.metaForPeriod("2026-05"))

        val meta = MonthlyReconciliationMeta(
            period = "2026-05",
            husbandIncome = Money(35_000_00),
            wifeIncome = Money(25_000_00),
            notes = "五一假期支出与奖金到账",
            updatedAt = Instant.fromEpochMilliseconds(5_000),
        )

        repo.upsertMeta(meta)

        val loaded = repo.metaForPeriod("2026-05")
        assertNotNull(loaded)
        assertEquals("2026-05", loaded.period)
        assertEquals(Money(35_000_00), loaded.husbandIncome)
        assertEquals(Money(25_000_00), loaded.wifeIncome)
        assertEquals(Money(60_000_00), loaded.totalIncome)
        assertEquals("五一假期支出与奖金到账", loaded.notes)

        // Upsert overwrite
        val updated = meta.copy(notes = "更新后的备忘", husbandIncome = Money(40_000_00))
        repo.upsertMeta(updated)

        val reloaded = repo.metaForPeriod("2026-05")
        assertNotNull(reloaded)
        assertEquals(Money(40_000_00), reloaded.husbandIncome)
        assertEquals("更新后的备忘", reloaded.notes)

        val allMetas = repo.allMetas()
        assertEquals(1, allMetas.size)
        assertEquals("2026-05", allMetas[0].period)
    }
}
