package com.omniflow.core.data.repository

import com.omniflow.core.db.OmniFlowDatabase
import com.omniflow.core.domain.model.AccountRole
import com.omniflow.core.domain.model.AccountType
import com.omniflow.core.domain.model.AppPreferenceKey
import com.omniflow.core.domain.model.SystemDefaults
import com.omniflow.core.domain.repository.InitialDataRepository
import com.omniflow.core.domain.util.UuidGenerator
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class SqlDelightInitialDataRepository(
    private val database: OmniFlowDatabase,
    private val ids: UuidGenerator = UuidGenerator(),
    private val now: () -> Instant = { Clock.System.now() },
) : InitialDataRepository {

    private data class FamilyAccountSeed(
        val name: String,
        val type: AccountType,
        val iconKey: String,
        val creditLimitYuan: Long,
        val availableLimitYuan: Long?,
        val balanceYuan: Long,
        val role: AccountRole,
    )

    override suspend fun seedIfNeeded() {
        if (database.appPreferenceQueries.preference(AppPreferenceKey.InitialDataSeeded)
                .executeAsOneOrNull() != null
        ) {
            return
        }

        val timestamp = now().toEpochMilliseconds()
        database.transaction {
            if (database.appPreferenceQueries.preference(AppPreferenceKey.InitialDataSeeded)
                    .executeAsOneOrNull() != null
            ) {
                return@transaction
            }

            val ledgerId = ids.next()
            database.ledgerQueries.insertLedger(ledgerId, "家庭账本", null, timestamp, timestamp)
            database.appPreferenceQueries.upsertPreference(
                key = AppPreferenceKey.DefaultLedgerId,
                value = ledgerId,
                updated_at = timestamp,
            )

            // 1. Seed categories
            SystemDefaults.categoryTemplates.forEach { category ->
                database.categoryQueries.insertCategory(
                    id = ids.next(),
                    ledger_id = ledgerId,
                    parent_id = null,
                    name = category.name,
                    icon_key = category.iconKey,
                    type = category.type.name,
                    created_at = timestamp,
                    updated_at = timestamp,
                )
            }

            // 2. Seed sample family accounts
            val accountSeeds = listOf(
                FamilyAccountSeed("示例账户-信用卡1", AccountType.CREDIT_CARD, "credit-card", 10_000, 8_000, 2_000, AccountRole.HUSBAND),
                FamilyAccountSeed("示例账户-信用卡2", AccountType.CREDIT_CARD, "credit-card", 10_000, 9_000, 1_000, AccountRole.WIFE),
                FamilyAccountSeed("示例账户-储蓄卡", AccountType.DEBIT_CARD, "banknote", 0, null, 5_000, AccountRole.FAMILY),
            )

            accountSeeds.forEach { seed ->
                val accountId = ids.next()
                val creditLimitMinor = seed.creditLimitYuan * 100
                val availableLimitMinor = seed.availableLimitYuan?.let { it * 100 }
                val balanceMinor = seed.balanceYuan * 100
                database.accountQueries.insertAccount(
                    id = accountId,
                    name = seed.name,
                    type = seed.type.name,
                    icon_key = seed.iconKey,
                    card_number = null,
                    note = null,
                    balance_minor = balanceMinor,
                    credit_limit_minor = creditLimitMinor,
                    available_limit_minor = availableLimitMinor,
                    profit_loss_minor = 0L,
                    owner_role = seed.role.name,
                    include_in_total_assets = 1L,
                    created_at = timestamp,
                    updated_at = timestamp,
                )
                // Record current reconciliation snapshot for 2026-10
                val debtMinor = if (availableLimitMinor != null && creditLimitMinor > availableLimitMinor) {
                    creditLimitMinor - availableLimitMinor
                } else 0L
                val depositMinor = if (seed.type != AccountType.INVESTMENT) balanceMinor else 0L
                val investmentMinor = if (seed.type == AccountType.INVESTMENT) balanceMinor else 0L

                database.reconciliationQueries.insertEntry(
                    id = ids.next(),
                    account_id = accountId,
                    period = "2026-10",
                    date = timestamp,
                    credit_limit_minor = creditLimitMinor,
                    available_limit_minor = availableLimitMinor,
                    debt_minor = debtMinor,
                    deposit_minor = depositMinor,
                    investment_minor = investmentMinor,
                    profit_loss_minor = 0L,
                    owner_role = seed.role.name,
                    created_at = timestamp,
                )
            }

            database.appPreferenceQueries.upsertPreference(
                key = AppPreferenceKey.InitialDataSeeded,
                value = "true",
                updated_at = timestamp,
            )
        }
    }
}
