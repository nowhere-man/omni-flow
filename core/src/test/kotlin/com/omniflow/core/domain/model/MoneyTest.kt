package com.omniflow.core.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MoneyTest {
    @Test
    fun parseOrNullHandlesNormalAmounts() {
        assertEquals(Money(0), Money.parseOrNull("0"))
        assertEquals(Money(100), Money.parseOrNull("1"))
        assertEquals(Money(100), Money.parseOrNull("1.00"))
        assertEquals(Money(1234), Money.parseOrNull("12.34"))
        assertEquals(Money(50), Money.parseOrNull("0.5"))
        assertEquals(Money(50), Money.parseOrNull(".5"))
        assertEquals(Money(50), Money.parseOrNull(".50"))
    }

    @Test
    fun parseOrNullHandlesNegativeAndSignedAmounts() {
        assertEquals(Money(-1234), Money.parseOrNull("-12.34"))
        assertEquals(Money(-50), Money.parseOrNull("-.5"))
        assertEquals(Money(1234), Money.parseOrNull("+12.34"))
    }

    @Test
    fun parseOrNullHandlesCurrencySymbolsAndCommas() {
        assertEquals(Money(10000), Money.parseOrNull("¥100"))
        assertEquals(Money(10000), Money.parseOrNull("￥100"))
        assertEquals(Money(10000), Money.parseOrNull("$100"))
        assertEquals(Money(-10000), Money.parseOrNull("-¥100"))
        assertEquals(Money(-10000), Money.parseOrNull("¥-100"))
        assertEquals(Money(-10000), Money.parseOrNull("-￥100"))
        assertEquals(Money(123456), Money.parseOrNull("1,234.56"))
        assertEquals(Money(123456), Money.parseOrNull("¥1,234.56"))
        assertEquals(Money(123456), Money.parseOrNull("￥1,234.56"))
    }

    @Test
    fun parseOrNullHandlesHalfUpRounding() {
        assertEquals(Money(1234), Money.parseOrNull("12.344"))
        assertEquals(Money(1235), Money.parseOrNull("12.345"))
        assertEquals(Money(1235), Money.parseOrNull("12.346"))
        assertEquals(Money(200), Money.parseOrNull("1.996"))
    }

    @Test
    fun parseOrNullReturnsNullOnInvalidInput() {
        assertNull(Money.parseOrNull(null))
        assertNull(Money.parseOrNull(""))
        assertNull(Money.parseOrNull("   "))
        assertNull(Money.parseOrNull("abc"))
        assertNull(Money.parseOrNull("12.34.56"))
        assertNull(Money.parseOrNull("¥"))
        assertNull(Money.parseOrNull("-"))
        assertNull(Money.parseOrNull("+"))
    }
}
