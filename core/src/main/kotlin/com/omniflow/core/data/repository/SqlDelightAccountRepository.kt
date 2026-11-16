package com.omniflow.core.data.repository

import com.omniflow.core.db.OmniFlowDatabase
import com.omniflow.core.domain.model.Account
import com.omniflow.core.domain.model.AccountId
import com.omniflow.core.domain.model.AccountRole
import com.omniflow.core.domain.model.AccountSummary
import com.omniflow.core.domain.model.AccountType
import com.omniflow.core.domain.model.AppTimeZone
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.repository.AccountRepository
import com.omniflow.core.domain.util.UuidGenerator
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

class SqlDelightAccountRepository(
    private val database: OmniFlowDatabase,
    private val ids: UuidGenerator = UuidGenerator(),
    private val now: () -> Instant = { Clock.System.now() },
) : AccountRepository {
    override suspend fun activeAccounts(): List<Account> = database.accountQueries.activeAccounts().executeAsList().map {
        Account(
            id = it.id,
            name = it.name,
            type = AccountType.valueOf(it.type),
            iconKey = it.icon_key,
            cardNumber = it.card_number,
            note = it.note,
            balance = Money(it.balance_minor),
            creditLimit = Money(it.credit_limit_minor),
            availableLimit = it.available_limit_minor?.let(::Money),
            profitLoss = Money(it.profit_loss_minor),
            ownerRole = AccountRole.fromString(it.owner_role),
            includeInTotalAssets = it.include_in_total_assets != 0L,
            deletedAt = it.deleted_at?.let(Instant::fromEpochMilliseconds),
        )
    }

    override suspend fun create(account: Account) {
        require(account.name.isNotBlank()) { "账户名称不能为空" }
        require(account.iconKey.isNotBlank()) { "请选择账户图标" }
        val timestamp = now().toEpochMilliseconds()
        database.transaction {
            database.accountQueries.insertAccount(
                id = account.id,
                name = account.name.trim(),
                type = account.type.name,
                icon_key = account.iconKey,
                card_number = account.cardNumber?.trim()?.takeIf(String::isNotEmpty),
                note = account.note?.trim()?.takeIf(String::isNotEmpty),
                balance_minor = account.balance.minor,
                credit_limit_minor = account.creditLimit.minor,
                available_limit_minor = account.availableLimit?.minor,
                profit_loss_minor = account.profitLoss.minor,
                owner_role = account.ownerRole.name,
                include_in_total_assets = if (account.includeInTotalAssets) 1L else 0L,
                created_at = timestamp,
                updated_at = timestamp,
            )
            if (account.balance != Money.Zero) {
                database.accountQueries.insertAccountBalanceRecord(
                    id = ids.next(),
                    account_id = account.id,
                    date = dayStart(timestamp),
                    balance_minor = account.balance.minor,
                    delta_minor = account.balance.minor,
                    created_at = timestamp,
                )
            }
        }
    }

    override suspend fun update(account: Account) {
        require(account.name.isNotBlank()) { "账户名称不能为空" }
        require(account.iconKey.isNotBlank()) { "请选择账户图标" }
        val timestamp = now().toEpochMilliseconds()
        database.transaction {
            val previousBalance = database.accountQueries.activeAccountBalance(account.id).executeAsOneOrNull()
                ?: error("账户不存在或已删除")
            database.accountQueries.updateAccount(
                name = account.name.trim(),
                type = account.type.name,
                icon_key = account.iconKey,
                card_number = account.cardNumber?.trim()?.takeIf(String::isNotEmpty),
                note = account.note?.trim()?.takeIf(String::isNotEmpty),
                credit_limit_minor = account.creditLimit.minor,
                available_limit_minor = account.availableLimit?.minor,
                profit_loss_minor = account.profitLoss.minor,
                owner_role = account.ownerRole.name,
                include_in_total_assets = if (account.includeInTotalAssets) 1L else 0L,
                updated_at = timestamp,
                id = account.id,
            )
            if (account.balance.minor != previousBalance) {
                database.accountQueries.setAccountBalance(account.balance.minor, timestamp, account.id)
                database.accountQueries.insertAccountBalanceRecord(
                    id = ids.next(),
                    account_id = account.id,
                    date = dayStart(timestamp),
                    balance_minor = account.balance.minor,
                    delta_minor = account.balance.minor - previousBalance,
                    created_at = timestamp,
                )
            }
        }
    }

    override suspend fun quickUpdateFinance(
        accountId: AccountId,
        balance: Money,
        availableLimit: Money?,
        profitLoss: Money,
    ) {
        val timestamp = now().toEpochMilliseconds()
        database.transaction {
            val previousBalance = database.accountQueries.activeAccountBalance(accountId).executeAsOneOrNull()
                ?: error("账户不存在或已删除")
            database.accountQueries.quickUpdateAccountFinance(
                balance_minor = balance.minor,
                available_limit_minor = availableLimit?.minor,
                profit_loss_minor = profitLoss.minor,
                updated_at = timestamp,
                id = accountId,
            )
            if (balance.minor != previousBalance) {
                database.accountQueries.insertAccountBalanceRecord(
                    id = ids.next(),
                    account_id = accountId,
                    date = dayStart(timestamp),
                    balance_minor = balance.minor,
                    delta_minor = balance.minor - previousBalance,
                    created_at = timestamp,
                )
            }
        }
    }

    override suspend fun calibrate(accountId: AccountId, balance: Money) {
        val timestamp = now().toEpochMilliseconds()
        database.transaction {
            val previousBalance = database.accountQueries.activeAccountBalance(accountId)
                .executeAsOneOrNull()
                ?: error("账户不存在或已删除")
            database.accountQueries.setAccountBalance(
                balance_minor = balance.minor,
                updated_at = timestamp,
                id = accountId,
            )
            database.accountQueries.insertAccountBalanceRecord(
                id = ids.next(),
                account_id = accountId,
                date = dayStart(timestamp),
                balance_minor = balance.minor,
                delta_minor = balance.minor - previousBalance,
                created_at = timestamp,
            )
        }
    }

    private fun dayStart(timestamp: Long): Long = Instant.fromEpochMilliseconds(timestamp)
        .toLocalDateTime(AppTimeZone)
        .date
        .atStartOfDayIn(AppTimeZone)
        .toEpochMilliseconds()

    override suspend fun summary(role: AccountRole?): AccountSummary {
        val (assets, liabilities) = if (role == null) {
            val s = database.accountQueries.accountSummary().executeAsOne()
            s.assets_minor to s.liabilities_minor
        } else {
            val s = database.accountQueries.accountSummaryByRole(role.name).executeAsOne()
            s.assets_minor to s.liabilities_minor
        }
        return AccountSummary(Money(assets), Money(liabilities))
    }

    override suspend fun totalCreditLimit(role: AccountRole?): Money {
        val minor = if (role == null) {
            database.accountQueries.totalCreditLimit().executeAsOne()
        } else {
            database.accountQueries.totalCreditLimitByRole(role.name).executeAsOne()
        }
        return Money(minor)
    }

    override suspend fun archive(accountId: AccountId) {
        val timestamp = now().toEpochMilliseconds()
        database.accountQueries.archiveAccount(timestamp, accountId)
    }
}
