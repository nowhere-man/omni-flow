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

    @Test
    fun calculateCreditPortions_preciseThreeWayNumerics_50k30k20k() {
        // Precise numerical assertions: 50k / 30k / 20k -> 50% / 30% / 20%, sum = 100%
        val husbandLimit = Money(50_000_00)
        val wifeLimit = Money(30_000_00)
        val familyLimit = Money(20_000_00)

        val summary = AccountAggregator.calculateCreditPortions(
            husbandLimit = husbandLimit,
            wifeLimit = wifeLimit,
            familyLimit = familyLimit,
        )

        assertEquals(Money(100_000_00), summary.totalLimit)

        assertEquals(AccountRole.HUSBAND, summary.husband.role)
        assertEquals(Money(50_000_00), summary.husband.limit)
        assertEquals(0.5f, summary.husband.ratio)
        assertEquals(50, summary.husband.percentage)

        assertEquals(AccountRole.WIFE, summary.wife.role)
        assertEquals(Money(30_000_00), summary.wife.limit)
        assertEquals(0.3f, summary.wife.ratio)
        assertEquals(30, summary.wife.percentage)

        assertEquals(AccountRole.FAMILY, summary.family.role)
        assertEquals(Money(20_000_00), summary.family.limit)
        assertEquals(0.2f, summary.family.ratio)
        assertEquals(20, summary.family.percentage)

        // 三方百分比和必须为 100%
        val totalPercentage = summary.husband.percentage + summary.wife.percentage + summary.family.percentage
        assertEquals(100, totalPercentage)

        // 三方比例和必须为 1.0f
        val totalRatio = summary.husband.ratio + summary.wife.ratio + summary.family.ratio
        assertEquals(1.0f, totalRatio, 0.0001f)

        // portionFor 检索一致性
        assertEquals(summary.husband, summary.portionFor(AccountRole.HUSBAND))
        assertEquals(summary.wife, summary.portionFor(AccountRole.WIFE))
        assertEquals(summary.family, summary.portionFor(AccountRole.FAMILY))
    }

    @Test
    fun calculateCreditPortions_zeroTotal_returnsZeroRatios() {
        val summary = AccountAggregator.calculateCreditPortions(
            husbandLimit = Money.Zero,
            wifeLimit = Money.Zero,
            familyLimit = Money.Zero,
        )

        assertEquals(Money.Zero, summary.totalLimit)
        assertEquals(0f, summary.husband.ratio)
        assertEquals(0, summary.husband.percentage)
        assertEquals(0f, summary.wife.ratio)
        assertEquals(0, summary.wife.percentage)
        assertEquals(0f, summary.family.ratio)
        assertEquals(0, summary.family.percentage)
    }

    @Test
    fun calculateCreditBuffer_fromAccountsList_ignoresDeletedAndNonCreditCards() {
        val accounts = listOf(
            Account(
                id = "c1",
                name = "丈夫信用卡",
                type = AccountType.CREDIT_CARD,
                iconKey = "card",
                balance = Money.Zero,
                creditLimit = Money(50_000_00),
                ownerRole = AccountRole.HUSBAND,
                includeInTotalAssets = true,
            ),
            Account(
                id = "c2",
                name = "妻子信用卡",
                type = AccountType.CREDIT_CARD,
                iconKey = "card",
                balance = Money.Zero,
                creditLimit = Money(30_000_00),
                ownerRole = AccountRole.WIFE,
                includeInTotalAssets = true,
            ),
            Account(
                id = "c3",
                name = "家庭公用信用卡",
                type = AccountType.CREDIT_CARD,
                iconKey = "card",
                balance = Money.Zero,
                creditLimit = Money(20_000_00),
                ownerRole = AccountRole.FAMILY,
                includeInTotalAssets = true,
            ),
            Account(
                id = "d1",
                name = "工行储蓄卡",
                type = AccountType.DEBIT_CARD,
                iconKey = "card",
                balance = Money(80_000_00),
                creditLimit = Money(100_000_00), // 非信用卡额度不计
                ownerRole = AccountRole.HUSBAND,
                includeInTotalAssets = true,
            ),
            Account(
                id = "c4",
                name = "已销卡信用卡",
                type = AccountType.CREDIT_CARD,
                iconKey = "card",
                balance = Money.Zero,
                creditLimit = Money(60_000_00),
                ownerRole = AccountRole.WIFE,
                includeInTotalAssets = true,
                deletedAt = Clock.System.now(), // 已删除不计
            ),
        )

        val summary = AccountAggregator.calculateCreditBuffer(accounts)

        assertEquals(Money(100_000_00), summary.totalLimit)
        assertEquals(50, summary.husband.percentage)
        assertEquals(30, summary.wife.percentage)
        assertEquals(20, summary.family.percentage)
        assertEquals(100, summary.husband.percentage + summary.wife.percentage + summary.family.percentage)
    }
}
