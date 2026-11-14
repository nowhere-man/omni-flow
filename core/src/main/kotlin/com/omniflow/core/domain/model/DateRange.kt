package com.omniflow.core.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

data class DateRange(
    val startInclusive: Instant,
    val endExclusive: Instant,
) {
    init {
        require(startInclusive <= endExclusive) {
            "startInclusive ($startInclusive) must be <= endExclusive ($endExclusive)"
        }
    }

    operator fun contains(instant: Instant): Boolean =
        instant >= startInclusive && instant < endExclusive

    fun startLocalDate(timeZone: TimeZone = AppTimeZone): LocalDate =
        startInclusive.toLocalDateTime(timeZone).date

    fun endLocalDateInclusive(timeZone: TimeZone = AppTimeZone): LocalDate =
        Instant.fromEpochMilliseconds(endExclusive.toEpochMilliseconds() - 1).toLocalDateTime(timeZone).date

    companion object {
        fun ofDay(date: LocalDate, timeZone: TimeZone = AppTimeZone): DateRange {
            val start = date.atStartOfDayIn(timeZone)
            val next = java.time.LocalDate.of(date.year, date.monthNumber, date.dayOfMonth).plusDays(1)
            val end = LocalDate(next.year, next.monthValue, next.dayOfMonth).atStartOfDayIn(timeZone)
            return DateRange(start, end)
        }

        fun ofMonth(year: Int, monthNumber: Int, timeZone: TimeZone = AppTimeZone): DateRange {
            val start = LocalDate(year, monthNumber, 1).atStartOfDayIn(timeZone)
            val next = java.time.YearMonth.of(year, monthNumber).plusMonths(1)
            val end = LocalDate(next.year, next.monthValue, 1).atStartOfDayIn(timeZone)
            return DateRange(start, end)
        }

        fun ofMonth(date: LocalDate, timeZone: TimeZone = AppTimeZone): DateRange =
            ofMonth(date.year, date.monthNumber, timeZone)

        fun ofClosedDates(start: LocalDate, endInclusive: LocalDate, timeZone: TimeZone = AppTimeZone): DateRange {
            val actualStart = minOf(start, endInclusive)
            val actualEnd = maxOf(start, endInclusive)
            val startInstant = actualStart.atStartOfDayIn(timeZone)
            val next = java.time.LocalDate.of(actualEnd.year, actualEnd.monthNumber, actualEnd.dayOfMonth).plusDays(1)
            val endInstant = LocalDate(next.year, next.monthValue, next.dayOfMonth).atStartOfDayIn(timeZone)
            return DateRange(startInstant, endInstant)
        }

        fun ofYear(year: Int, timeZone: TimeZone = AppTimeZone): DateRange {
            val start = LocalDate(year, 1, 1).atStartOfDayIn(timeZone)
            val end = LocalDate(year + 1, 1, 1).atStartOfDayIn(timeZone)
            return DateRange(start, end)
        }

        fun ofWeek(anchor: LocalDate, timeZone: TimeZone = AppTimeZone): DateRange {
            val jAnchor = java.time.LocalDate.of(anchor.year, anchor.monthNumber, anchor.dayOfMonth)
            val jStart = jAnchor.minusDays((jAnchor.dayOfWeek.value - 1).toLong())
            val jEnd = jStart.plusDays(7)
            val start = LocalDate(jStart.year, jStart.monthValue, jStart.dayOfMonth).atStartOfDayIn(timeZone)
            val end = LocalDate(jEnd.year, jEnd.monthValue, jEnd.dayOfMonth).atStartOfDayIn(timeZone)
            return DateRange(start, end)
        }
    }
}
