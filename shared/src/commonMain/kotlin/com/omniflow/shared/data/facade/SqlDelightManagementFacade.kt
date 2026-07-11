package com.omniflow.shared.data.facade

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.omniflow.shared.db.OmniFlowDatabase
import com.omniflow.shared.domain.facade.ManagementFacade
import com.omniflow.shared.domain.model.Account
import com.omniflow.shared.domain.model.AccountSummary
import com.omniflow.shared.domain.model.AccountType
import com.omniflow.shared.domain.model.AppPreferenceKey
import com.omniflow.shared.domain.model.Category
import com.omniflow.shared.domain.model.Ledger
import com.omniflow.shared.domain.model.LedgerId
import com.omniflow.shared.domain.model.Money
import com.omniflow.shared.domain.model.Rule
import com.omniflow.shared.domain.model.RuleActionType
import com.omniflow.shared.domain.model.RuleConditionType
import com.omniflow.shared.domain.model.Tag
import com.omniflow.shared.domain.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant

class SqlDelightManagementFacade(
    private val database: OmniFlowDatabase,
) : ManagementFacade {
    override fun observeLedgers(): Flow<Result<List<Ledger>>> = database.ledgerQueries.activeLedgers()
        .asFlow()
        .mapToList(Dispatchers.Default)
        .map { rows -> runCatching { rows.map { Ledger(it.id, it.name, it.cover_key) } } }

    override fun observeDefaultLedgerId(): Flow<Result<LedgerId?>> = database.appPreferenceQueries
        .preference(AppPreferenceKey.DefaultLedgerId)
        .asFlow()
        .mapToList(Dispatchers.Default)
        .map { rows -> runCatching { rows.singleOrNull() } }

    override fun observeAccounts(): Flow<Result<List<Account>>> = database.accountQueries.activeAccounts()
        .asFlow()
        .mapToList(Dispatchers.Default)
        .map { rows -> runCatching {
            rows.map {
                Account(
                    id = it.id,
                    name = it.name,
                    type = AccountType.valueOf(it.type),
                    iconKey = it.icon_key,
                    cardNumber = it.card_number,
                    note = it.note,
                    balance = Money(it.balance_minor),
                    includeInTotalAssets = it.include_in_total_assets != 0L,
                    deletedAt = it.deleted_at?.let(Instant::fromEpochMilliseconds),
                )
            }
        } }

    override fun observeAccountSummary(): Flow<Result<AccountSummary>> = database.accountQueries.accountSummary()
        .asFlow()
        .mapToList(Dispatchers.Default)
        .map { rows -> runCatching {
            val summary = rows.single()
            AccountSummary(Money(summary.assets_minor), Money(summary.liabilities_minor))
        } }

    override fun observeCategories(ledgerId: LedgerId): Flow<Result<List<Category>>> = database.categoryQueries
        .activeCategoriesForLedger(ledgerId)
        .asFlow()
        .mapToList(Dispatchers.Default)
        .map { rows -> runCatching {
            rows.map {
                Category(
                    id = it.id,
                    ledgerId = it.ledger_id,
                    parentId = it.parent_id,
                    name = it.name,
                    iconKey = it.icon_key,
                    type = TransactionType.valueOf(it.type),
                    deletedAt = it.deleted_at?.let(Instant::fromEpochMilliseconds),
                )
            }
        } }

    override fun observeTags(ledgerId: LedgerId): Flow<Result<List<Tag>>> = database.tagQueries
        .activeTagsForLedger(ledgerId)
        .asFlow()
        .mapToList(Dispatchers.Default)
        .map { rows -> runCatching {
            rows.map {
                Tag(
                    id = it.id,
                    ledgerId = it.ledger_id,
                    name = it.name,
                    deletedAt = it.deleted_at?.let(Instant::fromEpochMilliseconds),
                )
            }
        } }

    override fun observeRules(ledgerId: LedgerId): Flow<Result<List<Rule>>> = database.ruleQueries
        .activeRulesForLedger(ledgerId)
        .asFlow()
        .mapToList(Dispatchers.Default)
        .map { rows -> runCatching {
            rows.map {
                Rule(
                    id = it.id,
                    ledgerId = it.ledger_id,
                    name = it.name,
                    conditionType = RuleConditionType.valueOf(it.condition_type),
                    conditionValue = it.condition_value,
                    actionType = RuleActionType.valueOf(it.action_type),
                    actionValue = it.action_value,
                    priority = it.priority.toInt(),
                    deletedAt = it.deleted_at?.let(Instant::fromEpochMilliseconds),
                )
            }
        } }
}
