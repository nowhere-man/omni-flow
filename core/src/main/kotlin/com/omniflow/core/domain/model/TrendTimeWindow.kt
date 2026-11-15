package com.omniflow.core.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

/**
 * 统计页趋势图时间窗口计算器（Part 4 / 需求条目 9 领域层实现）。
 *
 * 核心规格：
 * 9.1 年模式柱状图：滚动近 12 个月窗口，以锚点月为最右端，向前推 12 个月，恒定 12 根柱。
 * 9.2 月模式柱状图：当月只展示 1 号至今天为止（例：今天 15 号则只展示 1–15 号），历史月展示全月。
 * 9.3 不变式：当天/当月永远位于最右端，右端不留空。
 * 9.4 周模式：保持自然周不变。
 */
object TrendTimeWindowCalculator {

    /**
     * 年模式下的滚动 12 个月时间窗口（9.1）：
     * 以 [anchorDate] 所在月份为最右端，向前推 12 个月。
     * 例如 anchorDate 为 2026-11 时，返回区间为 [2025-12-01, 2026-12-01)，
     * 对应 2025-12, 2026-01, ..., 2026-11 恒定 12 个自然月。
     */
    fun rolling12Months(
        anchorDate: LocalDate,
        timeZone: TimeZone = AppTimeZone,
    ): DateRange {
        val endYearMonth = java.time.YearMonth.of(anchorDate.year, anchorDate.monthNumber)
        val startYearMonth = endYearMonth.minusMonths(11) // 包含当前月在内的 12 个月
        val start = LocalDate(startYearMonth.year, startYearMonth.monthValue, 1).atStartOfDayIn(timeZone)
        val nextMonth = endYearMonth.plusMonths(1)
        val end = LocalDate(nextMonth.year, nextMonth.monthValue, 1).atStartOfDayIn(timeZone)
        return DateRange(start, end)
    }

    /**
     * 月模式下的时间窗口（9.2）：
     * 当 [anchorDate] 所在的年月等于 [today] 所在的年月时（当前月），只展示 1 号至今天（含今天）；
     * 当 [anchorDate] 所在的年月早于当前月（历史月），展示该自然月的 1 号至月末；
     * 当 [anchorDate] 所在的年月晚于当前月，展示该自然月 1 号至月末。
     */
    fun monthToDate(
        anchorDate: LocalDate,
        today: LocalDate,
        timeZone: TimeZone = AppTimeZone,
    ): DateRange {
        val anchorYM = java.time.YearMonth.of(anchorDate.year, anchorDate.monthNumber)
        val todayYM = java.time.YearMonth.of(today.year, today.monthNumber)

        val startDate = LocalDate(anchorDate.year, anchorDate.monthNumber, 1)
        val endInclusiveDate = when {
            anchorYM == todayYM -> if (today.dayOfMonth < 1) startDate else today
            anchorYM < todayYM -> {
                val lastDay = anchorYM.lengthOfMonth()
                LocalDate(anchorDate.year, anchorDate.monthNumber, lastDay)
            }
            else -> {
                val lastDay = anchorYM.lengthOfMonth()
                LocalDate(anchorDate.year, anchorDate.monthNumber, lastDay)
            }
        }
        return DateRange.ofClosedDates(startDate, endInclusiveDate, timeZone)
    }
}
