package com.omniflow.core.data.repository

import com.omniflow.core.data.local.createJvmDatabase
import com.omniflow.core.domain.model.Account
import com.omniflow.core.domain.model.AccountId
import com.omniflow.core.domain.model.AccountRole
import com.omniflow.core.domain.model.AccountType
import com.omniflow.core.domain.model.Money
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class SqlDelightAccountRepositoryFinanceTest {

    private fun createRepository(): SqlDelightAccountRepository {
        val db = createJvmDatabase()
        val initialRepo = SqlDelightInitialDataRepository(db) { Instant.fromEpochMilliseconds(1_000) }
        runBlocking { initialRepo.seedIfNeeded() }
        return SqlDelightAccountRepository(db) { Instant.fromEpochMilliseconds(1_000) }
    }

    private suspend fun createTestAccount(
        repo: SqlDelightAccountRepository,
        name: String,
        type: AccountType,
        balance: Money,
        creditLimit: Money = Money.Zero,
        availableLimit: Money? = null,
        ownerRole: AccountRole = AccountRole.HUSBAND,
        id: String = "acc-" + name.hashCode(),
    ): Account {
        val acc = Account(
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
    fun testQuickUpdateFinanceUpdatesAllFields() = runBlocking {
        val repo = createRepository()
        val account = repo.activeAccounts().first()

        val newBalance = Money(123_456_00)
        val newAvailable = Money(88_000_00)
        val newProfitLoss = Money(15_000_00)

        repo.quickUpdateFinance(
            accountId = account.id,
            balance = newBalance,
            availableLimit = newAvailable,
            profitLoss = newProfitLoss,
        )

        val updated = repo.activeAccounts().first { it.id == account.id }
        assertEquals(newBalance, updated.balance)
        assertEquals(newAvailable, updated.availableLimit)
        assertEquals(newProfitLoss, updated.profitLoss)
    }

    @Test
    fun testSummaryAndTotalCreditLimitByRole() = runBlocking {
        val db = createJvmDatabase()
        val repo = SqlDelightAccountRepository(db) { Instant.fromEpochMilliseconds(1_000) }

        // Create isolated accounts for precise assertions
        val hCard = createTestAccount(
            repo = repo,
            name = "丈夫信用卡",
            type = AccountType.CREDIT_CARD,
            balance = Money.Zero,
            creditLimit = Money(100_000_00),
            availableLimit = Money(70_000_00), // debt = 30,000.00
            ownerRole = AccountRole.HUSBAND,
        )
        val hDebit = createTestAccount(
            repo = repo,
            name = "丈夫储蓄卡",
            type = AccountType.DEBIT_CARD,
            balance = Money(50_000_00), // asset = 50,000.00
            ownerRole = AccountRole.HUSBAND,
        )
        val wCard = createTestAccount(
            repo = repo,
            name = "妻子信用卡",
            type = AccountType.CREDIT_CARD,
            balance = Money.Zero,
            creditLimit = Money(80_000_00),
            availableLimit = Money(60_000_00), // debt = 20,000.00
            ownerRole = AccountRole.WIFE,
        )
        val wDebit = createTestAccount(
            repo = repo,
            name = "妻子储蓄卡",
            type = AccountType.DEBIT_CARD,
            balance = Money(120_000_00), // asset = 120,000.00
            ownerRole = AccountRole.WIFE,
        )
        val fDebit = createTestAccount(
            repo = repo,
            name = "家庭公共账户",
            type = AccountType.DEBIT_CARD,
            balance = Money(200_000_00), // asset = 200,000.00
            ownerRole = AccountRole.FAMILY,
        )

        // Husband Summary: assets = 50k, liab = 30k, net = 20k
        val hSummary = repo.summary(AccountRole.HUSBAND)
        assertEquals(Money(50_000_00), hSummary.assets)
        assertEquals(Money(30_000_00), hSummary.liabilities)
        assertEquals(Money(20_000_00), hSummary.netAssets)
        assertEquals(Money(100_000_00), repo.totalCreditLimit(AccountRole.HUSBAND))

        // Wife Summary: assets = 120k, liab = 20k, net = 100k
        val wSummary = repo.summary(AccountRole.WIFE)
        assertEquals(Money(120_000_00), wSummary.assets)
        assertEquals(Money(20_000_00), wSummary.liabilities)
        assertEquals(Money(100_000_00), wSummary.netAssets)
        assertEquals(Money(80_000_00), repo.totalCreditLimit(AccountRole.WIFE))

        // Family Summary: assets = 200k, liab = 0, net = 200k
        val fSummary = repo.summary(AccountRole.FAMILY)
        assertEquals(Money(200_000_00), fSummary.assets)
        assertEquals(Money.Zero, fSummary.liabilities)
        assertEquals(Money(200_000_00), fSummary.netAssets)
        assertEquals(Money.Zero, repo.totalCreditLimit(AccountRole.FAMILY))

        // Total Combined Summary (role = null): assets = 370k, liab = 50k, net = 320k
        val totalSummary = repo.summary(null)
        assertEquals(Money(370_000_00), totalSummary.assets)
        assertEquals(Money(50_000_00), totalSummary.liabilities)
        assertEquals(Money(320_000_00), totalSummary.netAssets)
        assertEquals(Money(180_000_00), repo.totalCreditLimit(null))
    }
}
