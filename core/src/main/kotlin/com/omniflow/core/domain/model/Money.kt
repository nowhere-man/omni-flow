package com.omniflow.core.domain.model

import kotlin.jvm.JvmInline

@JvmInline
value class Money(val minor: Long) : Comparable<Money> {
    operator fun plus(other: Money) = Money(minor + other.minor)
    operator fun minus(other: Money) = Money(minor - other.minor)
    operator fun unaryMinus() = Money(-minor)
    override fun compareTo(other: Money) = minor.compareTo(other.minor)

    companion object {
        val Zero = Money(0)

        fun parseOrNull(value: CharSequence?): Money? {
            if (value == null) return null
            var str = value.toString().trim()
            if (str.isEmpty()) return null

            str = str.replace(",", "").replace(" ", "").replace("\t", "")
            if (str.startsWith('¥') || str.startsWith('\uFFE5') || str.startsWith('$')) {
                str = str.substring(1).trim()
            }
            val negative = str.startsWith('-')
            if (negative || str.startsWith('+')) {
                str = str.substring(1).trim()
            }
            if (str.startsWith('¥') || str.startsWith('\uFFE5') || str.startsWith('$')) {
                str = str.substring(1).trim()
            }
            if (str.isEmpty()) return null

            val parts = str.split('.')
            if (parts.size > 2) return null

            val intPartStr = parts[0]
            val fracPartStr = if (parts.size == 2) parts[1] else ""

            if (intPartStr.isEmpty() && fracPartStr.isEmpty()) return null

            val yuan = if (intPartStr.isEmpty()) {
                0L
            } else {
                if (!intPartStr.all { it.isDigit() }) return null
                intPartStr.toLongOrNull() ?: return null
            }

            var cents = 0L
            if (fracPartStr.isNotEmpty()) {
                if (!fracPartStr.all { it.isDigit() }) return null
                val padded = fracPartStr.padEnd(3, '0')
                var twoDigits = padded.take(2).toLong()
                if (padded[2] >= '5') {
                    twoDigits += 1
                }
                cents = twoDigits
            }

            val totalMinor = yuan * 100L + cents
            return Money(if (negative) -totalMinor else totalMinor)
        }
    }
}
