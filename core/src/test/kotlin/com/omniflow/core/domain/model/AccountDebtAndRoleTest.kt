package com.omniflow.core.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class AccountDebtAndRoleTest {

    private fun buildAccount(
        type: AccountType,
        balance: Money,
        creditLimit: Money = Money.Zero,
        availableLimit: Money? = null,
        ownerRole: AccountRole = AccountRole.HUSBAND,
    ) = Account(
        id = "test-acc",
        name = "测试账户",
        type = type,
        iconKey = "wallet",
        balance = balance,
        creditLimit = creditLimit,
        availableLimit = availableLimit,
        ownerRole = ownerRole,
        includeInTotalAssets = true,
    )

    @Test
    fun testCreditCardDebtWithAvailableLimit() {
        // Normal debt: credit limit 10,000, available 6,000 => debt = 4,000
        val acc1 = buildAccount(
            type = AccountType.CREDIT_CARD,
            balance = Money.Zero,
            creditLimit = Money(10_000_00),
            availableLimit = Money(6_000_00),
        )
        assertEquals(Money(4_000_00), acc1.debt)

        // Fully available: credit limit 10,000, available 10,000 => debt = 0
        val acc2 = buildAccount(
            type = AccountType.CREDIT_CARD,
            balance = Money.Zero,
            creditLimit = Money(10_000_00),
            availableLimit = Money(10_000_00),
        )
        assertEquals(Money.Zero, acc2.debt)

        // Overpayment (溢缴款): credit limit 10,000, available 12,000 => debt = 0
        val acc3 = buildAccount(
            type = AccountType.CREDIT_CARD,
            balance = Money(2_000_00),
            creditLimit = Money(10_000_00),
            availableLimit = Money(12_000_00),
        )
        assertEquals(Money.Zero, acc3.debt)
    }

    @Test
    fun testCreditCardDebtWithoutAvailableLimit() {
        // Negative balance: balance = -3,500 => debt = 3,500
        val acc1 = buildAccount(
            type = AccountType.CREDIT_CARD,
            balance = Money(-3_500_00),
            availableLimit = null,
        )
        assertEquals(Money(3_500_00), acc1.debt)

        // Positive balance (overpayment): balance = 500 => debt = 0
        val acc2 = buildAccount(
            type = AccountType.CREDIT_CARD,
            balance = Money(500_00),
            availableLimit = null,
        )
        assertEquals(Money.Zero, acc2.debt)

        // Zero balance: debt = 0
        val acc3 = buildAccount(
            type = AccountType.CREDIT_CARD,
            balance = Money.Zero,
            availableLimit = null,
        )
        assertEquals(Money.Zero, acc3.debt)
    }

    @Test
    fun testNonCreditCardDebtBoundary() {
        // Debit card with overdraft (负余额透支): balance = -200 => debt = 200
        val debitOverdraft = buildAccount(
            type = AccountType.DEBIT_CARD,
            balance = Money(-200_00),
        )
        assertEquals(Money(200_00), debitOverdraft.debt)

        // Debit card normal balance: balance = 5,000 => debt = 0
        val debitNormal = buildAccount(
            type = AccountType.DEBIT_CARD,
            balance = Money(5_000_00),
        )
        assertEquals(Money.Zero, debitNormal.debt)

        // Investment account normal balance => debt = 0
        val investNormal = buildAccount(
            type = AccountType.INVESTMENT,
            balance = Money(100_000_00),
        )
        assertEquals(Money.Zero, investNormal.debt)

        // Cash zero balance => debt = 0
        val cashZero = buildAccount(
            type = AccountType.CASH,
            balance = Money.Zero,
        )
        assertEquals(Money.Zero, cashZero.debt)
    }

    @Test
    fun testAccountRoleFromStringTolerance() {
        // Husband matches
        assertEquals(AccountRole.HUSBAND, AccountRole.fromString("HUSBAND"))
        assertEquals(AccountRole.HUSBAND, AccountRole.fromString("husband"))
        assertEquals(AccountRole.HUSBAND, AccountRole.fromString("  Husband  "))
        assertEquals(AccountRole.HUSBAND, AccountRole.fromString("丈夫"))

        // Wife matches
        assertEquals(AccountRole.WIFE, AccountRole.fromString("WIFE"))
        assertEquals(AccountRole.WIFE, AccountRole.fromString("wife"))
        assertEquals(AccountRole.WIFE, AccountRole.fromString("妻子"))
        assertEquals(AccountRole.WIFE, AccountRole.fromString("女主人"))

        // Family matches
        assertEquals(AccountRole.FAMILY, AccountRole.fromString("FAMILY"))
        assertEquals(AccountRole.FAMILY, AccountRole.fromString("family"))
        assertEquals(AccountRole.FAMILY, AccountRole.fromString("家庭"))
        assertEquals(AccountRole.FAMILY, AccountRole.fromString("家庭公用"))

        // Fallback to HUSBAND on null, empty, unknown
        assertEquals(AccountRole.HUSBAND, AccountRole.fromString(null))
        assertEquals(AccountRole.HUSBAND, AccountRole.fromString(""))
        assertEquals(AccountRole.HUSBAND, AccountRole.fromString("   "))
        assertEquals(AccountRole.HUSBAND, AccountRole.fromString("unknown_role"))
    }
}
