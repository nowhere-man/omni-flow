package com.omniflow.core.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrendTimeWindowTest {

    private val tz = AppTimeZone

    @Test
    fun rolling12Months_producesExact12MonthsWindow() {
        // 场景 1：2026 年 11 月 15 日作为锚点
        val anchor = LocalDate(2026, 11, 15)
        val range = TrendTimeWindowCalculator.rolling12Months(anchor, tz)

        // 起始时间必须是 2025 年 12 月 1 日 00:00:00
        val expectedStart = LocalDate(2025, 12, 1).atStartOfDayIn(tz)
        // 结束时间（开区间）必须是 2026 年 12 月 1 日 00:00:00
        val expectedEnd = LocalDate(2026, 12, 1).atStartOfDayIn(tz)

        assertEquals(expectedStart, range.startInclusive)
        assertEquals(expectedEnd, range.endExclusive)

        // 验证正好包含 12 个自然月
        val months = mutableListOf<String>()
        var cur = range.startInclusive
        while (cur < range.endExclusive) {
            val ldt = cur.toLocalDateTime(tz)
            months.add("${ldt.year}-${ldt.monthNumber}")
            val nextYm = java.time.YearMonth.of(ldt.year, ldt.monthNumber).plusMonths(1)
            cur = LocalDate(nextYm.year, nextYm.monthValue, 1).atStartOfDayIn(tz)
        }

        assertEquals(12, months.size)
        assertEquals("2025-12", months.first())
        assertEquals("2026-11", months.last())
    }

    @Test
    fun rolling12Months_januaryAnchor_crossesYearCorrectly() {
        // 场景 2：1 月作为锚点，回推至上一年的 2 月
        val anchor = LocalDate(2026, 1, 1)
        val range = TrendTimeWindowCalculator.rolling12Months(anchor, tz)

        val expectedStart = LocalDate(2025, 2, 1).atStartOfDayIn(tz)
        val expectedEnd = LocalDate(2026, 2, 1).atStartOfDayIn(tz)

        assertEquals(expectedStart, range.startInclusive)
        assertEquals(expectedEnd, range.endExclusive)
    }

    @Test
    fun monthToDate_currentMonth_endsOnToday() {
        // 场景 3：当月模式（锚点 2026-11-15，今天 2026-11-15）
        val anchor = LocalDate(2026, 11, 15)
        val today = LocalDate(2026, 11, 15)
        val range = TrendTimeWindowCalculator.monthToDate(anchor, today, tz)

        // 1 号至 15 号，endExclusive 是 16 号 00:00:00
        val expectedStart = LocalDate(2026, 11, 1).atStartOfDayIn(tz)
        val expectedEnd = LocalDate(2026, 11, 16).atStartOfDayIn(tz)

        assertEquals(expectedStart, range.startInclusive)
        assertEquals(expectedEnd, range.endExclusive)
        assertEquals(15, range.daysCount(tz))
    }

    @Test
    fun monthToDate_historicalMonth_showsFullMonth() {
        // 场景 4：历史月模式（锚点 2026-10-05，今天 2026-11-15）
        val anchor = LocalDate(2026, 10, 5)
        val today = LocalDate(2026, 11, 15)
        val range = TrendTimeWindowCalculator.monthToDate(anchor, today, tz)

        // 10 月有 31 天，包含 10-01 至 10-31，endExclusive 是 11-01
        val expectedStart = LocalDate(2026, 10, 1).atStartOfDayIn(tz)
        val expectedEnd = LocalDate(2026, 11, 1).atStartOfDayIn(tz)

        assertEquals(expectedStart, range.startInclusive)
        assertEquals(expectedEnd, range.endExclusive)
        assertEquals(31, range.daysCount(tz))
    }

    @Test
    fun monthToDate_leapYearFebruary_handlesCorrectDays() {
        // 场景 5：闰年二月历史月
        val anchor = LocalDate(2024, 2, 10)
        val today = LocalDate(2026, 11, 15)
        val range = TrendTimeWindowCalculator.monthToDate(anchor, today, tz)

        assertEquals(29, range.daysCount(tz))
    }
}
