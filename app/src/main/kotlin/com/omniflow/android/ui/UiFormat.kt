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

/**
 * 明细行和卡片用：不带货币符号，整数金额不拖一个 `.00`。
 * 一屏几十个金额，`¥` 和恒定的 `.00` 只是噪音，去掉之后同样宽度能多放两位数字。
 */
internal fun Money.asPlainAmount(): String {
    val absolute = kotlin.math.abs(minor)
    val prefix = if (minor < 0) "-" else ""
    val yuan = (absolute / 100).grouped()
    val cents = absolute % 100
    return if (cents == 0L) "$prefix$yuan" else "$prefix$yuan.${cents.toString().padStart(2, '0')}"
}

/** 汇总卡用：只保留整数，横向只有三分之一屏宽，小数位占的位置留给数量级更有用。 */
internal fun Money.asWholeAmount(): String {
    val prefix = if (minor < 0) "-" else ""
    return "$prefix${(kotlin.math.abs(minor) / 100).grouped()}"
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

/** 备份列表用的本地时间，精确到秒。 */
internal fun Instant.backupTimeText(): String {
    val local = toLocalDateTime(ChinaTimeZone)
    return "${local.year}-${local.monthNumber.twoDigits()}-${local.dayOfMonth.twoDigits()} " +
        "${local.hour.twoDigits()}:${local.minute.twoDigits()}:${local.second.twoDigits()}"
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
