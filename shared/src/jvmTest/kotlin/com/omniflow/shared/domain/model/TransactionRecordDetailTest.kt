package com.omniflow.shared.domain.model

import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class TransactionRecordDetailTest {
    @Test
    fun resolvesSharedNamesIncludingSameNamedSecondaryCategory() {
        val transaction = Transaction(
            id = "transaction",
            ledgerId = "ledger",
            accountId = "account",
            categoryId = "secondary",
            amount = Money(100),
            type = TransactionType.EXPENSE,
            occurredAt = Instant.parse("2026-07-13T06:05:00Z"),
            note = null,
            isExcluded = false,
            source = null,
            externalId = null,
            tagIds = setOf("tag"),
        )
        val detail = transaction.toRecordDetail(
            ledgers = listOf(Ledger("ledger", "我的账本", null)),
            accounts = listOf(Account("account", "现金", AccountType.CASH, "banknote", balance = Money.Zero, includeInTotalAssets = true)),
            categories = listOf(
                Category("primary", "ledger", null, "餐饮", "utensils", TransactionType.EXPENSE),
                Category("secondary", "ledger", "primary", "餐饮", null, TransactionType.EXPENSE),
            ),
            tags = listOf(Tag("tag", "ledger", "工作日")),
        )
        assertEquals("我的账本", detail.ledgerName)
        assertEquals("现金", detail.accountName)
        assertEquals("餐饮", detail.primaryCategoryName)
        assertEquals("餐饮", detail.secondaryCategoryName)
        assertEquals(listOf("工作日"), detail.tagNames)
    }
}
