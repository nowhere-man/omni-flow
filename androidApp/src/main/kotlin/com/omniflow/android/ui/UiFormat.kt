package com.omniflow.android.ui

import com.omniflow.shared.domain.model.DateRange
import com.omniflow.shared.domain.model.Money
import java.time.LocalDate as JavaLocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime

internal fun Money.asRmb(): String {
    val absolute = kotlin.math.abs(minor)
    val prefix = if (minor < 0) "-" else ""
    return "$prefix¥${absolute / 100}.${(absolute % 100).toString().padStart(2, '0')}"
}

internal fun Money.asCompactRmb(): String = "¥${minor / 100}"

internal fun LocalDate.displayName(): String {
    val day = JavaLocalDate.of(year, monthNumber, dayOfMonth)
    return "${monthNumber}月${dayOfMonth}日 ${day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINA)}"
}

internal fun Instant.displayTime(): String = toLocalDateTime(ChinaTimeZone).time.toString().take(5)

internal fun DateRange.displayLabel(): String {
    val start = startInclusive.toLocalDateTime(ChinaTimeZone).date
    val end = endExclusive.toLocalDateTime(ChinaTimeZone).date
    return if (start.year == end.year && start.monthNumber == end.monthNumber) {
        "${start.year}年${start.monthNumber}月"
    } else {
        "$start — $end"
    }
}
