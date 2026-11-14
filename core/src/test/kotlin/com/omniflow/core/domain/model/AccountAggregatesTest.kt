package com.omniflow.core.domain.model

import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AccountAggregatesTest {

    private fun createTestAccount(
        id: String,
        name: String,
        type: AccountType,
        balance: Money,
        included: Boolean = true,
        deleted: Boolean = false,
    ) = Account(
        id = id,
        name = name,
        type = type,
        iconKey = "wallet",
        balance = balance,
        includeInTotalAssets = included,
        deletedAt = if (deleted) Clock.System.now() else null,
    )

    @Test
    fun groupByType_correctlyGroupsAndCalculatesSubtotals() {
        val accounts = listOf(
            createTestAccount("1", "零钱", AccountType.CASH, Money(10000)), // 100.00
            createTestAccount("2", "钱包", AccountType.CASH, Money(5000)),  // 50.00
            createTestAccount("3", "工行储蓄卡", AccountType.DEBIT_CARD, Money(500000)), // 5000.00
            createTestAccount("4", "招行信用卡", AccountType.CREDIT_CARD, Money(-200000)), // -2000.00
            createTestAccount("5", "已删除卡", AccountType.DEBIT_CARD, Money(88888), deleted = true),
        )

        val groups = AccountAggregator.groupByType(accounts)

        assertEquals(3, groups.size)

        val cashGroup = groups.first { it.type == AccountType.CASH }
        assertEquals(2, cashGroup.accounts.size)
        assertEquals(Money(15000), cashGroup.subtotal)

        val debitGroup = groups.first { it.type == AccountType.DEBIT_CARD }
        assertEquals(1, debitGroup.accounts.size)
        assertEquals(Money(500000), debitGroup.subtotal)

        val creditGroup = groups.first { it.type == AccountType.CREDIT_CARD }
        assertEquals(1, creditGroup.accounts.size)
        assertEquals(Money(-200000), creditGroup.subtotal)
    }

    @Test
    fun groupByType_emptyAccountsReturnsEmptyList() {
        assertTrue(AccountAggregator.groupByType(emptyList()).isEmpty())
    }

    @Test
    fun calculateSummary_computesAssetsLiabilitiesAndNetAssets() {
        val accounts = listOf(
            createTestAccount("1", "储蓄卡", AccountType.DEBIT_CARD, Money(1000000)), // 10000.00
            createTestAccount("2", "支付宝", AccountType.E_WALLET, Money(200000)),   // 2000.00
            createTestAccount("3", "信用卡", AccountType.CREDIT_CARD, Money(-350000)), // -3500.00 (负债 3500)
            createTestAccount("4", "私房钱", AccountType.CASH, Money(50000), included = false), // 不计入总资产
            createTestAccount("5", "作废卡", AccountType.DEBIT_CARD, Money(80000), deleted = true),
        )

        val summary = AccountAggregator.calculateSummary(accounts)

        assertEquals(Money(1200000), summary.assets) // 10000 + 2000 = 12000.00
        assertEquals(Money(350000), summary.liabilities) // 3500.00
        assertEquals(Money(850000), summary.netAssets) // 12000 - 3500 = 8500.00
    }

    @Test
    fun formatAmount_masksWhenEnabled() {
        val money = Money(12345)
        assertEquals("••••••", AccountAggregator.formatAmount(money, masked = true))
        assertEquals("¥123.45", AccountAggregator.formatAmount(money, masked = false))
    }
}
