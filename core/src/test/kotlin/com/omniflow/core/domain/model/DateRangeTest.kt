package com.omniflow.core.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DateRangeTest {

    @Test
    fun appTimeZoneIsAsiaShanghai() {
        assertEquals("Asia/Shanghai", AppTimeZone.id)
    }

    @Test
    fun ofDayProducesExactOneDayRange() {
        val date = LocalDate(2026, 6, 15)
        val range = DateRange.ofDay(date)

        val expectedStart = Instant.parse("2026-06-14T16:00:00Z") // 2026-06-15 00:00:00+08:00
        val expectedEnd = Instant.parse("2026-06-15T16:00:00Z")   // 2026-06-16 00:00:00+08:00

        assertEquals(expectedStart, range.startInclusive)
        assertEquals(expectedEnd, range.endExclusive)
        assertEquals(date, range.startLocalDate())
        assertEquals(date, range.endLocalDateInclusive())
        assertTrue(expectedStart in range)
        assertFalse(expectedEnd in range)
    }

    @Test
    fun ofMonthHandlesLeapYearAndMonthBoundaries() {
        // Leap year February 2024
        val feb2024 = DateRange.ofMonth(2024, 2)
        assertEquals(LocalDate(2024, 2, 1), feb2024.startLocalDate())
        assertEquals(LocalDate(2024, 2, 29), feb2024.endLocalDateInclusive())

        // Non-leap year February 2025
        val feb2025 = DateRange.ofMonth(2025, 2)
        assertEquals(LocalDate(2025, 2, 1), feb2025.startLocalDate())
        assertEquals(LocalDate(2025, 2, 28), feb2025.endLocalDateInclusive())

        // December rollover to next year
        val dec2025 = DateRange.ofMonth(2025, 12)
        assertEquals(LocalDate(2025, 12, 1), dec2025.startLocalDate())
        assertEquals(LocalDate(2025, 12, 31), dec2025.endLocalDateInclusive())
        assertEquals(LocalDate(2026, 1, 1).atStartOfDayIn(AppTimeZone), dec2025.endExclusive)
    }

    @Test
    fun ofClosedDatesNormalizesInvertedOrder() {
        val start = LocalDate(2026, 3, 10)
        val end = LocalDate(2026, 3, 20)

        val forward = DateRange.ofClosedDates(start, end)
        val reverse = DateRange.ofClosedDates(end, start)

        assertEquals(forward, reverse)
        assertEquals(start, forward.startLocalDate())
        assertEquals(end, forward.endLocalDateInclusive())
        assertEquals(LocalDate(2026, 3, 21).atStartOfDayIn(AppTimeZone), forward.endExclusive)
    }

    @Test
    fun ofYearSpansFullCalendarYear() {
        val year2026 = DateRange.ofYear(2026)
        assertEquals(LocalDate(2026, 1, 1), year2026.startLocalDate())
        assertEquals(LocalDate(2026, 12, 31), year2026.endLocalDateInclusive())
        assertEquals(LocalDate(2027, 1, 1).atStartOfDayIn(AppTimeZone), year2026.endExclusive)
    }

    @Test
    fun ofWeekAnchorsToMonday() {
        // 2026-06-17 is Wednesday
        val wednesday = LocalDate(2026, 6, 17)
        val week = DateRange.ofWeek(wednesday)

        assertEquals(LocalDate(2026, 6, 15), week.startLocalDate()) // Monday
        assertEquals(LocalDate(2026, 6, 21), week.endLocalDateInclusive()) // Sunday
        assertEquals(LocalDate(2026, 6, 22).atStartOfDayIn(AppTimeZone), week.endExclusive)
    }

    @Test
    fun invalidRangeThrowsException() {
        val t1 = Instant.parse("2026-06-01T00:00:00Z")
        val t2 = Instant.parse("2026-05-01T00:00:00Z")
        assertThrows(IllegalArgumentException::class.java) {
            DateRange(t1, t2)
        }
    }
}
