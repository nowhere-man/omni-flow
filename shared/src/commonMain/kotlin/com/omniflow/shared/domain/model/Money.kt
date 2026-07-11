package com.omniflow.shared.domain.model

import kotlin.jvm.JvmInline

@JvmInline
value class Money(val minor: Long) : Comparable<Money> {
    operator fun plus(other: Money) = Money(minor + other.minor)
    operator fun minus(other: Money) = Money(minor - other.minor)
    operator fun unaryMinus() = Money(-minor)
    override fun compareTo(other: Money) = minor.compareTo(other.minor)

    companion object {
        val Zero = Money(0)
    }
}
