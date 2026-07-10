package com.omniflow.shared.domain.util

import kotlin.random.Random

class UuidGenerator(
    private val random: Random = Random.Default,
) {
    fun next(): String {
        val bytes = random.nextBytes(16)
        bytes[6] = (bytes[6].toInt() and 0x0f or 0x40).toByte()
        bytes[8] = (bytes[8].toInt() and 0x3f or 0x80).toByte()
        return bytes.joinToString(separator = "") { it.toUByte().toString(16).padStart(2, '0') }
            .let { "${it.substring(0, 8)}-${it.substring(8, 12)}-${it.substring(12, 16)}-${it.substring(16, 20)}-${it.substring(20)}" }
    }
}
