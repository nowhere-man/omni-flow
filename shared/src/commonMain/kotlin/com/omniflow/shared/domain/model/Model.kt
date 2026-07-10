package com.omniflow.shared.domain.model

import kotlinx.datetime.Instant

typealias LedgerId = String
typealias AccountId = String
typealias CategoryId = String
typealias TagId = String
typealias TransactionId = String

enum class TransactionType { EXPENSE, INCOME }

enum class AccountType { CASH, DEBIT_CARD, CREDIT_CARD, E_WALLET, INVESTMENT }

enum class TransactionSource { MANUAL, ALIPAY, WECHAT, JD, MEITUAN, CCB }

sealed interface LedgerScope {
    data object All : LedgerScope
    data class Single(val ledgerId: LedgerId) : LedgerScope
}

data class Ledger(
    val id: LedgerId,
    val name: String,
    val coverKey: String?,
    val deletedAt: Instant? = null,
)

data class Account(
    val id: AccountId,
    val name: String,
    val type: AccountType,
    val iconKey: String,
    val cardNumber: String? = null,
    val note: String? = null,
    val balance: Money,
    val includeInTotalAssets: Boolean,
    val deletedAt: Instant? = null,
)

data class Category(
    val id: CategoryId,
    val ledgerId: LedgerId,
    val parentId: CategoryId?,
    val name: String,
    val iconKey: String?,
    val type: TransactionType,
    val deletedAt: Instant? = null,
)

data class Tag(
    val id: TagId,
    val ledgerId: LedgerId,
    val name: String,
    val deletedAt: Instant? = null,
)

data class Transaction(
    val id: TransactionId,
    val ledgerId: LedgerId,
    val accountId: AccountId,
    val categoryId: CategoryId,
    val amount: Money,
    val type: TransactionType,
    val occurredAt: Instant,
    val note: String?,
    val isExcluded: Boolean,
    val source: TransactionSource?,
    val externalId: String?,
    val tagIds: Set<TagId> = emptySet(),
    val deletedAt: Instant? = null,
)

data class AccountSummary(
    val assets: Money,
    val liabilities: Money,
) {
    val netAssets: Money get() = assets - liabilities
}
