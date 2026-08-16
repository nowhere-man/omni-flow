package com.omniflow.core.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class CalendarDaySummaryTest {
    @Test
    fun calendarAmountsUseTruncatedThousandsAboveOneThousandYuan() {
        assertEquals("999", Money(99_999).calendarAmountText())
        assertEquals("1000", Money(100_000).calendarAmountText())
        assertEquals("1.0k", Money(100_001).calendarAmountText())
        assertEquals("25.6k", Money(2_567_499).calendarAmountText())
    }

    @Test
    fun sharedHomeAndTransactionDateFormatsAreStable() {
        assertEquals("2026年07月", LocalDate(2026, 7, 13).yearMonthText())
        val instant = Instant.parse("2026-07-13T06:05:00Z")
        val china = TimeZone.of("Asia/Shanghai")
        assertEquals("14:05", instant.hourMinuteText(china))
        assertEquals("2026-07-13 14:05", instant.transactionDateTimeText(china))
    }

}
