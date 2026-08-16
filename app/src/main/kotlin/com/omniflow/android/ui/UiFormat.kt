package com.omniflow.android.ui

import com.omniflow.core.domain.model.DateRange
import com.omniflow.core.domain.model.Money
import java.time.LocalDate as JavaLocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime

internal fun Money.asRmb(): String {
    val absolute = kotlin.math.abs(minor)
    val prefix = if (minor < 0) "-" else ""
    return "$prefix¥${(absolute / 100).grouped()}.${(absolute % 100).toString().padStart(2, '0')}"
}

internal fun Money.asCompactRmb(): String = "¥${(minor / 100).grouped()}"

/** 三位分组，长金额不加分隔符读起来很吃力。 */
internal fun Long.grouped(): String {
    val negative = this < 0
    val digits = kotlin.math.abs(this).toString()
    val builder = StringBuilder()
    digits.forEachIndexed { index, char ->
        if (index > 0 && (digits.length - index) % 3 == 0) builder.append(',')
        builder.append(char)
    }
    return if (negative) "-$builder" else builder.toString()
}

internal fun Int.grouped(): String = toLong().grouped()

internal fun LocalDate.displayName(): String {
    val day = JavaLocalDate.of(year, monthNumber, dayOfMonth)
    return "${monthNumber}月${dayOfMonth}日 ${day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINA)}"
}

internal fun Instant.displayTime(): String = toLocalDateTime(ChinaTimeZone).time.toString().take(5)

/** 备份列表用的时间戳，原来直接 `Instant.toString()` 会显示 ISO 文本且是 UTC。 */
internal fun Instant.backupTimeText(): String {
    val local = toLocalDateTime(ChinaTimeZone)
    return "${local.year}-${local.monthNumber.twoDigits()}-${local.dayOfMonth.twoDigits()} " +
        "${local.hour.twoDigits()}:${local.minute.twoDigits()}"
}

internal fun DateRange.displayLabel(mode: AnalyticsRangeMode): String {
    val start = startInclusive.toLocalDateTime(ChinaTimeZone).date
    val end = Instant.fromEpochMilliseconds(endExclusive.toEpochMilliseconds() - 1)
        .toLocalDateTime(ChinaTimeZone).date
    return when (mode) {
        AnalyticsRangeMode.WEEK -> "${start.monthDay()} 至 ${end.monthDay()}"
        AnalyticsRangeMode.MONTH -> "${start.year}-${start.monthNumber.twoDigits()}"
        AnalyticsRangeMode.YEAR -> start.year.toString()
        AnalyticsRangeMode.CUSTOM -> "${start.fullDate()} 至 ${end.fullDate()}"
    }
}

internal fun DateRange.detailLabel(): String {
    val start = startInclusive.toLocalDateTime(ChinaTimeZone).date
    val end = Instant.fromEpochMilliseconds(endExclusive.toEpochMilliseconds() - 1)
        .toLocalDateTime(ChinaTimeZone).date
    return if (start == end) start.displayName() else "${start.fullDate()} 至 ${end.fullDate()}"
}

private fun LocalDate.monthDay(): String = "${monthNumber.twoDigits()}-${dayOfMonth.twoDigits()}"

private fun LocalDate.fullDate(): String = "$year-${monthNumber.twoDigits()}-${dayOfMonth.twoDigits()}"

private fun Int.twoDigits(): String = toString().padStart(2, '0')
